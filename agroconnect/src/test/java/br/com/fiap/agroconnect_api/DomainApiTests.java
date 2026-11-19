package br.com.fiap.agroconnect_api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.agroconnect_api.control.*;
import br.com.fiap.agroconnect_api.model.User;
import br.com.fiap.agroconnect_api.record.*;
import br.com.fiap.agroconnect_api.repository.*;
import br.com.fiap.agroconnect_api.service.*;
import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
class DomainApiTests {

	@Autowired private DomainService service;
	@Autowired private UserRepository users;
	@Autowired private PropertyRepository properties;
	@Autowired private FieldRepository fields;
	@Autowired private PlantingRepository plantings;
	@Autowired private EntityManager entityManager;
	@Autowired private PropertyController propertyController;
	@Autowired private FieldController fieldController;
	@Autowired private PlantingController plantingController;
	@Autowired private ApiExceptionHandler advice;
	@Autowired private WebApplicationContext context;
	@Autowired private FilterChainProxy securityFilterChain;

	private MockMvc mvc;
	private UUID owner;
	private UUID outsider;
	private Principal principal;

	@BeforeEach
	void preparar() {
		owner = criarUsuario();
		outsider = criarUsuario();
		principal = () -> owner.toString();
		// Testa MVC e persistência reais; autenticação/JWT pertence à próxima etapa.
		mvc = MockMvcBuilders.standaloneSetup(propertyController, fieldController, plantingController)
				.setControllerAdvice(advice).build();
	}

	private UUID criarUsuario() {
		User user = new User();
		user.setName("Produtor");
		user.setEmail(UUID.randomUUID() + "@example.com");
		user.setPasswordHash("hash-usado-apenas-pelo-teste");
		return users.saveAndFlush(user).getId();
	}

	private PropertyInput input(String name) {
		return new PropertyInput(name, "São Paulo", "sp", new BigDecimal("1.234567"), "Descrição");
	}

	@Test
	void crudRelacionadoEPersistenciaECascata() {
		var property = service.createProperty(owner, input("Fazenda"));
		var field = service.createField(owner, property.id(), new FieldInput("Talhão", BigDecimal.ONE));
		var planting = service.createPlanting(owner, field.id(),
				new PlantingInput("Tomate", Instant.parse("2026-11-15T12:00:00Z"), "seedling"));
		entityManager.flush();
		entityManager.clear();
		assertEquals("SP", service.getProperty(owner, property.id()).state());
		assertEquals(property.id(), service.getField(owner, field.id()).propertyId());
		assertEquals("SEEDLING", service.getPlanting(owner, field.id(), planting.id()).stage());
		service.updateField(owner, field.id(), new FieldInput("Talhão novo", new BigDecimal("2")));
		service.updatePlanting(owner, field.id(), planting.id(),
				new PlantingInput("Milho", Instant.now(), "HARVEST"));
		entityManager.flush();
		entityManager.clear();
		assertEquals("Talhão novo", service.getField(owner, field.id()).name());
		assertEquals("Milho", service.getPlanting(owner, field.id(), planting.id()).cropName());
		service.deleteProperty(owner, property.id());
		entityManager.flush();
		entityManager.clear();
		assertFalse(properties.existsById(property.id()));
		assertFalse(fields.existsById(field.id()));
		assertFalse(plantings.existsById(planting.id()));
	}

	@Test
	void exclusoesIndividuais() {
		var property = service.createProperty(owner, input("Fazenda"));
		var field = service.createField(owner, property.id(), new FieldInput("Talhão", BigDecimal.ONE));
		var planting = service.createPlanting(owner, field.id(),
				new PlantingInput("Tomate", Instant.now(), "SEEDLING"));
		service.deletePlanting(owner, field.id(), planting.id());
		entityManager.flush();
		assertFalse(plantings.existsById(planting.id()));
		service.deleteField(owner, field.id());
		entityManager.flush();
		assertFalse(fields.existsById(field.id()));
		assertTrue(properties.existsById(property.id()));
	}

	@Test
	void recursosDeOutroUsuarioRetornam404() throws Exception {
		var property = service.createProperty(owner, input("Fazenda"));
		var field = service.createField(owner, property.id(), new FieldInput("Talhão", BigDecimal.ONE));
		var planting = service.createPlanting(owner, field.id(),
				new PlantingInput("Tomate", Instant.now(), "SEEDLING"));
		Principal other = () -> outsider.toString();
		mvc.perform(get("/api/v1/properties/" + property.id()).principal(other))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
		assertEquals(404, assertThrows(ApiException.class,
				() -> service.getField(outsider, field.id())).getStatus());
		assertEquals(404, assertThrows(ApiException.class,
				() -> service.getPlanting(outsider, field.id(), planting.id())).getStatus());
		assertEquals(0, service.listProperties(outsider, 1, 20).items().size());
		assertThrows(ApiException.class, () -> service.deleteProperty(outsider, property.id()));
		assertThrows(ApiException.class, () -> service.updateField(outsider, field.id(),
				new FieldInput("Inválido", BigDecimal.ONE)));
		var anotherField = service.createField(owner, property.id(), new FieldInput("Outro", BigDecimal.ONE));
		assertThrows(ApiException.class, () -> service.getPlanting(owner, anotherField.id(), planting.id()));
	}

	@Test
	void paginaUmOrdenacaoLimitesEFormatoMobile() throws Exception {
		service.createProperty(owner, input("Zebra"));
		service.createProperty(owner, input("Abacate"));
		mvc.perform(get("/api/v1/properties").principal(principal).param("size", "1"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.items[0].name").value("Abacate"))
				.andExpect(jsonPath("$.page.number").value(1))
				.andExpect(jsonPath("$.page.size").value(1))
				.andExpect(jsonPath("$.page.totalItems").value(2))
				.andExpect(jsonPath("$.page.totalPages").value(2));
		assertEquals("Zebra", service.listProperties(owner, 2, 1).items().get(0).name());
		assertEquals(100, service.listProperties(owner, 0, 999).page().size());
		assertEquals(1, service.listProperties(owner, -1, -1).page().number());
		assertTrue(service.listProperties(owner, Integer.MAX_VALUE, 100).items().isEmpty());
		assertEquals(1, service.listProperties(outsider, 1, 20).page().totalPages());
	}

	@Test
	void patchPreservaAusenteELimpaDescricaoNull() throws Exception {
		var property = service.createProperty(owner, input("Fazenda"));
		mvc.perform(patch("/api/v1/properties/" + property.id()).principal(principal)
				.contentType("application/json").content("{\"name\":\"Nome novo\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.description").value("Descrição"))
				.andExpect(jsonPath("$.city").value("São Paulo"));
		mvc.perform(patch("/api/v1/properties/" + property.id()).principal(principal)
				.contentType("application/json").content("{\"description\":null}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.description").doesNotExist());
		assertNull(service.getProperty(owner, property.id()).description());
		Instant updatedAt = service.getProperty(owner, property.id()).updatedAt();
		mvc.perform(patch("/api/v1/properties/" + property.id()).principal(principal)
				.contentType("application/json").content("{}")).andExpect(status().isOk());
		assertEquals(updatedAt, service.getProperty(owner, property.id()).updatedAt());
	}

	@Test
	void validacaoDosDtosEJsonInvalido() throws Exception {
		mvc.perform(post("/api/v1/properties").principal(principal).contentType("application/json")
				.content("{\"name\":\"   \",\"city\":\"X\",\"state\":\"123\",\"areaHectares\":0}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.details.name").exists()).andExpect(jsonPath("$.details.city").exists())
				.andExpect(jsonPath("$.details.state").exists()).andExpect(jsonPath("$.details.areaHectares").exists())
				.andExpect(jsonPath("$.timestamp").exists());
		mvc.perform(post("/api/v1/properties").principal(principal)
				.contentType("application/json").content("{"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		mvc.perform(get("/api/v1/properties/nao-e-uuid").principal(principal))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/api/v1/properties").principal(principal).param("page", "abc"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void validacaoDeTalhaoPlantioEPatch() throws Exception {
		var property = service.createProperty(owner, input("Fazenda"));
		var field = service.createField(owner, property.id(), new FieldInput("Talhão", BigDecimal.ONE));
		mvc.perform(post("/api/v1/properties/" + property.id() + "/fields").principal(principal)
				.contentType("application/json").content("{\"name\":\"\",\"areaHectares\":-1}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.name").exists());
		mvc.perform(post("/api/v1/fields/" + field.id() + "/plantings").principal(principal)
				.contentType("application/json").content("{\"cropName\":\"Tomate\",\"stage\":\"MUDA\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.stage").exists())
				.andExpect(jsonPath("$.details.plantedAt").exists());
		mvc.perform(patch("/api/v1/properties/" + property.id()).principal(principal)
				.contentType("application/json").content("{\"name\":\"   \"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.name").exists());
		assertEquals("Fazenda", service.getProperty(owner, property.id()).name());
	}

	@Test
	void criacaoRetorna201LocationUUIDEDataUtc() throws Exception {
		mvc.perform(post("/api/v1/properties/").principal(principal).contentType("application/json")
				.content("{\"name\":\" Fazenda \",\"city\":\" São Paulo \",\"state\":\"sp\",\"areaHectares\":1.234567}"))
				.andExpect(status().isCreated()).andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.name").value("Fazenda")).andExpect(jsonPath("$.state").value("SP"))
				.andExpect(jsonPath("$.createdAt").value(org.hamcrest.Matchers.endsWith("Z")))
				.andExpect(jsonPath("$.user").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void listasRelacionadasEStatus204() throws Exception {
		var property = service.createProperty(owner, input("Fazenda"));
		var field = service.createField(owner, property.id(), new FieldInput("Talhão", BigDecimal.ONE));
		service.createPlanting(owner, field.id(), new PlantingInput("Tomate", Instant.now(), "SEEDLING"));
		mvc.perform(get("/api/v1/properties/" + property.id() + "/fields").principal(principal))
				.andExpect(status().isOk()).andExpect(jsonPath("$.items[0].propertyId").value(property.id().toString()));
		mvc.perform(get("/api/v1/fields/" + field.id() + "/plantings").principal(principal))
				.andExpect(status().isOk()).andExpect(jsonPath("$.items[0].stage").value("SEEDLING"));
		mvc.perform(delete("/api/v1/properties/" + property.id()).principal(principal))
				.andExpect(status().isNoContent()).andExpect(content().string(""));
	}

	@Test
	void autenticacaoAusenteNuncaCriaPropriedade() throws Exception {
		mvc.perform(get("/api/v1/properties")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		assertThrows(ApiException.class, () -> service.currentUser(() -> "inexistente@example.com"));
	}

	@Test
	void filtrosDeSegurancaRetornamJsonEmVezDeRedirecionar() throws Exception {
		MockMvc secured = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilterChain).build();
		secured.perform(get("/api/v1/properties"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(header().doesNotExist("Location"));
		secured.perform(post("/api/v1/properties").contentType("application/json").content("{}"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void preservaPrecisaoDecimalDoContratoCSharp() {
		BigDecimal area = new BigDecimal("1.1234567890123456789012345678");
		var property = service.createProperty(owner, new PropertyInput("Fazenda", "São Paulo", "SP", area, null));
		entityManager.flush();
		entityManager.clear();
		assertEquals(0, area.compareTo(service.getProperty(owner, property.id()).areaHectares()));
	}
}

