package br.com.fiap.agroconnect_api.control;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import br.com.fiap.agroconnect_api.record.PageResponse;
import br.com.fiap.agroconnect_api.record.PlantingDetailsResponse;
import br.com.fiap.agroconnect_api.record.PlantingInput;
import br.com.fiap.agroconnect_api.record.PlantingResponse;
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
@RequestMapping("/api/v1/fields/{fieldId}/plantings")
public class PlantingController {

	@Autowired
	private IdempotencyService idempotency;

	@Autowired
	private DomainService service;

	@GetMapping({"", "/"})
	public PageResponse<PlantingResponse> listar(Principal principal, @PathVariable UUID fieldId,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
		return service.listPlantings(service.currentUser(principal), fieldId, page, size);
	}

	@GetMapping("/{id}")
	public PlantingResponse buscarPorID(Principal principal, @PathVariable UUID fieldId, @PathVariable UUID id) {
		return service.getPlanting(service.currentUser(principal), fieldId, id);
	}

	@GetMapping("/{id}/details")
	public PlantingDetailsResponse detalhes(Principal principal, @PathVariable UUID fieldId, @PathVariable UUID id) {
		return service.getPlantingDetails(service.currentUser(principal), fieldId, id);
	}

	@PostMapping({"", "/"})
	@ApiResponse(responseCode = "201", description = "Registro criado.", useReturnTypeSchema = true)
	public ResponseEntity<PlantingResponse> cadastrar(Principal principal, HttpServletRequest request, @PathVariable UUID fieldId,
			@Valid @RequestBody PlantingInput input) {
		UUID userId = service.currentUser(principal);
		return idempotency.execute(userId, request, input, PlantingResponse.class, () -> {
			var result = service.createPlanting(userId, fieldId, input);
			return ResponseEntity.created(URI.create("/api/v1/fields/" + fieldId + "/plantings/" + result.id()))
					.body(result);
		});
	}

	@PutMapping("/{id}")
	public PlantingResponse atualizar(Principal principal, @PathVariable UUID fieldId, @PathVariable UUID id,
			@Valid @RequestBody PlantingInput input) {
		return service.updatePlanting(service.currentUser(principal), fieldId, id, input);
	}

	@DeleteMapping("/{id}")
	@ApiResponse(responseCode = "204", description = "Operação concluída sem corpo de resposta.", useReturnTypeSchema = true)
	public ResponseEntity<Void> remover(Principal principal, @PathVariable UUID fieldId, @PathVariable UUID id) {
		service.deletePlanting(service.currentUser(principal), fieldId, id);
		return ResponseEntity.noContent().build();
	}
}
