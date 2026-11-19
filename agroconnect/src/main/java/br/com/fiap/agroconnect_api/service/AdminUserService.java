package br.com.fiap.agroconnect_api.service;

import br.com.fiap.agroconnect_api.model.UserRole;
import br.com.fiap.agroconnect_api.model.User;
import br.com.fiap.agroconnect_api.record.AdminUserResponse;
import br.com.fiap.agroconnect_api.record.PageResponse;
import br.com.fiap.agroconnect_api.record.UserRoleInput;
import br.com.fiap.agroconnect_api.record.UserStatusInput;
import br.com.fiap.agroconnect_api.repository.PushTokenRepository;
import br.com.fiap.agroconnect_api.repository.SessionRepository;
import br.com.fiap.agroconnect_api.repository.UserRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserService {

	@Autowired
	private UserRepository users;
	@Autowired
	private SessionRepository sessions;
	@Autowired
	private PushTokenRepository tokens;
	@Autowired
	private DomainSupport support;

	private AdminUserResponse response(User user) {
		return new AdminUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getActive(), user.getCreatedAt());
	}

	private User user(UUID id) {
		return users.findById(id).orElseThrow(() -> support.missing("Usuário"));
	}

	public PageResponse<AdminUserResponse> list(int page, int size) {
		return PageResponse.from(users.findAll(support.page(page, size, "email", "id")).map(this::response));
	}

	@Transactional
	public AdminUserResponse changeRole(UUID actorId, UUID id, UserRoleInput input) {
		support.validate(input);
		User user = user(id);
		if (actorId.equals(id) && input.role() != UserRole.ADMIN)
			throw support.invalid("role", "Você não pode remover o próprio papel de administrador.");
		if (user.getRole() != input.role()) {
			user.setRole(input.role());
			tokens.deleteAllForUser(id);
			sessions.deleteAllForUser(id);
		}
		return response(user);
	}

	@Transactional
	public AdminUserResponse changeStatus(UUID actorId, UUID id, UserStatusInput input) {
		support.validate(input);
		User user = user(id);
		if (actorId.equals(id) && !input.active())
			throw support.invalid("active", "Você não pode desativar a própria conta de administrador.");
		user.setActive(input.active());
		if (!input.active()) {
			tokens.deleteAllForUser(id);
			sessions.deleteAllForUser(id);
		}
		return response(user);
	}
}
