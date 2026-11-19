package br.com.fiap.agroconnect_api.security;

import br.com.fiap.agroconnect_api.repository.SessionRepository;
import br.com.fiap.agroconnect_api.repository.UserRepository;
import br.com.fiap.agroconnect_api.service.ApiException;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtAuthenticationService {

	@Autowired
	private JwtTokenService jwt;
	@Autowired
	private UserRepository users;
	@Autowired
	private SessionRepository sessions;
	@Autowired
	private Clock clock;

	@Transactional(readOnly = true)
	public Authentication authenticate(String token) {
		var identity = jwt.validate(token);
		var session = sessions.findByIdAndUserId(identity.sessionId(), identity.userId())
				.filter(item -> item.getExpiresAt().isAfter(clock.instant())
						&& !identity.expiresAt().isAfter(item.getExpiresAt()))
				.orElseThrow(this::invalid);
		var user = users.findById(identity.userId()).filter(item -> item.getActive()).orElseThrow(this::invalid);
		ApiPrincipal principal = new ApiPrincipal(user.getId(), session.getId(), user.getRole());
		return UsernamePasswordAuthenticationToken.authenticated(principal, null,
				List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
	}

	private ApiException invalid() {
		return new ApiException(401, "UNAUTHORIZED", "Sessão inválida ou encerrada.");
	}
}
