package br.com.fiap.agroconnect_api.security;

import br.com.fiap.agroconnect_api.service.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	@Autowired
	private JwtAuthenticationService service;
	@Autowired
	private ApiErrorResponder responder;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI().substring(request.getContextPath().length());
		return "POST".equals(request.getMethod())
				&& ("/api/v1/auth/login".equals(path) || "/api/v1/auth/register".equals(path));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		var headers = Collections.list(request.getHeaders("Authorization"));
		if (headers.isEmpty()) {
			chain.doFilter(request, response);
			return;
		}
		String header = headers.get(0);
		if (headers.size() != 1 || header.length() < 8 || !header.regionMatches(true, 0, "Bearer ", 0, 7)
				|| header.substring(7).isBlank()) {
			responder.write(response, 401, "UNAUTHORIZED", "Informe um Bearer token válido.");
			return;
		}
		try {
			var authentication = service.authenticate(header.substring(7));
			var context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
			var principal = (ApiPrincipal) authentication.getPrincipal();
			request.setAttribute("sessionId", principal.sessionId());
			chain.doFilter(request, response);
		} catch (ApiException exception) {
			SecurityContextHolder.clearContext();
			responder.write(response, 401, "UNAUTHORIZED", "Token inválido, expirado ou sessão encerrada.");
		}
	}
}
