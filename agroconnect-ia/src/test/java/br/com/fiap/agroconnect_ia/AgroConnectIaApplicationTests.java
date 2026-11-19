package br.com.fiap.agroconnect_ia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.ai.model.chat=none", "eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false", "app.ai.internal-key=test-internal-key-12345678901234567890"})
@org.springframework.test.context.TestExecutionListeners(listeners = org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class, inheritListeners = false)
class AgroConnectIaApplicationTests {

	@Test
	void contextLoads() {
	}

}
