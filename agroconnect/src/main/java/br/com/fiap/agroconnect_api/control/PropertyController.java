package br.com.fiap.agroconnect_api.control;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import br.com.fiap.agroconnect_api.record.PageResponse;
import br.com.fiap.agroconnect_api.record.PropertyInput;
import br.com.fiap.agroconnect_api.record.PropertyPatch;
import br.com.fiap.agroconnect_api.record.PropertyResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/properties")
public class PropertyController {

	@Autowired
	private IdempotencyService idempotency;

	@Autowired
	private DomainService service;

	@GetMapping({"", "/"})
	public PageResponse<PropertyResponse> listar(Principal principal,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
		return service.listProperties(service.currentUser(principal), page, size);
	}

	@GetMapping("/{id}")
	public PropertyResponse buscarPorID(Principal principal, @PathVariable UUID id) {
		return service.getProperty(service.currentUser(principal), id);
	}

	@PostMapping({"", "/"})
	@ApiResponse(responseCode = "201", description = "Registro criado.", useReturnTypeSchema = true)
	public ResponseEntity<PropertyResponse> cadastrar(Principal principal, HttpServletRequest request, @Valid @RequestBody PropertyInput input) {
		UUID userId = service.currentUser(principal);
		return idempotency.execute(userId, request, input, PropertyResponse.class, () -> {
			var result = service.createProperty(userId, input);
			return ResponseEntity.created(URI.create("/api/v1/properties/" + result.id())).body(result);
		});
	}

	@PatchMapping("/{id}")
	public PropertyResponse atualizar(Principal principal, @PathVariable UUID id,
			@Valid @RequestBody PropertyPatch input) {
		return service.patchProperty(service.currentUser(principal), id, input);
	}

	@DeleteMapping("/{id}")
	@ApiResponse(responseCode = "204", description = "Operação concluída sem corpo de resposta.", useReturnTypeSchema = true)
	public ResponseEntity<Void> remover(Principal principal, @PathVariable UUID id) {
		service.deleteProperty(service.currentUser(principal), id);
		return ResponseEntity.noContent().build();
	}
}
