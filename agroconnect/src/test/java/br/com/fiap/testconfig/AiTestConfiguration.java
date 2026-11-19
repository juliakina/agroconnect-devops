package br.com.fiap.testconfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

@TestConfiguration
public class AiTestConfiguration {

	@Bean
	public FakeChatModel fakeChatModel() {
		return new FakeChatModel();
	}

	public static class FakeChatModel implements ChatModel {

		public enum Mode {
			VALID, INVALID_JSON, WRONG_ID, DUPLICATE_ID, EMPTY_LIST, LONG_TITLE, BLANK_REASON, EMPTY_CONTENT, UNAVAILABLE
		}

		private final JsonMapper mapper = JsonMapper.builder().build();
		private final AtomicInteger calls = new AtomicInteger();
		private Mode mode = Mode.VALID;
		private Runnable beforeResponse;
		private String lastSystem;
		private String lastUser;

		public void reset() {
			calls.set(0);
			mode = Mode.VALID;
			beforeResponse = null;
			lastSystem = null;
			lastUser = null;
		}

		public void setMode(Mode mode) {
			this.mode = mode;
		}

		public void setBeforeResponse(Runnable callback) {
			beforeResponse = callback;
		}

		public int calls() {
			return calls.get();
		}

		public String lastSystem() {
			return lastSystem;
		}

		public String lastUser() {
			return lastUser;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			calls.incrementAndGet();
			lastSystem = prompt.getSystemMessage().getText();
			lastUser = prompt.getUserMessage().getText();
			if (beforeResponse != null) beforeResponse.run();
			if (mode == Mode.UNAVAILABLE) throw new IllegalStateException("Provedor de teste indisponível.");
			String content;
			if (mode == Mode.INVALID_JSON) content = "Resposta sem JSON";
			else if (mode == Mode.EMPTY_CONTENT) content = "";
			else {
				int start = lastUser.indexOf("CONTEXTO_JSON_INICIO\n") + "CONTEXTO_JSON_INICIO\n".length();
				int end = lastUser.indexOf("\nCONTEXTO_JSON_FIM");
				Map<?, ?> payload = mapper.readValue(lastUser.substring(start, end), Map.class);
				Map<?, ?> device = (Map<?, ?>) payload.get("device");
				List<?> alerts = (List<?>) device.get("alerts");
				List<Map<String, Object>> recommendations = new ArrayList<>();
				for (Object row : alerts) {
					Map<?, ?> alert = (Map<?, ?>) row;
					recommendations.add(Map.of(
							"alertId", mode == Mode.WRONG_ID ? UUID.randomUUID().toString() : alert.get("alertId"),
							"title", mode == Mode.LONG_TITLE ? "A".repeat(121) : "Confira a umidade do solo",
							"message", "Verifique o solo e o posicionamento do sensor antes de decidir.",
							"reason", mode == Mode.BLANK_REASON ? " " : "As leituras válidas indicam umidade abaixo da referência do cenário."));
				}
				if (mode == Mode.DUPLICATE_ID) recommendations.add(recommendations.get(0));
				if (mode == Mode.EMPTY_LIST) recommendations.clear();
				content = mapper.writeValueAsString(Map.of("recommendations", recommendations));
			}
			return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
		}
	}
}
