package br.com.fiap.agroconnect_api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;
import br.com.fiap.agroconnect_api.model.UserRole;
import br.com.fiap.agroconnect_api.model.User;
import br.com.fiap.agroconnect_api.record.AuthResponse;
import br.com.fiap.agroconnect_api.record.LoginRequest;
import br.com.fiap.agroconnect_api.record.RegisterRequest;
import br.com.fiap.agroconnect_api.repository.UserRepository;
import br.com.fiap.agroconnect_api.repository.SessionRepository;
import br.com.fiap.agroconnect_api.repository.PushTokenRepository;
import br.com.fiap.agroconnect_api.repository.PropertyRepository;
import br.com.fiap.agroconnect_api.security.ApiErrorResponder;
import br.com.fiap.agroconnect_api.security.AuthRateLimitFilter;
import br.com.fiap.agroconnect_api.security.JwtTokenService;
import br.com.fiap.agroconnect_api.security.PasswordHasher;
import br.com.fiap.agroconnect_api.service.AdminUserService;

@SpringBootTest
@ActiveProfiles("test")
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
class AuthenticationTests {

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private FilterChainProxy security;
	@Autowired
	private UserRepository users;
	@Autowired
	private SessionRepository sessions;
	@Autowired
	private PushTokenRepository tokens;
	@Autowired
	private PropertyRepository properties;
	@Autowired
	private PasswordHasher hasher;
	@Autowired
	private JwtTokenService jwt;
	@Autowired
	private PlatformTransactionManager transactionManager;
	@Autowired
	private AdminUserService adminService;
	@Value("${jwt.key}")
	private String key;

	private MockMvc mvc;
	private final JsonMapper mapper = JsonMapper.builder().build();
	private final Set<UUID> created = new HashSet<>();
	private final String password = "Senha-Teste-2026";

	@BeforeEach
	void preparar() {
		mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
	}

	@AfterEach
	void limpar() {
		SecurityContextHolder.clearContext();
		new TransactionTemplate(transactionManager).execute(status -> {
			for (UUID id : created) {
				for (var property : properties.findByUserId(id, org.springframework.data.domain.PageRequest.of(0, 100))) {
					properties.deleteRecord(property.getId());
				}
				tokens.deleteAllForUser(id);
				sessions.deleteAllForUser(id);
				entityManager.createQuery("delete from IdempotencyRecord r where r.user.id = :id")
						.setParameter("id", id).executeUpdate();
				users.deleteById(id);
			}
			return null;
		});
	}

	private String email() {
		return UUID.randomUUID() + "@example.com";
	}

	private AuthResponse register(String email) throws Exception {
		MvcResult result = mvc.perform(post("/api/v1/auth/register").contentType("application/json")
				.content(mapper.writeValueAsString(new RegisterRequest("Pessoa Teste", email, password))))
				.andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/auth/me")).andReturn();
		AuthResponse response = mapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
		created.add(response.user().id());
		return response;
	}

	private AuthResponse login(String email) throws Exception {
		MvcResult result = mvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content(mapper.writeValueAsString(new LoginRequest(email, password))))
				.andExpect(status().isOk()).andReturn();
		return mapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
	}

	private AuthResponse admin() throws Exception {
		User user = new User();
		user.setName("Administrador");
		user.setEmail(email());
		user.setPasswordHash(hasher.hash(password));
		user.setRole(UserRole.ADMIN);
		users.saveAndFlush(user);
		created.add(user.getId());
		return login(user.getEmail());
	}

	private Map<String, Object> claims(String token) {
		return new LinkedHashMap<>(mapper.readValue(Base64.getUrlDecoder().decode(token.split("\\.")[1]), Map.class));
	}

	private String sign(Map<String, Object> header, Map<String, Object> claims, String secret) throws Exception {
		var base64 = Base64.getUrlEncoder().withoutPadding();
		String content = base64.encodeToString(mapper.writeValueAsBytes(header)) + "."
				+ base64.encodeToString(mapper.writeValueAsBytes(claims));
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return content + "." + base64.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.US_ASCII)));
	}

	private void unauthorized(String token) throws Exception {
		mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(header().doesNotExist("Location"));
	}

	@Test
	void cadastroRetornaContratoCSharpEJwtComSessao() throws Exception {
		AuthResponse auth = register("  PESSOA-" + email() + "  ");
		assertEquals("Bearer", auth.tokenType());
		assertEquals(3600, auth.expiresIn());
		assertEquals(auth.user().email().toLowerCase(java.util.Locale.ROOT), auth.user().email());
		var claims = claims(auth.accessToken());
		assertEquals("AgroConnect", claims.get("iss"));
		assertEquals("AgroConnect.Mobile", claims.get("aud"));
		assertEquals(auth.user().id().toString(), claims.get("sub"));
		assertEquals("USER", claims.get("role"));
		assertTrue(sessions.findByIdAndUserId(UUID.fromString((String) claims.get("jti")), auth.user().id()).isPresent());
		mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + auth.accessToken()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(auth.user().id().toString()))
				.andExpect(jsonPath("$.createdAt").value(org.hamcrest.Matchers.endsWith("Z")))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void senhaTemHashCompativelComCSharpEUnicode() {
		String stored = "PBKDF2$AAECAwQFBgcICQoLDA0ODw==$P1F7t/ySB6R15d6y9qeNOGJDxomr9BWhA+UaWto7K30=";
		assertTrue(hasher.verify("Sënha🔐123", stored));
		assertFalse(hasher.verify("outra-senha", stored));
		String first = hasher.hash(password);
		String second = hasher.hash(password);
		assertNotEquals(first, second);
		assertTrue(hasher.verify(password, first));
		assertFalse(hasher.verify(password, "PBKDF2$invalido$invalido"));
		assertFalse(hasher.verify(password, "senha-em-texto"));
	}

	@Test
	void tokenComFormatoDoCSharpSemRoleNemIatContinuaValido() throws Exception {
		var auth = register(email());
		var payload = claims(auth.accessToken());
		payload.remove("role");
		payload.remove("iat");
		String compatible = sign(Map.of("alg", "HS256", "typ", "JWT"), payload, key);
		mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + compatible)).andExpect(status().isOk());
	}

	@Test
	void assinaturaPodeSerVerificadaIndependentemente() throws Exception {
		var auth = register(email());
		var payload = claims(auth.accessToken());
		Map<String, Object> header = mapper.readValue(Base64.getUrlDecoder().decode(auth.accessToken().split("\\.")[0]), Map.class);
		assertEquals(auth.accessToken(), sign(header, payload, key));
	}

	@Test
	void emailDuplicadoIgnoraMaiusculasERetornaCodigoDoCSharp() throws Exception {
		String email = email();
		register(email);
		mvc.perform(post("/api/v1/auth/register").contentType("application/json")
				.content(mapper.writeValueAsString(new RegisterRequest("Outra Pessoa", email.toUpperCase(java.util.Locale.ROOT), password))))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
	}

	@Test
	void cadastrosConcorrentesDoMesmoEmailNaoCriamDuasContas() throws Exception {
		String email = email();
		String body = mapper.writeValueAsString(new RegisterRequest("Pessoa", email, password));
		var executor = Executors.newFixedThreadPool(2);
		try {
			Callable<MvcResult> request = () -> mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(body)).andReturn();
			var results = executor.invokeAll(List.of(request, request));
			int first = results.get(0).get().getResponse().getStatus();
			int second = results.get(1).get().getResponse().getStatus();
			for (var result : results) {
				var response = result.get().getResponse();
				if (response.getStatus() == 201)
					created.add(mapper.readValue(response.getContentAsString(), AuthResponse.class).user().id());
			}
			assertEquals(Set.of(201, 409), Set.of(first, second));
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void cadastroInvalidoRetornaCamposENaoPermitePromoverPeloCorpo() throws Exception {
		mvc.perform(post("/api/v1/auth/register").contentType("application/json")
				.content("{\"name\":\"A\",\"email\":\"pessoa@localhost\",\"password\":\"curta\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.name").exists())
				.andExpect(jsonPath("$.details.email").exists()).andExpect(jsonPath("$.details.password").exists());
		String email = email();
		var result = mvc.perform(post("/api/v1/auth/register").contentType("application/json")
				.content(mapper.writeValueAsString(Map.of("name", "Pessoa", "email", email, "password", password, "role", "ADMIN", "active", true))))
				.andExpect(status().isCreated()).andReturn();
		var auth = mapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
		created.add(auth.user().id());
		assertEquals(UserRole.USER, users.findById(auth.user().id()).orElseThrow().getRole());
		mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + auth.accessToken()))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void loginInvalidoNaoRevelaSeEmailExiste() throws Exception {
		String email = email();
		register(email);
		for (String candidate : List.of(email, email())) {
			mvc.perform(post("/api/v1/auth/login").contentType("application/json")
					.content(mapper.writeValueAsString(new LoginRequest(candidate, "senha-incorreta"))))
					.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
					.andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
		}
		mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{}"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void loginAceitaEmailNormalizadoETokenAntigoNoHeader() throws Exception {
		String email = email();
		register(email);
		var response = mvc.perform(post("/api/v1/auth/login").header("Authorization", "Bearer expirado")
				.contentType("application/json").content(mapper.writeValueAsString(new LoginRequest("  " + email.toUpperCase(java.util.Locale.ROOT) + "  ", password))))
				.andExpect(status().isOk()).andReturn();
		assertNotNull(mapper.readValue(response.getResponse().getContentAsString(), AuthResponse.class).accessToken());
	}

	@Test
	void logoutRevogaSoASessaoAtualERemoveTokenDePush() throws Exception {
		String email = email();
		var first = register(email);
		var second = login(email);
		assertNotEquals(first.accessToken(), second.accessToken());
		mvc.perform(post("/api/v1/me/push-tokens").header("Authorization", "Bearer " + second.accessToken())
				.contentType("application/json").content("{\"token\":\"ExpoPushToken[auth_test]\",\"platform\":\"android\"}"))
				.andExpect(status().isNoContent());
		mvc.perform(post("/api/v1/me/push-tokens").header("Authorization", "Bearer " + second.accessToken())
				.contentType("application/json").content("{\"token\":\"ExpoPushToken[auth_test]\",\"platform\":\"android\"}"))
				.andExpect(status().isNoContent());
		mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + second.accessToken()))
				.andExpect(status().isNoContent()).andExpect(content().string(""));
		unauthorized(second.accessToken());
		assertTrue(tokens.findByToken("ExpoPushToken[auth_test]").isEmpty());
		mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + first.accessToken())).andExpect(status().isOk());
	}

	@Test
	void jwtAlteradoExpiradoOuComAlgoritmoDiferenteEhRejeitado() throws Exception {
		var auth = register(email());
		var payload = claims(auth.accessToken());
		payload.put("exp", Instant.now().minusSeconds(1).getEpochSecond());
		unauthorized(sign(Map.of("alg", "HS256"), payload, key));
		payload = claims(auth.accessToken());
		unauthorized(sign(Map.of("alg", "none"), payload, key));
		unauthorized(sign(Map.of("alg", "HS384"), payload, key));
		unauthorized(sign(Map.of("alg", "HS256"), payload, key + "-outra-chave"));
		String[] parts = auth.accessToken().split("\\.");
		parts[2] = (parts[2].charAt(0) == 'A' ? "B" : "A") + parts[2].substring(1);
		unauthorized(String.join(".", parts));
		unauthorized("nao-e-jwt");
		unauthorized("x".repeat(8193));
	}

	@Test
	void validaIssuerAudienceSubJtiEInicioDeValidade() throws Exception {
		var auth = register(email());
		for (Map.Entry<String, Object> invalid : Map.<String, Object>of(
				"iss", "OutroIssuer", "aud", "OutraAudience", "sub", UUID.randomUUID().toString(),
				"jti", UUID.randomUUID().toString(), "nbf", Instant.now().plusSeconds(60).getEpochSecond(),
				"iat", Instant.now().plusSeconds(60).getEpochSecond(), "exp", "amanha").entrySet()) {
			var payload = claims(auth.accessToken());
			payload.put(invalid.getKey(), invalid.getValue());
			unauthorized(sign(Map.of("alg", "HS256"), payload, key));
		}
		var payload = claims(auth.accessToken());
		payload.put("aud", List.of("OutraAudience", "AgroConnect.Mobile"));
		mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + sign(Map.of("alg", "HS256"), payload, key)))
				.andExpect(status().isOk());
	}

	@Test
	void tokenNaoPodeUsarASessaoDeOutroUsuarioOuInventarAdmin() throws Exception {
		var first = register(email());
		var second = register(email());
		var payload = claims(first.accessToken());
		payload.put("jti", claims(second.accessToken()).get("jti"));
		unauthorized(sign(Map.of("alg", "HS256"), payload, key));
		payload = claims(first.accessToken());
		payload.put("role", "ADMIN");
		mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + sign(Map.of("alg", "HS256"), payload, key)))
				.andExpect(status().isForbidden());
	}

	@Test
	void sessaoExpiradaNoBancoTambemInvalidaJwt() throws Exception {
		var auth = register(email());
		UUID sessionId = UUID.fromString((String) claims(auth.accessToken()).get("jti"));
		new TransactionTemplate(transactionManager).execute(status -> {
			var session = sessions.findById(sessionId).orElseThrow();
			session.setExpiresAt(Instant.now().minusSeconds(1));
			return null;
		});
		unauthorized(auth.accessToken());
	}

	@Test
	void cookieDaAplicacaoAntigaNaoAutenticaApi() throws Exception {
		var session = new MockHttpSession();
		var securityContext = SecurityContextHolder.createEmptyContext();
		securityContext.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("legado", null,
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
		session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
		mvc.perform(get("/api/v1/properties").session(session))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void adminGerenciaPapeisEAlteracaoRevogaTokensAnteriores() throws Exception {
		var admin = admin();
		var user = register(email());
		mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + admin.accessToken()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.page.number").value(1))
				.andExpect(jsonPath("$.items[0].passwordHash").doesNotExist());
		mvc.perform(patch("/api/v1/admin/users/" + user.user().id() + "/role")
				.header("Authorization", "Bearer " + admin.accessToken()).contentType("application/json").content("{\"role\":\"ADMIN\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
		unauthorized(user.accessToken());
		var promoted = login(user.user().email());
		mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + promoted.accessToken())).andExpect(status().isOk());
	}

	@Test
	void adminDesativaContaEReativacaoExigeNovoLogin() throws Exception {
		var admin = admin();
		var user = register(email());
		String path = "/api/v1/admin/users/" + user.user().id() + "/status";
		mvc.perform(patch(path).header("Authorization", "Bearer " + admin.accessToken())
				.contentType("application/json").content("{\"active\":false}")).andExpect(status().isOk());
		unauthorized(user.accessToken());
		mvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content(mapper.writeValueAsString(new LoginRequest(user.user().email(), password))))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
		mvc.perform(patch(path).header("Authorization", "Bearer " + admin.accessToken())
				.contentType("application/json").content("{\"active\":true}")).andExpect(status().isOk());
		unauthorized(user.accessToken());
		login(user.user().email());
	}

	@Test
	void adminNaoPodeRemoverOProprioAcessoENemAceitarPapelDesconhecido() throws Exception {
		var admin = admin();
		String path = "/api/v1/admin/users/" + admin.user().id();
		mvc.perform(patch(path + "/role").header("Authorization", "Bearer " + admin.accessToken())
				.contentType("application/json").content("{\"role\":\"USER\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.role").exists());
		mvc.perform(patch(path + "/status").header("Authorization", "Bearer " + admin.accessToken())
				.contentType("application/json").content("{\"active\":false}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.active").exists());
		mvc.perform(patch(path + "/role").header("Authorization", "Bearer " + admin.accessToken())
				.contentType("application/json").content("{\"role\":\"SUPER\"}"))
				.andExpect(status().isBadRequest());
		assertThrows(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class,
				() -> adminService.list(1, 20));
	}

	@Test
	void identidadeJwtPreservaIsolamentoDosDadosDoDominio() throws Exception {
		var first = register(email());
		var second = register(email());
		var result = mvc.perform(post("/api/v1/properties").header("Authorization", "Bearer " + first.accessToken())
				.contentType("application/json").content("{\"name\":\"Sítio\",\"city\":\"Ibiúna\",\"state\":\"SP\",\"areaHectares\":1}"))
				.andExpect(status().isCreated()).andReturn();
		String location = result.getResponse().getHeader("Location");
		mvc.perform(get(location).header("Authorization", "Bearer " + second.accessToken()))
				.andExpect(status().isNotFound());
		mvc.perform(get(location).header("Authorization", "Bearer " + first.accessToken()))
				.andExpect(status().isOk());
	}

	@Test
	void corsLiberaApenasAsOrigensConfiguradas() throws Exception {
		mvc.perform(options("/api/v1/properties").header("Origin", "http://localhost:8081")
				.header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "authorization,content-type"))
				.andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
		mvc.perform(options("/api/v1/properties").header("Origin", "http://origem-nao-autorizada.example")
				.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}

	@Test
	void limiteDeTentativasRetorna429() throws Exception {
		var filter = new AuthRateLimitFilter();
		ReflectionTestUtils.setField(filter, "clock", Clock.systemUTC());
		ReflectionTestUtils.setField(filter, "responder", new ApiErrorResponder());
		ReflectionTestUtils.setField(filter, "limit", 2);
		filter.initialize();
		for (int attempt = 1; attempt <= 3; attempt++) {
			var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
			var response = new MockHttpServletResponse();
			filter.doFilter(request, response, (req, res) -> ((HttpServletResponse) res).setStatus(204));
			assertEquals(attempt <= 2 ? 204 : 429, response.getStatus());
		}
	}

	@Test
	void configuracaoNaoAceitaChaveFracaForaDeDesenvolvimento() {
		var service = new JwtTokenService();
		ReflectionTestUtils.setField(service, "environment", new MockEnvironment());
		ReflectionTestUtils.setField(service, "clock", Clock.systemUTC());
		ReflectionTestUtils.setField(service, "key", "curta");
		ReflectionTestUtils.setField(service, "issuer", "AgroConnect");
		ReflectionTestUtils.setField(service, "audience", "AgroConnect.Mobile");
		assertThrows(IllegalStateException.class, service::initialize);
	}
}

