package br.com.fiap.agroconnect_api;

import br.com.fiap.agroconnect_api.control.ApiExceptionHandler;
import br.com.fiap.agroconnect_api.control.RecommendationController;
import br.com.fiap.agroconnect_api.model.Recommendation;
import br.com.fiap.agroconnect_api.model.User;
import br.com.fiap.agroconnect_api.record.RecommendationInput;
import br.com.fiap.agroconnect_api.repository.AlertRepository;
import br.com.fiap.agroconnect_api.repository.DeviceRepository;
import br.com.fiap.agroconnect_api.repository.RecommendationRepository;
import br.com.fiap.agroconnect_api.repository.UserRepository;
import br.com.fiap.agroconnect_api.service.ApiException;
import br.com.fiap.agroconnect_api.service.DemoDataService;
import br.com.fiap.agroconnect_api.service.RecommendationService;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
class AiNotConfiguredTests {

	@Autowired
	private UserRepository users;
	@Autowired
	private DeviceRepository devices;
	@Autowired
	private AlertRepository alerts;
	@Autowired
	private RecommendationRepository recommendations;
	@Autowired
	private DemoDataService demo;
	@Autowired
	private RecommendationService service;
	@Autowired
	private RecommendationController controller;
	@Autowired
	private ApiExceptionHandler advice;

	private User owner() {
		User user = new User();
		user.setName("Produtor");
		user.setEmail(UUID.randomUUID() + "@example.com");
		user.setPasswordHash("hash-apenas-teste");
		users.saveAndFlush(user);
		demo.seed(user.getId());
		return user;
	}

	@Test
	void semModeloRetorna503MesmoQuandoExisteRecomendacaoDemonstrativa() throws Exception {
		User user = owner();
		var device = devices.findByCode("ESP32-001").orElseThrow();
		var alert = alerts.findByDeviceIdAndDeviceFieldPropertyUserId(device.getId(), user.getId()).get(0);
		Recommendation old = new Recommendation();
		old.setDevice(device);
		old.setSourceAlertId(alert.getId());
		old.setPriority(alert.getSeverity());
		old.setTitle(alert.getTitle());
		old.setMessage(alert.getMessage());
		old.setReason(alert.getReason());
		old.setDisclaimer("Texto demonstrativo antigo.");
		old.setBasedOnJson(alert.getBasedOnJson());
		old.setReadings(new ArrayList<>(alert.getReadings()));
		old.setSimulatedData(true);
		recommendations.saveAndFlush(old);
		var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(advice).build();
		mvc.perform(post("/api/v1/recommendations").principal(() -> user.getId().toString())
				.contentType("application/json").content("{\"deviceId\":\"" + device.getId() + "\"}"))
				.andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("AI_NOT_CONFIGURED"));
		assertFalse(recommendations.findById(old.getId()).orElseThrow().getAiGenerated());
	}

	@Test
	void acessoAlheioRetorna404AntesDaVerificacaoDoProvedor() {
		owner();
		UUID deviceId = devices.findByCode("ESP32-001").orElseThrow().getId();
		ApiException error = assertThrows(ApiException.class, () -> service.generate(UUID.randomUUID(), new RecommendationInput(deviceId)));
		assertEquals(404, error.getStatus());
	}
}
