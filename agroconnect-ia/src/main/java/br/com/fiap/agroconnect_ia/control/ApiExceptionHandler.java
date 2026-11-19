package br.com.fiap.agroconnect_ia.control;

import br.com.fiap.agroconnect_ia.service.ApiException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<?> handle(ApiException exception) {
		return ResponseEntity.status(exception.getStatus()).body(Map.of("code", exception.getCode(), "message", exception.getMessage()));
	}
}
