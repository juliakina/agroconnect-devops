package br.com.fiap.agroconnect_api.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class DocumentationSecurityConfig {

	@Autowired
	private ApiErrorResponder responder;

	@Bean
	public SecurityFilterChain filtrarDocumentacao(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.requestCache(cache -> cache.disable())
				.formLogin(form -> form.disable())
				.httpBasic(basic -> basic.disable())
				.logout(logout -> logout.disable())
				.authorizeHttpRequests(req -> req
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**",
								"/v3/api-docs.yaml", "/v3/api-docs.yaml/**", "/actuator/health", "/actuator/health/**").permitAll()
						.anyRequest().denyAll())
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint((request, response, error) ->
								responder.write(response, 401, "UNAUTHORIZED", "Autenticação necessária."))
						.accessDeniedHandler((request, response, error) ->
								responder.write(response, 403, "FORBIDDEN", "Acesso não permitido.")));
		return http.build();
	}
}
