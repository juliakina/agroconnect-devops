package br.com.fiap.agroconnect_api.control;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import br.com.fiap.agroconnect_api.record.AlertInput;
import br.com.fiap.agroconnect_api.record.AlertResponse;
import br.com.fiap.agroconnect_api.record.AlertUpdateInput;
import br.com.fiap.agroconnect_api.record.PageResponse;
import br.com.fiap.agroconnect_api.service.AlertService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

	@Autowired
	private IdempotencyService idempotency;

	@Autowired
	private AlertService service;
	@Autowired
	private DomainService identity;

	@GetMapping({"", "/"})
	public PageResponse<AlertResponse> listar(Principal principal, @RequestParam(required = false) String status,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
		return service.listAlerts(identity.currentUser(principal), status, page, size);
	}

	@GetMapping("/{id}")
	public AlertResponse buscarPorID(Principal principal, @PathVariable UUID id) {
		return service.getAlert(identity.currentUser(principal), id);
	}

	@PostMapping({"", "/"})
	@ApiResponse(responseCode = "201", description = "Registro criado.", useReturnTypeSchema = true)
	public ResponseEntity<AlertResponse> cadastrar(Principal principal, HttpServletRequest request, @Valid @RequestBody AlertInput input) {
		UUID userId = identity.currentUser(principal);
		return idempotency.execute(userId, request, input, AlertResponse.class, () -> {
			var item = service.createAlert(userId, input);
			return ResponseEntity.created(URI.create("/api/v1/alerts/" + item.id())).body(item);
		});
	}

	@PutMapping("/{id}")
	public AlertResponse atualizar(Principal principal, @PathVariable UUID id, @Valid @RequestBody AlertUpdateInput input) {
		return service.updateAlert(identity.currentUser(principal), id, input);
	}

	@PatchMapping("/{id}/read")
	public AlertResponse marcarLido(Principal principal, @PathVariable UUID id) {
		return service.markRead(identity.currentUser(principal), id);
	}

	@DeleteMapping("/{id}")
	@ApiResponse(responseCode = "204", description = "Operação concluída sem corpo de resposta.", useReturnTypeSchema = true)
	public ResponseEntity<Void> remover(Principal principal, @PathVariable UUID id) {
		service.deleteAlert(identity.currentUser(principal), id);
		return ResponseEntity.noContent().build();
	}
}
