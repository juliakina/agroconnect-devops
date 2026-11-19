package br.com.fiap.agroconnect_api.security;

import br.com.fiap.agroconnect_api.record.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ApiErrorResponder {

	private final JsonMapper mapper = JsonMapper.builder().build();

	public void write(HttpServletResponse response, int status, String code, String message) throws IOException {
		response.setStatus(status);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		mapper.writeValue(response.getWriter(), new ApiError(code, message, null, Instant.now()));
	}
}
