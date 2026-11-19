package br.com.fiap.agroconnect_ia.control;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalKeyFilter extends OncePerRequestFilter {

	@Value("${app.ai.internal-key:}")
	private String key;

	@PostConstruct
	public void initialize() {
		if (key.length() < 32) throw new IllegalStateException("AI_INTERNAL_KEY deve ter pelo menos 32 caracteres.");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (request.getServletPath().startsWith("/internal/") || request.getServletPath().startsWith("/ia/")) {
			String supplied = request.getHeader("X-Internal-Key");
			if (supplied == null || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
				response.setStatus(401);
				response.setContentType("application/json");
				response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Credencial interna inválida.\"}");
				return;
			}
		}
		chain.doFilter(request, response);
	}
}
