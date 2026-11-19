package br.com.fiap.agroconnect_api.control;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import br.com.fiap.agroconnect_api.record.PageResponse;
import br.com.fiap.agroconnect_api.record.SensorInput;
import br.com.fiap.agroconnect_api.record.SensorResponse;
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
public class SensorController {

	@Autowired
	private MonitoringService service;
	@Autowired
	private DomainService identity;

	@GetMapping("/devices/{deviceId}/sensors")
	public PageResponse<SensorResponse> listar(Principal principal, @PathVariable UUID deviceId, 
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
		return service.listSensors(identity.currentUser(principal), deviceId, page, size);
	}

	@GetMapping("/sensors/{id}")
	public SensorResponse buscarPorID(Principal principal, @PathVariable UUID id) {
		return service.getSensor(identity.currentUser(principal), id);
	}

	@PostMapping("/sensors")
	@ApiResponse(responseCode = "201", description = "Registro criado.", useReturnTypeSchema = true)
	public ResponseEntity<SensorResponse> cadastrar(Principal principal, @Valid @RequestBody SensorInput input) {
		var result = service.createSensor(identity.currentUser(principal), input);
		return ResponseEntity.created(URI.create("/api/v1/sensors/" + result.id())).body(result);
	}

	@PutMapping("/sensors/{id}")
	public SensorResponse atualizar(Principal principal, @PathVariable UUID id, @Valid @RequestBody SensorInput input) {
		return service.updateSensor(identity.currentUser(principal), id, input);
	}

	@DeleteMapping("/sensors/{id}")
	@ApiResponse(responseCode = "204", description = "Operação concluída sem corpo de resposta.", useReturnTypeSchema = true)
	public ResponseEntity<Void> remover(Principal principal, @PathVariable UUID id) {
		service.deleteSensor(identity.currentUser(principal), id);
		return ResponseEntity.noContent().build();
	}
}
