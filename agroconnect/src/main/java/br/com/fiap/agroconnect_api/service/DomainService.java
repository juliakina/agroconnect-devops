package br.com.fiap.agroconnect_api.service;

import br.com.fiap.agroconnect_api.model.Field;
import br.com.fiap.agroconnect_api.model.Planting;
import br.com.fiap.agroconnect_api.model.Property;
import br.com.fiap.agroconnect_api.model.User;
import br.com.fiap.agroconnect_api.record.FieldInput;
import br.com.fiap.agroconnect_api.record.FieldResponse;
import br.com.fiap.agroconnect_api.record.PageResponse;
import br.com.fiap.agroconnect_api.record.PlantingDetailsResponse;
import br.com.fiap.agroconnect_api.record.PlantingInput;
import br.com.fiap.agroconnect_api.record.PlantingResponse;
import br.com.fiap.agroconnect_api.record.PropertyInput;
import br.com.fiap.agroconnect_api.record.PropertyPatch;
import br.com.fiap.agroconnect_api.record.PropertyResponse;
import br.com.fiap.agroconnect_api.repository.CropRepository;
import br.com.fiap.agroconnect_api.repository.FieldRepository;
import br.com.fiap.agroconnect_api.repository.PlantingRepository;
import br.com.fiap.agroconnect_api.repository.PropertyRepository;
import br.com.fiap.agroconnect_api.repository.UserRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.security.Principal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DomainService {

	@Autowired
	private PropertyRepository repP;
	@Autowired
	private FieldRepository repF;
	@Autowired
	private PlantingRepository repPlanting;
	@Autowired
	private UserRepository repU;
	@Autowired
	private Validator validator;
	@Autowired
	private CropRepository repCrop;
	@Autowired
	private AlertAuditService auditService;

	public UUID currentUser(Principal principal) {
		if (principal == null) {
			throw new ApiException(401, "UNAUTHORIZED", "Autenticação necessária.");
		}
		try {
			return UUID.fromString(principal.getName());
		} catch (IllegalArgumentException exception) {
			return repU.findByEmail(principal.getName()).map(User::getId)
					.orElseThrow(() -> new ApiException(401, "UNAUTHORIZED", "Usuário da API não encontrado."));
		}
	}

	private Pageable pagination(int page, int size, Sort sort) {
		return new LimitedOffsetPageRequest(Math.max(1, page) - 1, Math.max(1, Math.min(100, size)), sort);
	}

	private static class LimitedOffsetPageRequest extends PageRequest {

		LimitedOffsetPageRequest(int page, int size, Sort sort) {
			super(page, size, sort);
		}

		@Override
		public long getOffset() {
			return Math.min(Integer.MAX_VALUE, super.getOffset());
		}
	}

	private ApiException missing(String resource) {
		return new ApiException(404, "NOT_FOUND", resource + " não encontrado(a).");
	}

	private Property property(UUID userId, UUID id) {
		return repP.findByIdAndUserId(id, userId).orElseThrow(() -> missing("Propriedade"));
	}

	private Field field(UUID userId, UUID id) {
		return repF.findByIdAndPropertyUserId(id, userId).orElseThrow(() -> missing("Talhão"));
	}

	private Planting planting(UUID userId, UUID fieldId, UUID id) {
		return repPlanting.findByIdAndFieldIdAndFieldPropertyUserId(id, fieldId, userId)
				.orElseThrow(() -> missing("Plantio"));
	}

	private <T> T validate(T input) {
		var errors = validator.validate(input);
		if (!errors.isEmpty()) {
			throw new ConstraintViolationException(errors);
		}
		return input;
	}

	private PropertyInput normalize(PropertyInput input) {
		return validate(new PropertyInput(input.name() == null ? null : input.name().trim(),
				input.city() == null ? null : input.city().trim(),
				input.state() == null ? null : input.state().trim().toUpperCase(Locale.ROOT),
				input.areaHectares(), input.description()));
	}

	private PropertyResponse response(Property item) {
		return new PropertyResponse(item.getId(), item.getName(), item.getCity(), item.getState(),
				item.getAreaHectares(), item.getDescription(), item.getCreatedAt(), item.getUpdatedAt());
	}

	private FieldResponse response(Field item) {
		return new FieldResponse(item.getId(), item.getProperty().getId(), item.getName(), item.getAreaHectares());
	}

	private PlantingResponse response(Planting item) {
		return new PlantingResponse(item.getId(), item.getField().getId(), item.getCropName(),
				item.getPlantedAt(), item.getStage());
	}

	public PageResponse<PropertyResponse> listProperties(UUID userId, int page, int size) {
		return PageResponse.from(repP.findByUserId(userId,
				pagination(page, size, Sort.by("name", "id"))).map(this::response));
	}

	public PropertyResponse getProperty(UUID userId, UUID id) {
		return response(property(userId, id));
	}

	private void transfer(Property item, PropertyInput input) {
		item.setName(input.name());
		item.setCity(input.city());
		item.setState(input.state());
		item.setAreaHectares(input.areaHectares());
		item.setDescription(input.description());
	}

	@Transactional
	public PropertyResponse createProperty(UUID userId, PropertyInput input) {
		input = normalize(input);
		User user = repU.findById(userId)
				.orElseThrow(() -> new ApiException(401, "UNAUTHORIZED", "Usuário da API não encontrado."));
		Property item = new Property();
		item.setUser(user);
		transfer(item, input);
		return response(repP.save(item));
	}

	@Transactional
	public PropertyResponse patchProperty(UUID userId, UUID id, PropertyPatch input) {
		Property item = property(userId, id);
		PropertyInput merged = normalize(new PropertyInput(
				input.getName() == null ? item.getName() : input.getName(),
				input.getCity() == null ? item.getCity() : input.getCity(),
				input.getState() == null ? item.getState() : input.getState(),
				input.getAreaHectares() == null ? item.getAreaHectares() : input.getAreaHectares(),
				input.hasDescription() ? input.getDescription() : item.getDescription()));
		boolean changed = !item.getName().equals(merged.name()) || !item.getCity().equals(merged.city())
				|| !item.getState().equals(merged.state())
				|| item.getAreaHectares().compareTo(merged.areaHectares()) != 0
				|| !Objects.equals(item.getDescription(), merged.description());
		if (changed) {
			transfer(item, merged);
			item.setUpdatedAt(Instant.now());
		}
		return response(item);
	}

	@Transactional
	public void deleteProperty(UUID userId, UUID id) {
		property(userId, id);
		auditService.propertyDeletion(userId, id);
		repP.deleteRecord(id);
	}

	public PageResponse<FieldResponse> listFields(UUID userId, UUID propertyId, int page, int size) {
		property(userId, propertyId);
		return PageResponse.from(repF.findByPropertyIdAndPropertyUserId(propertyId, userId,
				pagination(page, size, Sort.by("name", "id"))).map(this::response));
	}

	public FieldResponse getField(UUID userId, UUID id) {
		return response(field(userId, id));
	}

	private void transfer(Field item, FieldInput input) {
		input = validate(new FieldInput(input.name() == null ? null : input.name().trim(), input.areaHectares()));
		item.setName(input.name());
		item.setAreaHectares(input.areaHectares());
	}

	@Transactional
	public FieldResponse createField(UUID userId, UUID propertyId, FieldInput input) {
		Field item = new Field();
		item.setProperty(property(userId, propertyId));
		transfer(item, input);
		return response(repF.save(item));
	}

	@Transactional
	public FieldResponse updateField(UUID userId, UUID id, FieldInput input) {
		Field item = field(userId, id);
		transfer(item, input);
		return response(item);
	}

	@Transactional
	public void deleteField(UUID userId, UUID id) {
		field(userId, id);
		auditService.fieldDeletion(userId, id);
		repF.deleteRecord(id);
	}

	public PageResponse<PlantingResponse> listPlantings(UUID userId, UUID fieldId, int page, int size) {
		field(userId, fieldId);
		return PageResponse.from(repPlanting.findByFieldIdAndFieldPropertyUserId(fieldId, userId,
				pagination(page, size, Sort.by(Sort.Order.desc("plantedAt"), Sort.Order.asc("id"))))
				.map(this::response));
	}

	public PlantingResponse getPlanting(UUID userId, UUID fieldId, UUID id) {
		return response(planting(userId, fieldId, id));
	}

	public PlantingDetailsResponse getPlantingDetails(UUID userId, UUID fieldId, UUID id) {
		var item = planting(userId, fieldId, id);
		return new PlantingDetailsResponse(item.getId(), item.getField().getId(),
				item.getCropName(), item.getPlantedAt(), item.getStage(),
				item.getCrop() == null ? null : item.getCrop().getId(), item.getVariety(), item.getStatus(), item.getObservations());
	}

	private void transfer(Planting item, PlantingInput input) {
		input = validate(input);
		item.setCropName(input.cropName());
		item.setPlantedAt(input.plantedAt());
		item.setStage(input.stage().toUpperCase(Locale.ROOT));
		UUID userId = item.getField().getProperty().getUser().getId();
		UUID cropId = input.cropId();
		item.setCrop(cropId == null ? null : repCrop.findByIdAndUserId(cropId, userId)
				.orElseThrow(() -> missing("Cultura")));
		item.setVariety(input.variety());
		item.setStatus(input.status() == null ? "ACTIVE" : input.status());
		item.setObservations(input.observations());
	}

	@Transactional
	public PlantingResponse createPlanting(UUID userId, UUID fieldId, PlantingInput input) {
		Planting item = new Planting();
		item.setField(field(userId, fieldId));
		transfer(item, input);
		return response(repPlanting.save(item));
	}

	@Transactional
	public PlantingResponse updatePlanting(UUID userId, UUID fieldId, UUID id, PlantingInput input) {
		Planting item = planting(userId, fieldId, id);
		transfer(item, input);
		return response(item);
	}

	@Transactional
	public void deletePlanting(UUID userId, UUID fieldId, UUID id) {
		repPlanting.delete(planting(userId, fieldId, id));
	}
}
