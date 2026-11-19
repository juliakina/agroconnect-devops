package br.com.fiap.agroconnect_api.control;

import br.com.fiap.agroconnect_api.record.AlertAuditResponse;
import br.com.fiap.agroconnect_api.record.PageResponse;
import br.com.fiap.agroconnect_api.service.AlertService;
import br.com.fiap.agroconnect_api.service.DomainService;
import java.security.Principal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alert-audits")
public class AlertAuditController {

	@Autowired
	private AlertService service;
	@Autowired
	private DomainService identity;

	@GetMapping({"", "/"})
	public PageResponse<AlertAuditResponse> listar(Principal principal,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
		return service.listAudits(identity.currentUser(principal), page, size);
	}

	@GetMapping("/{id}")
	public AlertAuditResponse buscarPorID(Principal principal, @PathVariable UUID id) {
		return service.getAudit(identity.currentUser(principal), id);
	}
}
