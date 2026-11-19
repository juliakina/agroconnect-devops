package br.com.fiap.agroconnect_api.control;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import br.com.fiap.agroconnect_api.record.FieldInput;
import br.com.fiap.agroconnect_api.record.FieldResponse;
import br.com.fiap.agroconnect_api.record.PageResponse;
import br.com.fiap.agroconnect_api.service.DomainService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import br.com.fiap.agroconnect_api.service.IdempotencyService;
import java.net.URI;
import java.security.Principal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class FieldController {

	@Autowired
	private IdempotencyService idempotency;

	@Autowired
	private DomainService service;

	@GetMapping({"/properties/{propertyId}/fields", "/properties/{propertyId}/fields/"})
	public PageResponse<FieldResponse> listar(Principal principal, @PathVariable UUID propertyId,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
		return service.listFields(service.currentUser(principal), propertyId, page, size);
	}

	@GetMapping("/fields/{id}")
	public FieldResponse buscarPorID(Principal principal, @PathVariable UUID id) {
		return service.getField(service.currentUser(principal), id);
	}

	@PostMapping({"/properties/{propertyId}/fields", "/properties/{propertyId}/fields/"})
	@ApiResponse(responseCode = "201", description = "Registro criado.", useReturnTypeSchema = true)
	public ResponseEntity<FieldResponse> cadastrar(Principal principal, HttpServletRequest request, @PathVariable UUID propertyId,
			@Valid @RequestBody FieldInput input) {
		UUID userId = service.currentUser(principal);
		return idempotency.execute(userId, request, input, FieldResponse.class, () -> {
			var result = service.createField(userId, propertyId, input);
			return ResponseEntity.created(URI.create("/api/v1/fields/" + result.id())).body(result);
		});
	}

	@PutMapping("/fields/{id}")
	public FieldResponse atualizar(Principal principal, @PathVariable UUID id,
			@Valid @RequestBody FieldInput input) {
		return service.updateField(service.currentUser(principal), id, input);
	}

	@DeleteMapping("/fields/{id}")
	@ApiResponse(responseCode = "204", description = "Operação concluída sem corpo de resposta.", useReturnTypeSchema = true)
	public ResponseEntity<Void> remover(Principal principal, @PathVariable UUID id) {
		service.deleteField(service.currentUser(principal), id);
		return ResponseEntity.noContent().build();
	}
}
