package br.com.fiap.agroconnect_api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import br.com.fiap.testconfig.AiTestConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolationException;
import tools.jackson.databind.json.JsonMapper;
import br.com.fiap.agroconnect_api.control.ApiExceptionHandler;
import br.com.fiap.agroconnect_api.model.User;
import br.com.fiap.agroconnect_api.model.Session;
import br.com.fiap.agroconnect_api.record.PropertyInput;
import br.com.fiap.agroconnect_api.record.FieldInput;
import br.com.fiap.agroconnect_api.record.DeviceInput;
import br.com.fiap.agroconnect_api.record.SensorInput;
import br.com.fiap.agroconnect_api.record.ReadingInput;
import br.com.fiap.agroconnect_api.record.AlertInput;
import br.com.fiap.agroconnect_api.record.AlertUpdateInput;
import br.com.fiap.agroconnect_api.record.RecommendationInput;
import br.com.fiap.agroconnect_api.record.RecommendationWriteInput;
import br.com.fiap.agroconnect_api.record.CropInput;
import br.com.fiap.agroconnect_api.record.WeatherHistoryInput;
import br.com.fiap.agroconnect_api.record.AiInteractionInput;
import br.com.fiap.agroconnect_api.record.DeviceEventInput;
import br.com.fiap.agroconnect_api.record.PlantingInput;
import br.com.fiap.agroconnect_api.record.PushTokenInput;
import br.com.fiap.agroconnect_api.record.UserInput;
import br.com.fiap.agroconnect_api.record.RegisterRequest;
import br.com.fiap.agroconnect_api.service.DomainService;
import br.com.fiap.agroconnect_api.service.MonitoringService;
import br.com.fiap.agroconnect_api.service.AlertService;
import br.com.fiap.agroconnect_api.service.AdditionalDomainService;
import br.com.fiap.agroconnect_api.service.PushTokenService;
import br.com.fiap.agroconnect_api.service.ApiException;
import br.com.fiap.agroconnect_api.repository.UserRepository;
import br.com.fiap.agroconnect_api.repository.DeviceRepository;
import br.com.fiap.agroconnect_api.repository.SensorRepository;
import br.com.fiap.agroconnect_api.repository.ReadingRepository;
import br.com.fiap.agroconnect_api.repository.AlertRepository;
import br.com.fiap.agroconnect_api.repository.RecommendationRepository;
import br.com.fiap.agroconnect_api.repository.CropRepository;
import br.com.fiap.agroconnect_api.repository.SessionRepository;
import br.com.fiap.agroconnect_api.repository.PushTokenRepository;

@SpringBootTest
@Import(AiTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
class CompleteDomainTests {

	@Autowired private ApplicationContext context;
	@Autowired private DomainService domain;
	@Autowired private MonitoringService monitoring;
	@Autowired private AlertService alertService;
	@Autowired private AdditionalDomainService additional;
	@Autowired private PushTokenService push;
	@Autowired private UserRepository users;
	@Autowired private DeviceRepository devices;
	@Autowired private SensorRepository sensors;
	@Autowired private ReadingRepository readings;
	@Autowired private AlertRepository alerts;
	@Autowired private RecommendationRepository recommendations;
	@Autowired private CropRepository crops;
	@Autowired private SessionRepository sessions;
	@Autowired private PushTokenRepository tokens;
	@Autowired private EntityManager em;
	@Autowired private Validator validator;
	@Autowired private ApiExceptionHandler advice;

	private UUID owner;
	private UUID other;
	private UUID propertyId;
	private UUID fieldId;
	private UUID deviceId;
	private UUID sensorId;
	private Principal principal;
	private MockMvc mvc;
	private final Instant at = Instant.parse("2026-11-15T10:00:00Z");
	private final JsonMapper mapper = JsonMapper.builder().build();

	@BeforeEach
	void preparar() {
		owner = user();
		other = user();
		principal = () -> owner.toString();
		propertyId = domain.createProperty(owner, new PropertyInput("Fazenda", "Ibiúna", "SP", BigDecimal.ONE, null)).id();
		fieldId = domain.createField(owner, propertyId, new FieldInput("Talhão", BigDecimal.ONE)).id();
		deviceId = monitoring.createDevice(owner, device(fieldId, UUID.randomUUID().toString())).id();
		sensorId = monitoring.createSensor(owner, sensor(deviceId)).id();
		mvc = MockMvcBuilders.standaloneSetup(context.getBeansWithAnnotation(RestController.class).values().stream()
				.filter(bean -> AopUtils.getTargetClass(bean).getPackageName().equals("br.com.fiap.agroconnect_api.control"))
				.toArray())
				.setControllerAdvice(advice).build();
	}

	private UUID user() {
		User user = new User();
		user.setName("Produtor");
		user.setEmail(UUID.randomUUID() + "@example.com");
		user.setPasswordHash("hash-apenas-teste");
		return users.saveAndFlush(user).getId();
	}

	private DeviceInput device(UUID field, String code) {
		return new DeviceInput(field, code, "Estação", null, -60, "1.0.0", "WIFI", true, new BigDecimal("30"));
	}

	private SensorInput sensor(UUID device) {
		return new SensorInput(device, "SOIL_MOISTURE", "Umidade", "%", null, true, null);
	}

	private ReadingInput reading(BigDecimal value, String quality, Instant recordedAt) {
		return new ReadingInput(sensorId, value, quality, recordedAt, recordedAt, true, "SIMULATED", null);
	}

	private AlertInput alert(String severity) {
		return new AlertInput(deviceId, severity, "Alerta do solo", "Mensagem do alerta", "Motivo do alerta");
	}

	@Test
	void statusDeComunicacaoRespeitaLimitesExatosDoCSharp() {
		assertEquals("UNKNOWN", MonitoringService.deviceStatus(null, at));
		assertEquals("ONLINE", MonitoringService.deviceStatus(at.minusSeconds(300), at));
		assertEquals("UNSTABLE", MonitoringService.deviceStatus(at.minusMillis(300001), at));
		assertEquals("UNSTABLE", MonitoringService.deviceStatus(at.minusSeconds(1800), at));
		assertEquals("OFFLINE", MonitoringService.deviceStatus(at.minusMillis(1800001), at));
	}

	@Test
	void crudDispositivosSensoresLeiturasComPersistencia() {
		monitoring.updateDevice(owner, deviceId, device(fieldId, "ESP32-NOVO"));
		monitoring.updateSensor(owner, sensorId, new SensorInput(deviceId, "AIR_TEMPERATURE", "Temperatura", "°C", null, true, null));
		var row = monitoring.createReading(owner, reading(new BigDecimal("20.123"), "VALID", at));
		monitoring.updateReading(owner, row.id(), reading(new BigDecimal("21.123"), "VALID", at.plusSeconds(60)));
		em.flush(); em.clear();
		assertEquals("ESP32-NOVO", monitoring.getDevice(owner, deviceId).code());
		assertEquals("AIR_TEMPERATURE", monitoring.getSensor(owner, sensorId).type());
		assertEquals(0, new BigDecimal("21.123").compareTo(monitoring.getReading(owner, row.id()).value()));
		assertEquals(1, monitoring.listReadings(owner, sensorId, 1, 20).items().size());
		monitoring.deleteReading(owner, row.id());
		em.flush();
		assertFalse(readings.existsById(row.id()));
	}

	@Test
	void ultimasLeiturasMostramFalhaSemValorEAlertaUsaUltimaValida() {
		monitoring.createReading(owner, reading(new BigDecimal("20"), "VALID", at));
		monitoring.createReading(owner, reading(null, "SENSOR_ERROR", at.plusSeconds(60)));
		var latest = monitoring.latest(owner, deviceId);
		assertEquals(1, latest.readings().size());
		assertEquals("SENSOR_ERROR", latest.readings().get(0).quality());
		assertNull(latest.readings().get(0).value());
		assertEquals(0, new BigDecimal("20").compareTo(alertService.createAlert(owner, alert("WARNING")).basedOn().get(0).value()));
	}

	@Test
	void historicoAgregaSomenteValoresValidosEMantemLacunas() {
		monitoring.createReading(owner, reading(new BigDecimal("10"), "VALID", at));
		monitoring.createReading(owner, reading(new BigDecimal("20"), "VALID", at.plusSeconds(60)));
		monitoring.createReading(owner, reading(null, "UNCALIBRATED", at.plusSeconds(120)));
		monitoring.createReading(owner, reading(null, "SENSOR_ERROR", at.plusSeconds(900)));
		var history = monitoring.history(owner, deviceId, sensorId, null, at, at.plusSeconds(1200), "5m");
		assertEquals(2, history.items().size());
		assertEquals(3, history.items().get(0).sampleCount());
		assertEquals(0, new BigDecimal("15").compareTo(history.items().get(0).value()));
		assertNull(history.items().get(1).value());
		assertEquals("SENSOR_ERROR", history.items().get(1).quality());
		assertEquals(0, new BigDecimal("15").compareTo(history.summary().avg()));
		assertTrue(monitoring.history(owner, deviceId, sensorId, null, at.minusSeconds(100), at.minusSeconds(1), "raw").items().isEmpty());
	}

	@Test
	void historicoRejeitaPeriodoIntervaloESeletorInvalidos() throws Exception {
		assertThrows(ApiException.class, () -> monitoring.history(owner, deviceId, null, null, at, at.plusSeconds(60), "raw"));
		assertThrows(ApiException.class, () -> monitoring.history(owner, deviceId, sensorId, null, at, at.plus(Duration.ofDays(367)), "1h"));
		assertThrows(ApiException.class, () -> monitoring.history(owner, deviceId, sensorId, null, at, at, "raw"));
		mvc.perform(get("/api/v1/devices/" + deviceId + "/readings").principal(principal)
				.param("sensorId", sensorId.toString()).param("interval", "2m"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.interval").exists());
		mvc.perform(get("/api/v1/devices/" + deviceId + "/readings").principal(principal)
				.param("sensorId", sensorId.toString()).param("from", "data-invalida"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rotasMobileDispositivosLeiturasERecomendacoes() throws Exception {
		monitoring.createReading(owner, reading(new BigDecimal("10"), "VALID", at));
		alertService.createAlert(owner, alert("WARNING"));
		mvc.perform(get("/api/v1/devices").principal(principal)).andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].fieldId").value(fieldId.toString()))
				.andExpect(jsonPath("$.page.number").value(1));
		mvc.perform(get("/api/v1/devices/" + deviceId + "/sensors").principal(principal))
				.andExpect(status().isOk()).andExpect(jsonPath("$.items[0].type").value("SOIL_MOISTURE"));
		mvc.perform(get("/api/v1/devices/" + deviceId + "/readings/latest").principal(principal))
				.andExpect(status().isOk()).andExpect(jsonPath("$.readings[0].quality").value("VALID"))
				.andExpect(jsonPath("$.recordedAt").value(org.hamcrest.Matchers.endsWith("Z")));
		mvc.perform(get("/api/v1/devices/" + deviceId + "/readings").principal(principal)
				.param("sensorId", sensorId.toString()).param("from", at.minusSeconds(1).toString()).param("to", at.plusSeconds(60).toString()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.summary.avg").value(10));
		mvc.perform(post("/api/v1/recommendations").principal(principal).contentType("application/json")
				.content(mapper.writeValueAsString(new RecommendationInput(deviceId))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.items[0].simulatedData").value(true))
				.andExpect(jsonPath("$.items[0].basedOn[0].type").value("SOIL_MOISTURE"));
	}

	@Test
	void alertasOrdenadosFiltradosLeituraIdempotenteEAuditoriaAutomatica() {
		var info = alertService.createAlert(owner, alert("INFO"));
		var critical = alertService.createAlert(owner, alert("CRITICAL"));
		assertEquals(critical.id(), alertService.listAlerts(owner, null, 1, 20).items().get(0).id());
		alertService.markRead(owner, info.id());
		alertService.markRead(owner, info.id());
		assertEquals(1, alertService.listAlerts(owner, "READ", 1, 20).items().size());
		assertEquals(3, alertService.listAudits(owner, 1, 20).items().size());
		alertService.deleteAlert(owner, info.id());
		em.flush(); em.clear();
		assertFalse(alerts.existsById(info.id()));
		assertEquals(4, alertService.listAudits(owner, 1, 20).items().size());
		assertTrue(alertService.listAudits(other, 1, 20).items().isEmpty());
	}

	@Test
	void recomendacoesPersistemRelacaoComLeiturasSemDuplicarGeracao() {
		var row = monitoring.createReading(owner, reading(new BigDecimal("10"), "VALID", at));
		var alert = alertService.createAlert(owner, alert("WARNING"));
		var first = alertService.generate(owner, new RecommendationInput(deviceId));
		var again = alertService.generate(owner, new RecommendationInput(deviceId));
		assertEquals(first.items().get(0).id(), again.items().get(0).id());
		em.flush(); em.clear();
		assertEquals(1, recommendations.count());
		assertEquals(row.id(), recommendations.findById(first.items().get(0).id()).orElseThrow().getReadings().get(0).getId());
		alertService.updateAlert(owner, alert.id(), new AlertUpdateInput("WARNING", "RESOLVED", "Resolvido", "Mensagem resolvida", "Motivo resolvido"));
		assertTrue(alertService.generate(owner, new RecommendationInput(deviceId)).items().isEmpty());
		assertEquals(1, alertService.listRecommendations(owner, deviceId, 1, 20).items().size());
	}

	@Test
	void crudRecomendacoesManuaisEValidacaoDaOrigemDasLeituras() {
		var row = monitoring.createReading(owner, reading(new BigDecimal("10"), "VALID", at));
		var input = new RecommendationWriteInput(deviceId, "WARNING", "Título", "Mensagem", "Motivo", "Aviso informativo", true, "modelo", List.of(row.id()));
		var item = alertService.createRecommendation(owner, input);
		alertService.updateRecommendation(owner, item.id(), input);
		assertEquals(1, alertService.getRecommendation(owner, item.id()).basedOn().size());
		var second = monitoring.createDevice(owner, device(fieldId, "OUTRO-DISPOSITIVO")).id();
		var bad = new RecommendationWriteInput(second, "WARNING", "Título", "Mensagem", "Motivo", "Aviso informativo", true, null, List.of(row.id()));
		assertThrows(ApiException.class, () -> alertService.createRecommendation(owner, bad));
		alertService.deleteRecommendation(owner, item.id());
		em.flush();
		assertFalse(recommendations.existsById(item.id()));
	}

	@Test
	void isolamentoEntreUsuariosCobreTodoMonitoramento() throws Exception {
		var reading = monitoring.createReading(owner, reading(BigDecimal.ONE, "VALID", at));
		var alert = alertService.createAlert(owner, alert("INFO"));
		var recommendation = alertService.generate(owner, new RecommendationInput(deviceId)).items().get(0);
		assertTrue(monitoring.listDevices(other, null, 1, 20).items().isEmpty());
		assertTrue(alertService.listAlerts(other, null, 1, 20).items().isEmpty());
		assertThrows(ApiException.class, () -> monitoring.getDevice(other, deviceId));
		assertThrows(ApiException.class, () -> monitoring.getSensor(other, sensorId));
		assertThrows(ApiException.class, () -> monitoring.getReading(other, reading.id()));
		assertThrows(ApiException.class, () -> monitoring.latest(other, deviceId));
		assertThrows(ApiException.class, () -> monitoring.listSensors(other, deviceId, 1, 20));
		assertThrows(ApiException.class, () -> monitoring.createSensor(other, sensor(deviceId)));
		assertThrows(ApiException.class, () -> alertService.getAlert(other, alert.id()));
		assertThrows(ApiException.class, () -> alertService.markRead(other, alert.id()));
		assertThrows(ApiException.class, () -> alertService.getRecommendation(other, recommendation.id()));
		mvc.perform(get("/api/v1/devices/" + deviceId).principal(() -> other.toString()))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void culturasLigadasAoPlantioSemAlterarRespostaMobile() {
		var crop = additional.createCrop(owner, new CropInput("ALFACE", "Alface", "Lactuca sativa", "Descrição"));
		var planting = domain.createPlanting(owner, fieldId, new PlantingInput("Alface", at, "SEEDLING", crop.id(), "Lisa", "ACTIVE", "Teste"));
		assertEquals(crop.id(), domain.getPlantingDetails(owner, fieldId, planting.id()).cropId());
		assertThrows(ApiException.class, () -> additional.getCrop(other, crop.id()));
		additional.updateCrop(owner, crop.id(), new CropInput("ALFACE", "Alface atualizada", null, null));
		assertEquals("Alface atualizada", additional.listCrop(owner, 1, 20).items().get(0).name());
		additional.deleteCrop(owner, crop.id());
		em.flush(); em.clear();
		assertNull(domain.getPlantingDetails(owner, fieldId, planting.id()).cropId());
		assertEquals("Alface", domain.getPlanting(owner, fieldId, planting.id()).cropName());
	}

	@Test
	void eventosClimaEInteracoesPossuemCrudEPaginacao() {
		var eventInput = new DeviceEventInput(deviceId, "CONNECTED", "Dispositivo conectado", at, "SIMULATED");
		var event = additional.createDeviceEvent(owner, eventInput);
		additional.updateDeviceEvent(owner, event.id(), eventInput);
		assertEquals(1, additional.listDeviceEvent(owner, deviceId, 1, 20).items().size());
		assertEquals("CONNECTED", additional.getDeviceEvent(owner, event.id()).type());
		var weatherInput = new WeatherHistoryInput(propertyId, at, new BigDecimal("25"), new BigDecimal("60"), BigDecimal.ZERO, BigDecimal.ONE, "SIMULATED");
		var weather = additional.createWeatherHistory(owner, weatherInput);
		additional.updateWeatherHistory(owner, weather.id(), weatherInput);
		assertEquals(1, additional.listWeatherHistory(owner, propertyId, 1, 20).items().size());
		assertEquals("SIMULATED", additional.getWeatherHistory(owner, weather.id()).origin());
		var aiInput = new AiInteractionInput(propertyId, deviceId, "Como está o solo?", "Resposta registrada", "{\"source\":\"demo\"}", "modelo");
		var ai = additional.createAiInteraction(owner, aiInput);
		additional.updateAiInteraction(owner, ai.id(), aiInput);
		assertEquals(1, additional.listAiInteraction(owner, 1, 20).items().size());
		assertEquals("Resposta registrada", additional.getAiInteraction(owner, ai.id()).response());
		assertThrows(ApiException.class, () -> additional.getAiInteraction(other, ai.id()));
		assertThrows(ApiException.class, () -> additional.getWeatherHistory(other, weather.id()));
		assertThrows(ApiException.class, () -> additional.getDeviceEvent(other, event.id()));
		additional.deleteAiInteraction(owner, ai.id());
		additional.deleteWeatherHistory(owner, weather.id());
		additional.deleteDeviceEvent(owner, event.id());
		em.flush();
		assertTrue(additional.listAiInteraction(owner, 1, 20).items().isEmpty());
	}

	@Test
	void interacaoNaoPodeCombinarDispositivoDeOutraPropriedade() {
		var another = domain.createProperty(owner, new PropertyInput("Outro sítio", "Ibiúna", "SP", BigDecimal.ONE, null));
		assertThrows(ApiException.class, () -> additional.createAiInteraction(owner,
				new AiInteractionInput(another.id(), deviceId, "Pergunta", null, null, null)));
		assertThrows(ApiException.class, () -> additional.createAiInteraction(other,
				new AiInteractionInput(propertyId, null, "Pergunta", null, null, null)));
	}

	@Test
	void validacoesCruzadasDeLeiturasETokensEEmail() {
		assertFalse(validator.validate(reading(null, "VALID", at)).isEmpty());
		assertFalse(validator.validate(reading(BigDecimal.ONE, "SENSOR_ERROR", at)).isEmpty());
		assertFalse(validator.validate(new ReadingInput(sensorId, BigDecimal.ONE, "VALID", at, at.minusSeconds(1), true, "SIMULATED", null)).isEmpty());
		assertFalse(validator.validate(new PushTokenInput("invalido", "android")).isEmpty());
		assertTrue(validator.validate(new PushTokenInput("ExponentPushToken[test_123]", "ANDROID")).isEmpty());
		assertTrue(validator.validate(new UserInput("Pessoa", "pessoa@example.com")).isEmpty());
		assertFalse(validator.validate(new UserInput("Pessoa", "pessoa@localhost")).isEmpty());
		assertFalse(validator.validate(new RegisterRequest("Pessoa", "pessoa@example.com", "curta")).isEmpty());
		assertFalse(validator.validate(new RegisterRequest("Pessoa", "pessoa@localhost", "senha-valida")).isEmpty());
		assertFalse(validator.validate(new RecommendationInput(new UUID(0, 0))).isEmpty());
	}

	@Test
	void exclusoesIndividuaisDeSensorEDispositivoPreservamConsistencia() {
		var row = monitoring.createReading(owner, reading(BigDecimal.ONE, "VALID", at));
		monitoring.deleteSensor(owner, sensorId);
		em.flush(); em.clear();
		assertFalse(sensors.existsById(sensorId));
		assertFalse(readings.existsById(row.id()));
		assertTrue(devices.existsById(deviceId));
		sensorId = monitoring.createSensor(owner, sensor(deviceId)).id();
		monitoring.createReading(owner, reading(BigDecimal.ONE, "VALID", at));
		alertService.createAlert(owner, alert("WARNING"));
		monitoring.deleteDevice(owner, deviceId);
		em.flush(); em.clear();
		assertFalse(devices.existsById(deviceId));
		assertFalse(sensors.existsById(sensorId));
		assertEquals(2, alertService.listAudits(owner, 1, 20).items().size());
	}

	@Test
	void validacoesHttpParaNovosRecursos() throws Exception {
		mvc.perform(post("/api/v1/devices").principal(principal).contentType("application/json")
				.content("{\"fieldId\":\"00000000-0000-0000-0000-000000000000\",\"code\":\"\",\"name\":\"\",\"firmwareVersion\":\"1\",\"rssi\":20}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.fieldId").exists()).andExpect(jsonPath("$.details.rssi").exists());
		mvc.perform(post("/api/v1/ai-interactions").principal(principal).contentType("application/json")
				.content("{\"question\":\"Pergunta\",\"context\":\"nao-json\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.context").exists());
		mvc.perform(post("/api/v1/weather-history").principal(principal).contentType("application/json")
				.content(mapper.writeValueAsString(Map.of("propertyId", propertyId, "observedAt", at.toString(), "relativeHumidity", 101, "origin", "SIMULATED"))))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.relativeHumidity").exists());
		mvc.perform(get("/api/v1/alerts").principal(principal).param("status", "OUTRO"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.status").exists());
	}

	@Test
	void conflitoDeCodigoDuplicadoRetorna409SemExporSql() throws Exception {
		mvc.perform(post("/api/v1/devices").principal(principal).contentType("application/json")
				.content(mapper.writeValueAsString(device(fieldId, monitoring.getDevice(owner, deviceId).code()))))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLICT"))
				.andExpect(jsonPath("$.message").value("Os dados conflitam com um registro existente."));
	}

	@Test
	void perfilDoUsuarioNaoExpoeSenhaERejeitaEmailDuplicado() throws Exception {
		additional.updateUser(owner, new UserInput("Pessoa Atualizada", "ATUALIZADA@example.com"));
		mvc.perform(get("/api/v1/users/me").principal(principal)).andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("atualizada@example.com")).andExpect(jsonPath("$.passwordHash").doesNotExist());
		assertThrows(ApiException.class, () -> additional.updateUser(other, new UserInput("Outra", "atualizada@example.com")));
	}

	@Test
	void cadastroDeTokenDependeDeSessaoValidaERepeticaoNaoDuplica() {
		Session session = new Session();
		session.setUser(users.findById(owner).orElseThrow());
		session.setExpiresAt(Instant.now().plusSeconds(3600));
		sessions.saveAndFlush(session);
		PushTokenInput input = new PushTokenInput("ExpoPushToken[test]", "android");
		push.register(owner, session.getId(), input);
		push.register(owner, session.getId(), input);
		assertEquals(1, tokens.count());
		assertThrows(ApiException.class, () -> push.register(other, session.getId(), input));
		assertThrows(ApiException.class, () -> push.register(owner, null, input));
		session.setExpiresAt(Instant.now().minusSeconds(1));
		assertThrows(ApiException.class, () -> push.register(owner, session.getId(), input));
	}

	@Test
	void exclusaoDaPropriedadeRemoveDependentesEPreservaAuditoria() {
		monitoring.createReading(owner, reading(BigDecimal.ONE, "VALID", at));
		var alert = alertService.createAlert(owner, alert("INFO"));
		alertService.generate(owner, new RecommendationInput(deviceId));
		additional.createDeviceEvent(owner, new DeviceEventInput(deviceId, "CONNECTED", "Evento", at, "SIMULATED"));
		additional.createWeatherHistory(owner, new WeatherHistoryInput(propertyId, at, BigDecimal.TEN, null, null, null, "SIMULATED"));
		var ai = additional.createAiInteraction(owner, new AiInteractionInput(propertyId, deviceId, "Pergunta", null, null, null));
		domain.deleteProperty(owner, propertyId);
		em.flush(); em.clear();
		assertFalse(devices.existsById(deviceId));
		assertFalse(sensors.existsById(sensorId));
		assertEquals(0, readings.count());
		assertEquals(0, recommendations.count());
		assertThrows(ApiException.class, () -> alertService.getAlert(owner, alert.id()));
		assertEquals(2, alertService.listAudits(owner, 1, 20).items().size());
		assertNull(additional.getAiInteraction(owner, ai.id()).propertyId());
		assertNull(additional.getAiInteraction(owner, ai.id()).deviceId());
	}
}

