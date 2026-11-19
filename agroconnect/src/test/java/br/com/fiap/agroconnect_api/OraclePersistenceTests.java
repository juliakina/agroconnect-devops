package br.com.fiap.agroconnect_api;

import br.com.fiap.agroconnect_api.model.AiInteraction;
import br.com.fiap.agroconnect_api.model.Property;
import br.com.fiap.agroconnect_api.model.User;
import br.com.fiap.agroconnect_api.record.PropertyInput;
import br.com.fiap.agroconnect_api.record.RegisterRequest;
import br.com.fiap.agroconnect_api.record.RecommendationInput;
import br.com.fiap.agroconnect_api.repository.DeviceRepository;
import br.com.fiap.agroconnect_api.service.AuthService;
import br.com.fiap.agroconnect_api.service.DemoDataService;
import br.com.fiap.agroconnect_api.service.RecommendationService;
import br.com.fiap.testconfig.AiTestConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:oracle-structure;MODE=Oracle;NON_KEYWORDS=VALUE,KEY;DB_CLOSE_DELAY=-1",
		"spring.flyway.enabled=false", "spring.sql.init.mode=always",
		"spring.jpa.hibernate.ddl-auto=none",
		"spring.sql.init.schema-locations=classpath:db/oracle/agroconnect_java_oracle.sql",
		"spring.jpa.hibernate.naming.physical-strategy=br.com.fiap.agroconnect_api.config.OracleSchemaNamingStrategy",
		"spring.jpa.mapping-resources=META-INF/orm-oracle.xml",
		"spring.jpa.properties.hibernate.type.preferred_uuid_jdbc_type=BINARY",
		"spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type=TIMESTAMP_WITH_TIMEZONE"
})
@ActiveProfiles("test")
@Import(AiTestConfiguration.class)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
@Transactional
class OraclePersistenceTests {

	@Autowired
	private EntityManager entityManager;
	@Autowired
	private Validator validator;
	@Autowired
	private AuthService auth;
	@Autowired
	private DemoDataService demo;
	@Autowired
	private DeviceRepository devices;
	@Autowired
	private RecommendationService recommendations;

	@Test
	void preservesBinaryUuidNumericBooleanNanosecondsAndLongText() {
		User user = new User();
		user.setName("Produtor Oracle");
		user.setEmail(UUID.randomUUID() + "@example.com");
		user.setPasswordHash("fixture");
		Instant timestamp = Instant.parse("2026-11-16T00:00:00.123456789Z");
		user.setCreatedAt(timestamp);
		entityManager.persist(user);
		Property property = new Property();
		property.setUser(user);
		property.setName("Propriedade");
		property.setCity("Ibiúna");
		property.setState("SP");
		property.setAreaHectares(new BigDecimal("0.1234567890123456789012345678"));
		property.setDescription("漢".repeat(2000));
		entityManager.persist(property);
		AiInteraction interaction = new AiInteraction();
		interaction.setUser(user);
		interaction.setQuestion("Pergunta");
		interaction.setContext("á漢".repeat(15000));
		entityManager.persist(interaction);
		entityManager.flush();
		UUID userId = user.getId();
		UUID propertyId = property.getId();
		UUID interactionId = interaction.getId();
		entityManager.clear();
		assertEquals(timestamp, entityManager.find(User.class, userId).getCreatedAt());
		assertTrue(entityManager.find(User.class, userId).getActive());
		assertEquals(0, property.getAreaHectares().compareTo(entityManager.find(Property.class, propertyId).getAreaHectares()));
		assertEquals("漢".repeat(2000), entityManager.find(Property.class, propertyId).getDescription());
		assertEquals(30000, entityManager.find(AiInteraction.class, interactionId).getContext().length());
		assertEquals(1, ((Number) entityManager.createNativeQuery("select Active from Users where Id = :id", Object.class)
				.setParameter("id", userId).getSingleResult()).intValue());
	}

	@Test
	void rejectsDecimalValuesOracleWouldRound() {
		PropertyInput input = new PropertyInput("Propriedade", "Ibiúna", "SP",
				new BigDecimal("12345678901234567890123456789.1234567891"), null);
		assertTrue(validator.validate(input).stream().anyMatch(error -> error.getMessage().contains("38")));
	}

	@Test
	void generatesRecommendationsUsingOracleStorageMappings() {
		var session = auth.register(new RegisterRequest("Produtor Oracle", UUID.randomUUID() + "@example.com", "Senha-Teste-2026!"));
		demo.seed(session.user().id());
		UUID device = devices.findByCode("ESP32-001").orElseThrow().getId();
		assertEquals(1, recommendations.generate(session.user().id(), new RecommendationInput(device)).items().size());
	}
}
