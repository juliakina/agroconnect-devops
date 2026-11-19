package br.com.fiap.agroconnect_api.control;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import br.com.fiap.agroconnect_api.record.DeviceInput;
import br.com.fiap.agroconnect_api.record.DeviceResponse;
import br.com.fiap.agroconnect_api.record.HistoryResponse;
import br.com.fiap.agroconnect_api.record.LatestReadingsResponse;
import br.com.fiap.agroconnect_api.record.PageResponse;
import br.com.fiap.agroconnect_api.service.DomainService;
import br.com.fiap.agroconnect_api.service.MonitoringService;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/devices")
public class DeviceController {

	@Autowired
	private MonitoringService service;
	@Autowired
	private DomainService identity;

	@GetMapping({"", "/"})
	public PageResponse<DeviceResponse> listar(Principal principal, @RequestParam(required = false) UUID fieldId, 
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
		return service.listDevices(identity.currentUser(principal), fieldId, page, size);
	}

	@GetMapping("/{id}")
	public DeviceResponse buscarPorID(Principal principal, @PathVariable UUID id) {
		return service.getDevice(identity.currentUser(principal), id);
	}

	@PostMapping({"", "/"})
	@ApiResponse(responseCode = "201", description = "Registro criado.", useReturnTypeSchema = true)
	public ResponseEntity<DeviceResponse> cadastrar(Principal principal, @Valid @RequestBody DeviceInput input) {
		var result = service.createDevice(identity.currentUser(principal), input);
		return ResponseEntity.created(URI.create("/api/v1/devices/" + result.id())).body(result);
	}

	@PutMapping("/{id}")
	public DeviceResponse atualizar(Principal principal, @PathVariable UUID id, @Valid @RequestBody DeviceInput input) {
		return service.updateDevice(identity.currentUser(principal), id, input);
	}

	@DeleteMapping("/{id}")
	@ApiResponse(responseCode = "204", description = "Operação concluída sem corpo de resposta.", useReturnTypeSchema = true)
	public ResponseEntity<Void> remover(Principal principal, @PathVariable UUID id) {
		service.deleteDevice(identity.currentUser(principal), id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/readings/latest")
	public LatestReadingsResponse ultimasLeituras(Principal principal, @PathVariable UUID id) {
		return service.latest(identity.currentUser(principal), id);
	}

	@GetMapping("/{id}/readings")
	public HistoryResponse historico(Principal principal, @PathVariable UUID id,
			@RequestParam(required = false) UUID sensorId, @RequestParam(required = false) String type,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@RequestParam(required = false) String interval) {
		return service.history(identity.currentUser(principal), id, sensorId, type, from, to, interval);
	}
}
