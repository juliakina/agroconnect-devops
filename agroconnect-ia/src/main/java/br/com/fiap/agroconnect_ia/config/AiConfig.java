package br.com.fiap.agroconnect_ia.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
