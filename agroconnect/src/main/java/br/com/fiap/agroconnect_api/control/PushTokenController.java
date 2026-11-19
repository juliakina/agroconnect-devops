package br.com.fiap.agroconnect_api.control;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import br.com.fiap.agroconnect_api.record.PushTokenInput;
import br.com.fiap.agroconnect_api.service.DomainService;
import br.com.fiap.agroconnect_api.service.PushTokenService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PushTokenController {

	@Autowired
	private PushTokenService service;
	@Autowired
	private DomainService identity;

	@PostMapping("/api/v1/me/push-tokens")
	@ApiResponse(responseCode = "204", description = "Operação concluída sem corpo de resposta.", useReturnTypeSchema = true)
	public ResponseEntity<Void> cadastrar(Principal principal,
			@RequestAttribute(name = "sessionId", required = false) UUID sessionId,
			@Valid @RequestBody PushTokenInput input) {
		service.register(identity.currentUser(principal), sessionId, input);
		return ResponseEntity.noContent().build();
	}
}
