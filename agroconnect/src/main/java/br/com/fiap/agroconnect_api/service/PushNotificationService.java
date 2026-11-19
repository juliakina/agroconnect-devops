package br.com.fiap.agroconnect_api.service;

import br.com.fiap.agroconnect_api.repository.PushTokenRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

@Service
public class PushNotificationService implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
    private final PushTokenRepository tokens;
    private final java.util.function.Supplier<HttpClient> http;
    private final String accessToken;
    private final JsonMapper json = JsonMapper.builder().build();
    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(500), r -> {
                Thread thread = new Thread(r, "agroconnect-push");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    // Accessed only by the single worker; bounded by expiry on each dispatch.
    private final Map<UUID, Instant> processed = new HashMap<>();
    private final Map<UUID, Instant> lastSent = new HashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public PushNotificationService(PushTokenRepository tokens,
            @Value("${app.push.expo-access-token:${EXPO_ACCESS_TOKEN:}}") String accessToken) {
        this(tokens, accessToken, () -> HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    PushNotificationService(PushTokenRepository tokens, String accessToken, HttpClient http) {
        this(tokens, accessToken, () -> http);
    }

    private PushNotificationService(PushTokenRepository tokens, String accessToken,
            java.util.function.Supplier<HttpClient> http) {
        this.tokens = tokens;
        this.accessToken = accessToken;
        this.http = http;
    }

    @TransactionalEventListener
    public void onAlertCreated(AlertCreated event) {
        try {
            executor.execute(() -> send(event));
        } catch (RuntimeException ex) {
            log.warn("Push queue unavailable for alert {} ({})", event.alertId(), ex.getClass().getSimpleName());
        }
    }

    synchronized void send(AlertCreated event) {
        try {
            if (!Set.of("WARNING", "CRITICAL").contains(event.severity())) return;
            Instant now = Instant.now();
            processed.values().removeIf(time -> time.isBefore(now.minus(Duration.ofDays(1))));
            lastSent.values().removeIf(time -> time.isBefore(now.minusSeconds(30)));
            if (processed.putIfAbsent(event.alertId(), now) != null) return;
            var recipients = tokens.findAllByUserId(event.userId()).stream()
                    .filter(token -> !lastSent.containsKey(token.getId())).toList();
            for (int offset = 0; offset < recipients.size(); offset += 100) {
                var batch = recipients.subList(offset, Math.min(offset + 100, recipients.size()));
                try {
                    var payload = batch.stream().map(token -> Map.of(
                            "to", token.getToken(), "title", event.title(), "body", event.message(),
                            "channelId", "alerts", "sound", "default",
                            "data", Map.of("alertId", event.alertId().toString(), "type", "ALERT"))).toList();
                    var request = HttpRequest.newBuilder(URI.create("https://exp.host/--/api/v2/push/send"))
                            .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                            .header("Accept", "application/json");
                    if (!accessToken.isBlank()) request.header("Authorization", "Bearer " + accessToken);
                    batch.forEach(token -> lastSent.put(token.getId(), now));
                    var response = http.get().send(request.POST(HttpRequest.BodyPublishers.ofString(
                            json.writeValueAsString(payload))).build(), HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() != 200) {
                        log.warn("Expo HTTP {} for alert {}", response.statusCode(), event.alertId());
                        continue;
                    }
                    var root = json.readTree(response.body());
                    var tickets = root.path("data");
                    if (root.has("errors") || !tickets.isArray() || tickets.size() != batch.size())
                        log.warn("Unexpected Expo response for alert {}", event.alertId());
                    for (int i = 0; i < batch.size() && i < tickets.size(); i++) {
                        var ticket = tickets.get(i);
                        if (!"error".equals(ticket.path("status").asText())) continue;
                        String error = ticket.path("details").path("error").asText();
                        if ("DeviceNotRegistered".equals(error)) tokens.deleteById(batch.get(i).getId());
                        // Never log response messages or tokens: Expo can echo credentials in them.
                        String safeError = error.matches("[A-Za-z]{1,50}") ? error : "UnknownError";
                        log.warn("Expo rejected push: {}, alert {}, token record {}", safeError, event.alertId(), batch.get(i).getId());
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception ex) {
                    log.warn("Push failed for alert {} ({})", event.alertId(), ex.getClass().getSimpleName());
                }
            }
        } catch (Exception ex) {
            log.warn("Push dispatch failed for alert {} ({})", event.alertId(), ex.getClass().getSimpleName());
        }
    }

    @Override
    public void close() { executor.shutdown(); }
}
