package br.com.fiap.agroconnect_eureka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class RegistrySecurityConfig {

	@Value("${app.registry.username:agroconnect}")
	private String username;
	@Value("${app.registry.password:}")
	private String password;

	@Bean
	public UserDetailsService registryUsers() {
		if (username.isBlank() || password.length() < 32)
			throw new IllegalStateException("Configure EUREKA_USERNAME e EUREKA_PASSWORD com pelo menos 32 caracteres na senha.");
		return new InMemoryUserDetailsManager(User.withUsername(username)
				.password("{bcrypt}" + new BCryptPasswordEncoder().encode(password)).roles("REGISTRY").build());
	}

	@Bean
	public SecurityFilterChain filtrarRegistro(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.ignoringRequestMatchers("/eureka/**"))
				.authorizeHttpRequests(req -> req.requestMatchers("/health").permitAll().anyRequest().hasRole("REGISTRY"))
				.httpBasic(Customizer.withDefaults());
		return http.build();
	}
}
