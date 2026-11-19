package br.com.fiap.agroconnect_api.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthRateLimitFilter extends OncePerRequestFilter {

	@Autowired
	private Clock clock;
	@Autowired
	private ApiErrorResponder responder;
	@Value("${app.auth-rate-limit:30}")
	private int limit;
	private final ConcurrentHashMap<String, Window> attempts = new ConcurrentHashMap<>();

	@PostConstruct
	public void initialize() {
		if (limit < 1) throw new IllegalStateException("O limite de autenticação deve ser positivo.");
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI().substring(request.getContextPath().length());
		return !"POST".equals(request.getMethod()) || (!"/api/v1/auth/login".equals(path) && !"/api/v1/auth/register".equals(path));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		long minute = Math.floorDiv(clock.instant().getEpochSecond(), 60);
		attempts.entrySet().removeIf(entry -> entry.getValue().minute() < minute);
		String address = request.getRemoteAddr();
		if (attempts.size() >= 10000 && !attempts.containsKey(address)) {
			reject(response);
			return;
		}
		Window window = attempts.compute(address, (key, previous) ->
				previous == null || previous.minute() != minute ? new Window(minute, 1)
						: new Window(minute, Math.min(limit + 1L, previous.count() + 1)));
		if (window.count() > limit) {
			reject(response);
			return;
		}
		chain.doFilter(request, response);
	}

	private void reject(HttpServletResponse response) throws IOException {
		response.setHeader("Retry-After", "60");
		responder.write(response, 429, "RATE_LIMITED", "Muitas tentativas. Aguarde antes de tentar novamente.");
	}

	private record Window(long minute, long count) {
	}
}
