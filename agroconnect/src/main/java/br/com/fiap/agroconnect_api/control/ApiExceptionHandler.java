package br.com.fiap.agroconnect_api.control;

import br.com.fiap.agroconnect_api.record.ApiError;
import br.com.fiap.agroconnect_api.service.ApiException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> tratarAcessoNegado(AccessDeniedException exception) {
		return error(403, "FORBIDDEN", "Acesso não permitido.", null);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError> tratarAutenticacao(AuthenticationException exception) {
		return error(401, "UNAUTHORIZED", "Autenticação necessária.", null);
	}

	private ResponseEntity<ApiError> error(int status, String code, String message, Object details) {
		return ResponseEntity.status(status).body(new ApiError(code, message, details, Instant.now()));
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiError> tratarApi(ApiException exception) {
		return error(exception.getStatus(), exception.getCode(), exception.getMessage(), exception.getDetails());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> tratarValidacao(MethodArgumentNotValidException exception) {
		Map<String, String> details = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors().forEach(
				item -> details.putIfAbsent(item.getField(), item.getDefaultMessage()));
		return error(400, "VALIDATION_ERROR", "Os dados enviados são inválidos.", details);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiError> tratarRestricoes(ConstraintViolationException exception) {
		Map<String, String> details = new LinkedHashMap<>();
		exception.getConstraintViolations().forEach(
				item -> details.putIfAbsent(item.getPropertyPath().toString(), item.getMessage()));
		return error(400, "VALIDATION_ERROR", "Os dados enviados são inválidos.", details);
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class})
	public ResponseEntity<ApiError> tratarEntrada(Exception exception) {
		return error(400, "VALIDATION_ERROR", "JSON, identificador ou parâmetro inválido.", null);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> tratarConflito(DataIntegrityViolationException exception) {
		return error(409, "CONFLICT", "Os dados conflitam com um registro existente.", null);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiError> tratarMetodo(HttpRequestMethodNotSupportedException exception) {
		return error(405, "METHOD_NOT_ALLOWED", "Método HTTP não permitido.", null);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiError> tratarTipo(HttpMediaTypeNotSupportedException exception) {
		return error(415, "UNSUPPORTED_MEDIA_TYPE", "Envie o corpo como application/json.", null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> tratarErro(Exception exception) {
		LoggerFactory.getLogger(ApiExceptionHandler.class).error("Falha ao processar a API", exception);
		return error(500, "INTERNAL_ERROR", "Não foi possível processar a solicitação.", null);
	}
}
