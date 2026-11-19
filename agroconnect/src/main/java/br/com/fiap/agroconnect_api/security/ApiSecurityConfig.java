package br.com.fiap.agroconnect_api.security;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class ApiSecurityConfig {

	@Autowired
	private ApiErrorResponder responder;
	@Value("${app.cors.allowed-origins:}")
	private String allowedOrigins;

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter() {
		return new JwtAuthenticationFilter();
	}

	@Bean
	public AuthRateLimitFilter authRateLimitFilter() {
		return new AuthRateLimitFilter();
	}

	@Bean
	public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
		FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	public FilterRegistrationBean<AuthRateLimitFilter> authRateLimitRegistration(AuthRateLimitFilter filter) {
		FilterRegistrationBean<AuthRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	public CorsConfigurationSource apiCorsConfigurationSource() {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList());
		cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
		cors.setExposedHeaders(List.of("Location"));
		cors.setAllowCredentials(false);
		cors.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/v1/**", cors);
		return source;
	}

	@Bean
	@Order(1)
	public SecurityFilterChain filtrarApi(HttpSecurity http, JwtAuthenticationFilter jwtFilter, AuthRateLimitFilter rateFilter)
			throws Exception {
		http.securityMatcher("/api/v1/**")
				.cors(cors -> cors.configurationSource(apiCorsConfigurationSource()))
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.requestCache(cache -> cache.disable())
				.formLogin(form -> form.disable())
				.httpBasic(basic -> basic.disable())
				.logout(logout -> logout.disable())
				.authorizeHttpRequests(req -> req
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/api/v1/**").permitAll()
						.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
						.anyRequest().hasAnyRole("USER", "ADMIN"))
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint((request, response, error) ->
								responder.write(response, 401, "UNAUTHORIZED", "Autenticação necessária."))
						.accessDeniedHandler((request, response, error) ->
								responder.write(response, 403, "FORBIDDEN", "Acesso não permitido.")))
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(rateFilter, JwtAuthenticationFilter.class);
		return http.build();
	}
}
