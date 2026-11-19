package br.com.fiap.agroconnect_api.control;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import br.com.fiap.agroconnect_api.record.AiInteractionInput;
import br.com.fiap.agroconnect_api.record.AiInteractionResponse;
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
@RequestMapping("/api/v1/ai-interactions")
public class AiInteractionController {

	@Autowired
	private AdditionalDomainService service;
	@Autowired
	private DomainService identity;

	@GetMapping({"", "/"})
	public PageResponse<AiInteractionResponse> listar(Principal principal, 
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
		return service.listAiInteraction(identity.currentUser(principal), page, size);
	}

	@GetMapping("/{id}")
	public AiInteractionResponse buscarPorID(Principal principal, @PathVariable UUID id) {
		return service.getAiInteraction(identity.currentUser(principal), id);
	}

	@PostMapping({"", "/"})
	@ApiResponse(responseCode = "201", description = "Registro criado.", useReturnTypeSchema = true)
	public ResponseEntity<AiInteractionResponse> cadastrar(Principal principal, @Valid @RequestBody AiInteractionInput input) {
		var result = service.createAiInteraction(identity.currentUser(principal), input);
		return ResponseEntity.created(URI.create("/api/v1/ai-interactions/" + result.id())).body(result);
	}

	@PutMapping("/{id}")
	public AiInteractionResponse atualizar(Principal principal, @PathVariable UUID id, @Valid @RequestBody AiInteractionInput input) {
		return service.updateAiInteraction(identity.currentUser(principal), id, input);
	}

	@DeleteMapping("/{id}")
	@ApiResponse(responseCode = "204", description = "Operação concluída sem corpo de resposta.", useReturnTypeSchema = true)
	public ResponseEntity<Void> remover(Principal principal, @PathVariable UUID id) {
		service.deleteAiInteraction(identity.currentUser(principal), id);
		return ResponseEntity.noContent().build();
	}
}
