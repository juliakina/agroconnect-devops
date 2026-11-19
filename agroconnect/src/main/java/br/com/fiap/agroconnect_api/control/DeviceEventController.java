package br.com.fiap.agroconnect_api.control;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import br.com.fiap.agroconnect_api.record.DeviceEventInput;
import br.com.fiap.agroconnect_api.record.DeviceEventResponse;
import br.com.fiap.agroconnect_api.record.PageResponse;
import br.com.fiap.agroconnect_api.service.AdditionalDomainService;
import br.com.fiap.agroconnect_api.service.DomainService;
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
@RequestMapping("/api/v1/device-events")
public class DeviceEventController {

	@Autowired
	private AdditionalDomainService service;
	@Autowired
	private DomainService identity;

	@GetMapping({"", "/"})
	public PageResponse<DeviceEventResponse> listar(Principal principal, @RequestParam UUID deviceId, 
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
		return service.listDeviceEvent(identity.currentUser(principal), deviceId, page, size);
	}

	@GetMapping("/{id}")
	public DeviceEventResponse buscarPorID(Principal principal, @PathVariable UUID id) {
		return service.getDeviceEvent(identity.currentUser(principal), id);
	}

	@PostMapping({"", "/"})
	@ApiResponse(responseCode = "201", description = "Registro criado.", useReturnTypeSchema = true)
	public ResponseEntity<DeviceEventResponse> cadastrar(Principal principal, @Valid @RequestBody DeviceEventInput input) {
		var result = service.createDeviceEvent(identity.currentUser(principal), input);
		return ResponseEntity.created(URI.create("/api/v1/device-events/" + result.id())).body(result);
	}

	@PutMapping("/{id}")
	public DeviceEventResponse atualizar(Principal principal, @PathVariable UUID id, @Valid @RequestBody DeviceEventInput input) {
		return service.updateDeviceEvent(identity.currentUser(principal), id, input);
	}

	@DeleteMapping("/{id}")
	@ApiResponse(responseCode = "204", description = "Operação concluída sem corpo de resposta.", useReturnTypeSchema = true)
	public ResponseEntity<Void> remover(Principal principal, @PathVariable UUID id) {
		service.deleteDeviceEvent(identity.currentUser(principal), id);
		return ResponseEntity.noContent().build();
	}
}
