package br.com.fiap.agroconnect_api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI agroConnectOpenApi() {
		Components components = new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
				.type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
				.description("Faça login em /api/v1/auth/login e informe somente o accessToken."));
		components.addSchemas("ApiError", errorSchema());
		return new OpenAPI().info(new Info().title("AgroConnect API").version("v1")
				.description("API compatível com o contrato mobile AgroConnect. Paginação inicia em 1. "
						+ "Os dados pertencem ao usuário autenticado; ADMIN não ignora essa separação. "
						+ "As rotas /admin/users exigem ADMIN. Datas são transmitidas em UTC."))
				.components(components);
	}

	@Bean
	public GroupedOpenApi mobileApi() {
		return GroupedOpenApi.builder().group("agroconnect-v1").pathsToMatch("/api/v1/**").build();
	}

	@Bean
	public GlobalOpenApiCustomizer mobileContractDocumentation() {
		return this::customize;
	}

	private void customize(OpenAPI api) {
		List<String> aliases = api.getPaths().keySet().stream()
				.filter(path -> path.endsWith("/") && api.getPaths().containsKey(path.substring(0, path.length() - 1)))
				.toList();
		aliases.forEach(api.getPaths()::remove);
		api.getComponents().addSchemas("ApiError", errorSchema());
		api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
			boolean publicRoute = method.name().equals("POST")
					&& (path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/register"));
			operation.setSecurity(publicRoute ? new ArrayList<>() : List.of(new SecurityRequirement().addList("bearerAuth")));
			String resource = path.substring("/api/v1/".length()).split("/")[0];
			operation.setTags(List.of(resource));
			if (operation.getSummary() == null) operation.setSummary(method.name() + " " + path);
			if (path.startsWith("/api/v1/admin/")) operation.setDescription("Exige papel ADMIN. "
					+ "Alterar papel ou desativar um usuário revoga suas sessões.");
			for (var error : Map.of("400", "Entrada inválida", "401", "Autenticação ausente ou inválida",
					"403", "Acesso não permitido", "404", "Registro não encontrado", "409", "Conflito de dados").entrySet()) {
				operation.getResponses().addApiResponse(error.getKey(), errorResponse(error.getValue()));
			}
			if (path.equals("/api/v1/recommendations") && method.name().equals("POST")) {
				operation.setSummary("Gerar recomendações agrícolas com Spring AI");
				operation.setDescription("Analisa alertas não resolvidos com evidências. Pode reutilizar um resultado "
						+ "por 30 minutos se o contexto não mudou. Retorna items vazio quando não há alertas elegíveis.");
				operation.getResponses().addApiResponse("422", errorResponse("Contexto muito grande"));
				operation.getResponses().addApiResponse("429", errorResponse("Serviço ocupado"));
				operation.getResponses().addApiResponse("502", errorResponse("Resposta da IA inválida"));
				operation.getResponses().addApiResponse("503", errorResponse("Serviço de IA indisponível"));
			}
		}));
	}

	private Schema<?> errorSchema() {
		return new Schema<>().type("object")
				.addProperty("code", new Schema<>().type("string"))
				.addProperty("message", new Schema<>().type("string"))
				.addProperty("details", new Schema<>())
				.addProperty("timestamp", new Schema<>().type("string").format("date-time"));
	}

	private ApiResponse errorResponse(String description) {
		return new ApiResponse().description(description).content(new Content().addMediaType("application/json",
				new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiError"))));
	}
}
