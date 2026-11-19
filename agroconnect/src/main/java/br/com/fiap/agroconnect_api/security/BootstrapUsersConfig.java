package br.com.fiap.agroconnect_api.security;

import br.com.fiap.agroconnect_api.model.UserRole;
import br.com.fiap.agroconnect_api.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "bootstrap"})
@Order(10)
public class BootstrapUsersConfig implements ApplicationRunner {

	@Autowired
	private AuthService auth;
	@Value("${app.bootstrap.admin-email:}")
	private String adminEmail;
	@Value("${app.bootstrap.admin-password:}")
	private String adminPassword;
	@Value("${app.bootstrap.user-email:}")
	private String userEmail;
	@Value("${app.bootstrap.user-password:}")
	private String userPassword;

	@Override
	public void run(ApplicationArguments args) {
		provision(adminEmail, adminPassword, "Administrador", UserRole.ADMIN);
		provision(userEmail, userPassword, "Produtor de teste", UserRole.USER);
	}

	private void provision(String email, String password, String name, UserRole role) {
		if (email.isBlank() && password.isBlank()) return;
		if (email.isBlank() || password.isBlank())
			throw new IllegalStateException("Informe e-mail e senha para a conta inicial " + role + ".");
		auth.provision(name, email, password, role);
	}
}
