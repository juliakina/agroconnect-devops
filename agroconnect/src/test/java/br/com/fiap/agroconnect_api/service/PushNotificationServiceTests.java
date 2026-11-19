package br.com.fiap.agroconnect_api.service;

import br.com.fiap.agroconnect_api.model.PushToken;
import br.com.fiap.agroconnect_api.repository.PushTokenRepository;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

class PushNotificationServiceTests {
    PushTokenRepository tokens = mock(PushTokenRepository.class, withSettings().mockMaker("mock-maker-subclass"));
    HttpClient http = mock(HttpClient.class, withSettings().mockMaker("mock-maker-subclass"));
    HttpResponse<String> response;
    PushNotificationService service;
    UUID owner = UUID.randomUUID();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() throws Exception {
        response = mock(HttpResponse.class, withSettings().mockMaker("mock-maker-subclass"));
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"data\":[{\"status\":\"ok\",\"id\":\"ticket\"}]}");
        when(http.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);
        service = new PushNotificationService(tokens, "secret", http);
    }

    @AfterEach void close() { service.close(); }

    PushToken token(int n) {
        PushToken token = new PushToken();
        token.setId(UUID.randomUUID());
        token.setToken("ExponentPushToken[test" + n + "]");
        return token;
    }

    AlertCreated event(String severity) {
        return new AlertCreated(owner, UUID.randomUUID(), "Low moisture", "Check soil", severity);
    }

    @Test void sendsOwnedTokensWithMobilePayloadAndAuthorization() throws Exception {
        when(tokens.findAllByUserId(owner)).thenReturn(List.of(token(1)));
        AlertCreated event = event("WARNING");
        service.send(event);
        var capture = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(capture.capture(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        var request = capture.getValue();
        assertEquals("https://exp.host/--/api/v2/push/send", request.uri().toString());
        assertEquals("Bearer secret", request.headers().firstValue("Authorization").orElseThrow());
        StringBuilder body = new StringBuilder();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer b) { byte[] bytes = new byte[b.remaining()]; b.get(bytes); body.append(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)); }
            public void onError(Throwable t) { fail(t); }
            public void onComplete() {}
        });
        var json = tools.jackson.databind.json.JsonMapper.builder().build().readTree(body.toString()).get(0);
        assertEquals("alerts", json.path("channelId").asText());
        assertEquals("default", json.path("sound").asText());
        assertEquals(event.alertId().toString(), json.path("data").path("alertId").asText());
        assertEquals("ALERT", json.path("data").path("type").asText());
        assertEquals("Low moisture", json.path("title").asText());
        assertEquals("Check soil", json.path("body").asText());
        verify(tokens).findAllByUserId(owner);
    }

    @Test void infoAndNoTokensDoNotSend() {
        service.send(event("INFO"));
        verifyNoInteractions(tokens);
        when(tokens.findAllByUserId(owner)).thenReturn(List.of());
        service.send(event("WARNING"));
        verifyNoInteractions(http);
    }

    @Test void deletesOnlyUnregisteredToken() {
        PushToken bad = token(1), good = token(2);
        when(tokens.findAllByUserId(owner)).thenReturn(List.of(bad, good));
        when(response.body()).thenReturn("{\"data\":[{\"status\":\"error\",\"details\":{\"error\":\"DeviceNotRegistered\"}},{\"status\":\"error\",\"details\":{\"error\":\"MessageTooBig\"}}]}");
        service.send(event("CRITICAL"));
        verify(tokens).deleteById(bad.getId());
        verify(tokens, never()).deleteById(good.getId());
    }

    @Test void duplicateAlertAndDeviceBurstAreSuppressed() throws Exception {
        when(tokens.findAllByUserId(owner)).thenReturn(List.of(token(1)));
        var alert = event("WARNING");
        service.send(alert);
        service.send(alert);
        service.send(event("CRITICAL"));
        verify(http, times(1)).send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }

    @Test void splitsBatchesAt100AndContinuesAfterHttpFailure() throws Exception {
        when(tokens.findAllByUserId(owner)).thenReturn(java.util.stream.IntStream.range(0, 201).mapToObj(this::token).toList());
        when(response.statusCode()).thenReturn(503);
        service.send(event("WARNING"));
        var capture = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http, times(3)).send(capture.capture(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }

    @Test void networkAndRepositoryFailuresNeverEscape() throws Exception {
        when(tokens.findAllByUserId(owner)).thenThrow(new IllegalStateException("database unavailable"));
        assertDoesNotThrow(() -> service.send(event("WARNING")));
        doReturn(List.of(token(1))).when(tokens).findAllByUserId(owner);
        when(http.send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenThrow(new java.io.IOException("offline"));
        assertDoesNotThrow(() -> service.send(event("CRITICAL")));
    }

    @Test void optionalAccessTokenIsOmitted() throws Exception {
        service.close();
        service = new PushNotificationService(tokens, "", http);
        when(tokens.findAllByUserId(owner)).thenReturn(List.of(token(1)));
        service.send(event("WARNING"));
        var capture = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(capture.capture(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertTrue(capture.getValue().headers().firstValue("Authorization").isEmpty());
    }

    @Test void commitQueuesPushAndRollbackDoesNotSend() throws Exception {
        when(tokens.findAllByUserId(owner)).thenReturn(List.of(token(1)));
        try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.registerBean(org.springframework.transaction.event.TransactionalEventListenerFactory.class);
            context.registerBean(PushNotificationService.class, () -> service);
            context.refresh();
            var manager = new org.springframework.transaction.support.AbstractPlatformTransactionManager() {
                protected Object doGetTransaction() { return new Object(); }
                protected void doBegin(Object tx, org.springframework.transaction.TransactionDefinition def) {}
                protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status) {}
                protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus status) {}
            };
            var transaction = new org.springframework.transaction.support.TransactionTemplate(manager);
            transaction.executeWithoutResult(status -> {
                context.publishEvent(event("WARNING"));
                verifyNoInteractions(http);
                status.setRollbackOnly();
            });
            verifyNoInteractions(http);
            transaction.executeWithoutResult(status -> {
                context.publishEvent(event("WARNING"));
                verifyNoInteractions(http);
            });
            verify(http, timeout(2000)).send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        }
    }

    @Test void rejectedQueueNeverEscapes() {
        service.close();
        assertDoesNotThrow(() -> service.onAlertCreated(event("WARNING")));
        verifyNoInteractions(http);
    }
}
