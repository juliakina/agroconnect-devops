package br.com.fiap.agroconnect_api.control;

import br.com.fiap.agroconnect_api.record.UserInput;
import br.com.fiap.agroconnect_api.record.UserResponse;
import br.com.fiap.agroconnect_api.service.AdditionalDomainService;
import br.com.fiap.agroconnect_api.service.DomainService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserProfileController {

	@Autowired
	private AdditionalDomainService service;
	@Autowired
	private DomainService identity;

	@GetMapping
	public UserResponse buscar(Principal principal) {
		return service.getUser(identity.currentUser(principal));
	}

	@PutMapping
	public UserResponse atualizar(Principal principal, @Valid @RequestBody UserInput input) {
		return service.updateUser(identity.currentUser(principal), input);
	}
}
