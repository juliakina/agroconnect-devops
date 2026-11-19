package br.com.fiap.agroconnect_api.control;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import br.com.fiap.agroconnect_api.record.PageResponse;
import br.com.fiap.agroconnect_api.record.ReadingDetailsResponse;
import br.com.fiap.agroconnect_api.record.ReadingInput;
import br.com.fiap.agroconnect_api.service.DomainService;
import br.com.fiap.agroconnect_api.service.MonitoringService;
import jakarta.validation.Valid;
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
public class ReadingController {

	@Autowired
	private MonitoringService service;
	@Autowired
	private DomainService identity;

	@GetMapping("/sensors/{sensorId}/readings")
	public PageResponse<ReadingDetailsResponse> listar(Principal principal, @PathVariable UUID sensorId, 
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
		return service.listReadings(identity.currentUser(principal), sensorId, page, size);
	}

	@GetMapping("/readings/{id}")
	public ReadingDetailsResponse buscarPorID(Principal principal, @PathVariable UUID id) {
		return service.getReading(identity.currentUser(principal), id);
	}

	@PostMapping("/readings")
	@ApiResponse(responseCode = "201", description = "Registro criado.", useReturnTypeSchema = true)
	public ResponseEntity<ReadingDetailsResponse> cadastrar(Principal principal, @Valid @RequestBody ReadingInput input) {
		var result = service.createReading(identity.currentUser(principal), input);
		return ResponseEntity.created(URI.create("/api/v1/readings/" + result.id())).body(result);
	}

	@PutMapping("/readings/{id}")
	public ReadingDetailsResponse atualizar(Principal principal, @PathVariable UUID id, @Valid @RequestBody ReadingInput input) {
		return service.updateReading(identity.currentUser(principal), id, input);
	}

	@DeleteMapping("/readings/{id}")
	@ApiResponse(responseCode = "204", description = "Operação concluída sem corpo de resposta.", useReturnTypeSchema = true)
	public ResponseEntity<Void> remover(Principal principal, @PathVariable UUID id) {
		service.deleteReading(identity.currentUser(principal), id);
		return ResponseEntity.noContent().build();
	}
}
