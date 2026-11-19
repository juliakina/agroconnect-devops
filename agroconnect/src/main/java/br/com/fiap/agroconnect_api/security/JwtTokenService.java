package br.com.fiap.agroconnect_api.security;

import br.com.fiap.agroconnect_api.model.Session;
import br.com.fiap.agroconnect_api.model.User;
import br.com.fiap.agroconnect_api.service.ApiException;
import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Service
public class JwtTokenService {

	public static final int EXPIRES_IN = 3600;

	@Autowired
	private Environment environment;
	@Autowired
	private Clock clock;
	@Value("${jwt.key:}")
	private String key;
	@Value("${jwt.issuer:AgroConnect}")
	private String issuer;
	@Value("${jwt.audience:AgroConnect.Mobile}")
	private String audience;

	private byte[] signingKey;
	private final JsonMapper mapper = JsonMapper.builder()
			.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
			.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();

	@PostConstruct
	public void initialize() {
		if (key.isBlank() && environment.acceptsProfiles(Profiles.of("dev", "test"))) {
			byte[] random = new byte[32];
			new SecureRandom().nextBytes(random);
			key = HexFormat.of().withUpperCase().formatHex(random);
		}
		signingKey = key.getBytes(StandardCharsets.UTF_8);
		if (key.isBlank() || signingKey.length < 32)
			throw new IllegalStateException("Configure Jwt__Key com no mínimo 32 bytes.");
		if (issuer.isBlank() || audience.isBlank())
			throw new IllegalStateException("Informe issuer e audience do JWT.");
	}

	public String create(User user, Session session) {
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("sub", user.getId().toString());
		claims.put("jti", session.getId().toString());
		claims.put("iss", issuer);
		claims.put("aud", audience);
		claims.put("iat", clock.instant().getEpochSecond());
		claims.put("exp", session.getExpiresAt().getEpochSecond());
		claims.put("role", user.getRole().name());
		String content = encode(mapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")))
				+ "." + encode(mapper.writeValueAsBytes(claims));
		return content + "." + encode(sign(content));
	}

	public TokenIdentity validate(String token) {
		try {
			if (token == null || token.length() > 8192) throw invalid();
			String[] parts = token.split("\\.", -1);
			if (parts.length != 3) throw invalid();
			for (String part : parts) if (!part.matches("[A-Za-z0-9_-]+")) throw invalid();
			Map<?, ?> header = mapper.readValue(Base64.getUrlDecoder().decode(parts[0]), Map.class);
			if (!"HS256".equals(header.get("alg")) || header.containsKey("crit") || header.containsKey("b64")
					|| (header.containsKey("typ") && !"JWT".equals(header.get("typ")))) throw invalid();
			byte[] signature = Base64.getUrlDecoder().decode(parts[2]);
			if (signature.length != 32 || !MessageDigest.isEqual(sign(parts[0] + "." + parts[1]), signature)) throw invalid();
			Map<?, ?> claims = mapper.readValue(Base64.getUrlDecoder().decode(parts[1]), Map.class);
			long now = clock.instant().getEpochSecond();
			long expiresAt = numericDate(claims.get("exp"));
			Object aud = claims.get("aud");
			if (!issuer.equals(claims.get("iss")) || !(audience.equals(aud) || (aud instanceof List<?> values && values.contains(audience)))
					|| expiresAt <= now
					|| (claims.containsKey("nbf") && numericDate(claims.get("nbf")) > now)
					|| (claims.containsKey("iat") && numericDate(claims.get("iat")) > now)) throw invalid();
			return new TokenIdentity(uuid(claims.get("sub")), uuid(claims.get("jti")), Instant.ofEpochSecond(expiresAt));
		} catch (RuntimeException exception) {
			throw invalid();
		}
	}

	private UUID uuid(Object value) {
		if (!(value instanceof String text)) throw invalid();
		UUID id = UUID.fromString(text);
		if (!id.toString().equalsIgnoreCase(text) || id.equals(new UUID(0, 0))) throw invalid();
		return id;
	}

	private long numericDate(Object value) {
		if (value instanceof Integer number) return number.longValue();
		if (value instanceof Long number) return number;
		if (value instanceof BigInteger number) return number.longValueExact();
		throw invalid();
	}

	private ApiException invalid() {
		return new ApiException(401, "UNAUTHORIZED", "Token inválido ou expirado.");
	}

	private String encode(byte[] value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
	}

	private byte[] sign(String content) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
			return mac.doFinal(content.getBytes(StandardCharsets.US_ASCII));
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Não foi possível processar o JWT.", exception);
		}
	}

	public record TokenIdentity(UUID userId, UUID sessionId, Instant expiresAt) {
	}
}
