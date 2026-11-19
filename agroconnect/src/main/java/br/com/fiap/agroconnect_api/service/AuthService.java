package br.com.fiap.agroconnect_api.service;

import br.com.fiap.agroconnect_api.model.UserRole;
import br.com.fiap.agroconnect_api.model.Session;
import br.com.fiap.agroconnect_api.model.User;
import br.com.fiap.agroconnect_api.record.AuthResponse;
import br.com.fiap.agroconnect_api.record.LoginRequest;
import br.com.fiap.agroconnect_api.record.RegisterRequest;
import br.com.fiap.agroconnect_api.record.UserResponse;
import br.com.fiap.agroconnect_api.repository.PushTokenRepository;
import br.com.fiap.agroconnect_api.repository.SessionRepository;
import br.com.fiap.agroconnect_api.repository.UserRepository;
import br.com.fiap.agroconnect_api.security.ApiPrincipal;
import br.com.fiap.agroconnect_api.security.JwtTokenService;
import br.com.fiap.agroconnect_api.security.PasswordHasher;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AuthService {

	@Autowired
	private UserRepository users;
	@Autowired
	private SessionRepository sessions;
	@Autowired
	private PushTokenRepository tokens;
	@Autowired
	private PasswordHasher passwordHasher;
	@Autowired
	private JwtTokenService jwt;
	@Autowired
	private DomainSupport support;
	@Autowired
	private Clock clock;
	@Autowired
	private PlatformTransactionManager transactionManager;
	private String dummyHash;

	@PostConstruct
	public void initialize() {
		dummyHash = passwordHasher.hash(UUID.randomUUID().toString());
	}

	public AuthResponse register(RegisterRequest input) {
		support.validate(input);
		String email = input.email().toLowerCase(Locale.ROOT);
		if (users.findByEmailIgnoreCase(email).isPresent()) throw duplicate();
		String hash = passwordHasher.hash(input.password());
		try {
			return new TransactionTemplate(transactionManager).execute(status -> {
				User user = new User();
				user.setName(input.name());
				user.setEmail(email);
				user.setPasswordHash(hash);
				user.setRole(UserRole.USER);
				user.setActive(true);
				users.saveAndFlush(user);
				return startSession(user);
			});
		} catch (DataIntegrityViolationException exception) {
			if (users.findByEmailIgnoreCase(email).isPresent()) throw duplicate();
			throw exception;
		}
	}

	@Transactional
	public AuthResponse login(LoginRequest input) {
		String email = input.email() == null ? "" : input.email().trim().toLowerCase(Locale.ROOT);
		User user = users.findByEmailIgnoreCase(email).orElse(null);
		boolean validLength = input.password() != null && input.password().length() >= 8 && input.password().length() <= 128;
		boolean validPassword = passwordHasher.verify(validLength ? input.password() : "",
				user == null ? dummyHash : user.getPasswordHash());
		if (!validLength || user == null || !user.getActive() || !validPassword)
			throw new ApiException(401, "INVALID_CREDENTIALS", "E-mail ou senha inválidos.");
		var now = clock.instant();
		tokens.deleteExpiredForUser(user.getId(), now);
		sessions.deleteExpiredForUser(user.getId(), now);
		return startSession(user);
	}

	private AuthResponse startSession(User user) {
		Session session = new Session();
		session.setUser(user);
		session.setExpiresAt(clock.instant().truncatedTo(ChronoUnit.SECONDS).plusSeconds(JwtTokenService.EXPIRES_IN));
		sessions.saveAndFlush(session);
		return new AuthResponse(jwt.create(user, session), "Bearer", JwtTokenService.EXPIRES_IN, response(user));
	}

	@Transactional(readOnly = true)
	public UserResponse me(UUID userId) {
		return response(support.user(userId));
	}

	@Transactional
	public void logout(ApiPrincipal principal) {
		tokens.deleteForSession(principal.userId(), principal.sessionId());
		sessions.deleteOwned(principal.userId(), principal.sessionId());
	}

	private UserResponse response(User user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
	}

	private ApiException duplicate() {
		return new ApiException(409, "EMAIL_ALREADY_EXISTS", "Este e-mail já está cadastrado.");
	}

	@Transactional
	public void provision(String name, String email, String password, UserRole role) {
		RegisterRequest input = support.validate(new RegisterRequest(name, email, password));
		String normalizedEmail = input.email().toLowerCase(Locale.ROOT);
		var existing = users.findByEmailIgnoreCase(normalizedEmail);
		if (existing.isPresent()) {
			if (existing.get().getRole() != role)
				throw new IllegalStateException("A conta de desenvolvimento já existe com outro papel.");
			return;
		}
		User user = new User();
		user.setName(input.name());
		user.setEmail(normalizedEmail);
		user.setPasswordHash(passwordHasher.hash(input.password()));
		user.setRole(role);
		user.setActive(true);
		users.saveAndFlush(user);
	}
}
