package br.com.fiap.agroconnect_api.config;

import br.com.fiap.agroconnect_api.repository.UserRepository;
import br.com.fiap.agroconnect_api.service.DemoDataService;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "bootstrap"})
@Order(20)
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataConfig implements ApplicationRunner {

	@Autowired
	private DemoDataService service;
	@Autowired
	private UserRepository users;
	@Value("${app.bootstrap.user-email:}")
	private String email;

	@Override
	public void run(ApplicationArguments args) {
		if (email.isBlank())
			throw new IllegalStateException("Configure DEMO_USER_EMAIL e DEMO_USER_PASSWORD para criar a base de demonstração.");
		var user = users.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
				.orElseThrow(() -> new IllegalStateException("A conta de demonstração não foi criada."));
		service.seed(user.getId());
	}
}
