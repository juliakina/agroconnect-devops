package br.com.fiap.agroconnect_ia.service;

import br.com.fiap.agroconnect_ia.record.AiAlertContext;
import br.com.fiap.agroconnect_ia.record.AiDeviceContext;
import br.com.fiap.agroconnect_ia.record.AiRecommendationBatch;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class RecommendationAiService {

	@Autowired
	private ObjectProvider<ChatModel> models;
	@Autowired
	private Validator validator;
	@Autowired
	private Clock clock;
	@Value("classpath:prompts/recommendations-system.txt")
	private Resource instructions;
	@Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
	private String modelName;
	private String systemPrompt;
	private final JsonMapper mapper = JsonMapper.builder().build();
	private final BeanOutputConverter<AiRecommendationBatch> converter = new BeanOutputConverter<>(AiRecommendationBatch.class);
	
	@Value("${spring.ai.openai.api-key:}")
	private String apiKey;

	@PostConstruct
	public void initialize() throws IOException {
		systemPrompt = instructions.getContentAsString(StandardCharsets.UTF_8);
		LoggerFactory.getLogger(RecommendationAiService.class).info("OpenAI key: tamanho={}, final={}",
			    apiKey.length(), apiKey.isEmpty() ? "-" : apiKey.substring(Math.max(0, apiKey.length() - 4)));
	}

	public String modelName() {
		return modelName;
	}

	public String fingerprint(AiDeviceContext context, AiAlertContext alert) {
		AiDeviceContext single = new AiDeviceContext(context.deviceCode(), context.deviceName(), context.deviceStatus(),
				context.lastSeenAt(), context.fieldName(), context.plantings(), List.of(alert));
		String input = modelName + "\n" + systemPrompt + "\n" + mapper.writeValueAsString(single);
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}

	public String contextJson(AiDeviceContext context) {
		return mapper.writeValueAsString(Map.of("referenceTime", clock.instant(), "device", context));
	}

	public AiRecommendationBatch generate(AiDeviceContext context) {
		ChatModel model = models.getIfAvailable();
		if (model == null) throw new ApiException(503, "AI_NOT_CONFIGURED", "Configure o provedor de IA e sua chave de acesso.");
		String json = contextJson(context);
		if (json.length() > 60000) throw new ApiException(422, "AI_CONTEXT_TOO_LARGE", "Reduza os alertas ou as evidências desta solicitação.");
		String content;
		try {
			ChatClient agente = ChatClient.builder(model).build();
			content = agente.prompt().system(systemPrompt)
					.user(user -> user.text("CONTEXTO_JSON_INICIO\n{context}\nCONTEXTO_JSON_FIM\n{format}")
							.param("context", json).param("format", converter.getFormat()))
					.call().content();
		} catch (RuntimeException exception) {
			LoggerFactory.getLogger(RecommendationAiService.class)
				.warn("O provedor de IA falhou: {} - {}", exception.getClass().getSimpleName(), exception.getMessage());
			throw new ApiException(503, "AI_UNAVAILABLE", "Não foi possível consultar o modelo de IA. Tente novamente mais tarde.");
		}
		if (content == null || content.isBlank() || content.length() > 40000) throw invalidOutput();
		AiRecommendationBatch result;
		try {
			result = converter.convert(content);
		} catch (RuntimeException exception) {
			throw invalidOutput();
		}
		if (result == null || !validator.validate(result).isEmpty()) throw invalidOutput();
		var expected = context.alerts().stream().map(AiAlertContext::alertId).collect(Collectors.toSet());
		var actual = result.recommendations().stream().map(item -> item.alertId()).collect(Collectors.toSet());
		if (result.recommendations().size() != expected.size() || actual.size() != expected.size() || !expected.equals(actual))
			throw invalidOutput();
		return result;
	}

	private ApiException invalidOutput() {
		return new ApiException(502, "AI_INVALID_RESPONSE", "O modelo retornou uma resposta fora do formato esperado.");
	}
}
