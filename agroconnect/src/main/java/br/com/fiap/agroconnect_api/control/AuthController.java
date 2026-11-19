package br.com.fiap.agroconnect_api.control;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import br.com.fiap.agroconnect_api.record.AuthResponse;
import br.com.fiap.agroconnect_api.record.LoginRequest;
import br.com.fiap.agroconnect_api.record.RegisterRequest;
import br.com.fiap.agroconnect_api.record.UserResponse;
import br.com.fiap.agroconnect_api.security.ApiPrincipal;
import br.com.fiap.agroconnect_api.service.AuthService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	@Autowired
	private AuthService service;

	@PostMapping("/register")
	@ApiResponse(responseCode = "201", description = "Registro criado.", useReturnTypeSchema = true)
	public ResponseEntity<AuthResponse> cadastrar(@Valid @RequestBody RegisterRequest input) {
		return ResponseEntity.created(URI.create("/api/v1/auth/me")).body(service.register(input));
	}

	@PostMapping("/login")
	public AuthResponse login(@RequestBody LoginRequest input) {
		return service.login(input);
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal ApiPrincipal principal) {
		return service.me(principal.userId());
	}

	@PostMapping("/logout")
	@ApiResponse(responseCode = "204", description = "Operação concluída sem corpo de resposta.", useReturnTypeSchema = true)
	public ResponseEntity<Void> logout(@AuthenticationPrincipal ApiPrincipal principal) {
		service.logout(principal);
		return ResponseEntity.noContent().build();
	}
}
