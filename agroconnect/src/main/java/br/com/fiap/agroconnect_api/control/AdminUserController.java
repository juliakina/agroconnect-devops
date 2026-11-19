package br.com.fiap.agroconnect_api.control;

import br.com.fiap.agroconnect_api.record.AdminUserResponse;
import br.com.fiap.agroconnect_api.record.PageResponse;
import br.com.fiap.agroconnect_api.record.UserRoleInput;
import br.com.fiap.agroconnect_api.record.UserStatusInput;
import br.com.fiap.agroconnect_api.security.ApiPrincipal;
import br.com.fiap.agroconnect_api.service.AdminUserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

	@Autowired
	private AdminUserService service;

	@GetMapping
	public PageResponse<AdminUserResponse> listar(@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size) {
		return service.list(page, size);
	}

	@PatchMapping("/{id}/role")
	public AdminUserResponse alterarPapel(@AuthenticationPrincipal ApiPrincipal principal,
			@PathVariable UUID id, @Valid @RequestBody UserRoleInput input) {
		return service.changeRole(principal.userId(), id, input);
	}

	@PatchMapping("/{id}/status")
	public AdminUserResponse alterarStatus(@AuthenticationPrincipal ApiPrincipal principal,
			@PathVariable UUID id, @Valid @RequestBody UserStatusInput input) {
		return service.changeStatus(principal.userId(), id, input);
	}
}
