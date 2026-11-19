package br.com.fiap.agroconnect_api;

import br.com.fiap.agroconnect_api.record.AuthResponse;
import br.com.fiap.agroconnect_api.record.RegisterRequest;
import br.com.fiap.agroconnect_api.repository.AiInteractionRepository;
import br.com.fiap.agroconnect_api.repository.DeviceRepository;
import br.com.fiap.agroconnect_api.repository.PropertyRepository;
import br.com.fiap.agroconnect_api.repository.PushTokenRepository;
import br.com.fiap.agroconnect_api.repository.SessionRepository;
import br.com.fiap.agroconnect_api.repository.UserRepository;
import br.com.fiap.agroconnect_api.service.AuthService;
import br.com.fiap.agroconnect_api.service.DemoDataService;
import br.com.fiap.agroconnect_api.service.DomainService;
import br.com.fiap.testconfig.AiTestConfiguration.FakeChatModel;
import br.com.fiap.testconfig.AiTestConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(AiTestConfiguration.class)
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
class AiHttpFlowTests {

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private FilterChainProxy security;
	@Autowired
	private AuthService auth;
	@Autowired
	private DomainService domain;
	@Autowired
	private DemoDataService demo;
	@Autowired
	private FakeChatModel model;
	@Autowired
	private DeviceRepository devices;
	@Autowired
	private PropertyRepository properties;
	@Autowired
	private AiInteractionRepository interactions;
	@Autowired
	private UserRepository users;
	@Autowired
	private SessionRepository sessions;
	@Autowired
	private PushTokenRepository tokens;
	@Autowired
	private PlatformTransactionManager transactionManager;

	private AuthResponse session;
	private UUID deviceId;
	private MockMvc mvc;

	@BeforeEach
	void preparar() {
		model.reset();
		session = auth.register(new RegisterRequest("Produtor", UUID.randomUUID() + "@example.com", "Senha-Teste-2026"));
		demo.seed(session.user().id());
		deviceId = devices.findByCode("ESP32-001").orElseThrow().getId();
		mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
	}

	@AfterEach
	void limpar() {
		model.reset();
		new TransactionTemplate(transactionManager).execute(status -> {
			UUID owner = session.user().id();
			interactions.deleteAll(interactions.findByUserId(owner, PageRequest.of(0, 100)).getContent());
			interactions.flush();
			for (var property : properties.findByUserId(owner, PageRequest.of(0, 100))) domain.deleteProperty(owner, property.getId());
			tokens.deleteAllForUser(owner);
			sessions.deleteAllForUser(owner);
			users.deleteById(owner);
			return null;
		});
	}

	@Test
	void jwtSpringAiEDadosPersistidosFuncionamNoMesmoFluxoSemTransacaoDuranteModelo() throws Exception {
		model.setBeforeResponse(() -> assertFalse(TransactionSynchronizationManager.isActualTransactionActive()));
		String body = "{\"deviceId\":\"" + deviceId + "\"}";
		mvc.perform(post("/api/v1/recommendations").contentType("application/json").content(body))
				.andExpect(status().isUnauthorized());
		assertEquals(0, model.calls());
		mvc.perform(post("/api/v1/recommendations").header("Authorization", "Bearer " + session.accessToken())
				.contentType("application/json").content(body))
				.andExpect(status().isOk()).andExpect(jsonPath("$.items[0].message").exists())
				.andExpect(jsonPath("$.items[0].simulatedData").value(true));
		assertEquals(1, model.calls());
		assertEquals(1, interactions.count());
	}
}
