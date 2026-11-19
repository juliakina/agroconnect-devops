package br.com.fiap.agroconnect_api.control;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import br.com.fiap.agroconnect_api.record.PageResponse;
import br.com.fiap.agroconnect_api.record.RecommendationInput;
import br.com.fiap.agroconnect_api.record.RecommendationResponse;
import br.com.fiap.agroconnect_api.record.RecommendationWriteInput;
import br.com.fiap.agroconnect_api.service.AlertService;
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
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

	@Autowired
	private AlertService service;
	@Autowired
	private DomainService identity;

	@PostMapping({"", "/"})
	public PageResponse<RecommendationResponse> gerar(Principal principal, @Valid @RequestBody RecommendationInput input) {
		return service.generate(identity.currentUser(principal), input);
	}

	@GetMapping({"", "/"})
	public PageResponse<RecommendationResponse> listar(Principal principal, @RequestParam UUID deviceId,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
		return service.listRecommendations(identity.currentUser(principal), deviceId, page, size);
	}

	@GetMapping("/{id}")
	public RecommendationResponse buscarPorID(Principal principal, @PathVariable UUID id) {
		return service.getRecommendation(identity.currentUser(principal), id);
	}

	@PostMapping("/records")
	@ApiResponse(responseCode = "201", description = "Registro criado.", useReturnTypeSchema = true)
	public ResponseEntity<RecommendationResponse> cadastrar(Principal principal, @Valid @RequestBody RecommendationWriteInput input) {
		var item = service.createRecommendation(identity.currentUser(principal), input);
		return ResponseEntity.created(URI.create("/api/v1/recommendations/" + item.id())).body(item);
	}

	@PutMapping("/{id}")
	public RecommendationResponse atualizar(Principal principal, @PathVariable UUID id, @Valid @RequestBody RecommendationWriteInput input) {
		return service.updateRecommendation(identity.currentUser(principal), id, input);
	}

	@DeleteMapping("/{id}")
	@ApiResponse(responseCode = "204", description = "Operação concluída sem corpo de resposta.", useReturnTypeSchema = true)
	public ResponseEntity<Void> remover(Principal principal, @PathVariable UUID id) {
		service.deleteRecommendation(identity.currentUser(principal), id);
		return ResponseEntity.noContent().build();
	}
}
