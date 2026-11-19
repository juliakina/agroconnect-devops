package br.com.fiap.agroconnect_api.service;

import br.com.fiap.agroconnect_api.model.AiInteraction;
import br.com.fiap.agroconnect_api.model.Crop;
import br.com.fiap.agroconnect_api.model.DeviceEvent;
import br.com.fiap.agroconnect_api.model.WeatherHistory;
import br.com.fiap.agroconnect_api.record.AiInteractionInput;
import br.com.fiap.agroconnect_api.record.AiInteractionResponse;
import br.com.fiap.agroconnect_api.record.CropInput;
import br.com.fiap.agroconnect_api.record.CropResponse;
import br.com.fiap.agroconnect_api.record.DeviceEventInput;
import br.com.fiap.agroconnect_api.record.DeviceEventResponse;
import br.com.fiap.agroconnect_api.record.PageResponse;
import br.com.fiap.agroconnect_api.record.UserInput;
import br.com.fiap.agroconnect_api.record.UserResponse;
import br.com.fiap.agroconnect_api.record.WeatherHistoryInput;
import br.com.fiap.agroconnect_api.record.WeatherHistoryResponse;
import br.com.fiap.agroconnect_api.repository.AiInteractionRepository;
import br.com.fiap.agroconnect_api.repository.CropRepository;
import br.com.fiap.agroconnect_api.repository.DeviceEventRepository;
import br.com.fiap.agroconnect_api.repository.UserRepository;
import br.com.fiap.agroconnect_api.repository.WeatherHistoryRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdditionalDomainService {

	@Autowired
	private DomainSupport support;
	@Autowired
	private UserRepository users;
	@Autowired
	private CropRepository crops;
	@Autowired
	private DeviceEventRepository events;
	@Autowired
	private WeatherHistoryRepository weather;
	@Autowired
	private AiInteractionRepository interactions;

	private Crop requireCrop(UUID userId, UUID id) {
		return crops.findByIdAndUserId(id, userId).orElseThrow(() -> support.missing("Crop"));
	}

	private CropResponse response(Crop item) {
		return new CropResponse(item.getId(), item.getCode(), item.getName(), item.getScientificName(), item.getDescription());
	}

	private void transfer(Crop item, UUID userId, CropInput input) {
		support.validate(input);
		item.setUser(support.user(userId));
		item.setCode(input.code());
		item.setName(input.name());
		item.setScientificName(input.scientificName());
		item.setDescription(input.description());
	}

	public PageResponse<CropResponse> listCrop(UUID userId, int page, int size) {
		
		return PageResponse.from(crops.findByUserId(userId, support.page(page, size, "name", "id")).map(this::response));
	}

	public CropResponse getCrop(UUID userId, UUID id) {
		return response(requireCrop(userId, id));
	}

	@Transactional
	public CropResponse createCrop(UUID userId, CropInput input) {
		Crop item = new Crop();
		transfer(item, userId, input);
		return response(crops.saveAndFlush(item));
	}

	@Transactional
	public CropResponse updateCrop(UUID userId, UUID id, CropInput input) {
		Crop item = requireCrop(userId, id);
		transfer(item, userId, input);
		crops.flush();
		return response(item);
	}

	@Transactional
	public void deleteCrop(UUID userId, UUID id) {
		requireCrop(userId, id);
		crops.deleteRecord(id);
	}

	private DeviceEvent requireDeviceEvent(UUID userId, UUID id) {
		return events.findByIdAndDeviceFieldPropertyUserId(id, userId).orElseThrow(() -> support.missing("DeviceEvent"));
	}

	private DeviceEventResponse response(DeviceEvent item) {
		return new DeviceEventResponse(item.getId(), item.getDevice().getId(), item.getType(), item.getDescription(), item.getOccurredAt(), item.getOrigin());
	}

	private void transfer(DeviceEvent item, UUID userId, DeviceEventInput input) {
		support.validate(input);
		item.setDevice(support.device(userId, input.deviceId()));
		item.setType(input.type());
		item.setDescription(input.description());
		item.setOccurredAt(input.occurredAt());
		item.setOrigin(input.origin());
	}

	public PageResponse<DeviceEventResponse> listDeviceEvent(UUID userId, UUID deviceId, int page, int size) {
		support.device(userId, deviceId);
		return PageResponse.from(events.findByDeviceIdAndDeviceFieldPropertyUserId(deviceId, userId, support.page(page, size, Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.asc("id")))).map(this::response));
	}

	public DeviceEventResponse getDeviceEvent(UUID userId, UUID id) {
		return response(requireDeviceEvent(userId, id));
	}

	@Transactional
	public DeviceEventResponse createDeviceEvent(UUID userId, DeviceEventInput input) {
		DeviceEvent item = new DeviceEvent();
		transfer(item, userId, input);
		return response(events.saveAndFlush(item));
	}

	@Transactional
	public DeviceEventResponse updateDeviceEvent(UUID userId, UUID id, DeviceEventInput input) {
		DeviceEvent item = requireDeviceEvent(userId, id);
		transfer(item, userId, input);
		events.flush();
		return response(item);
	}

	@Transactional
	public void deleteDeviceEvent(UUID userId, UUID id) {
		requireDeviceEvent(userId, id);
		events.deleteRecord(id);
	}

	private WeatherHistory requireWeatherHistory(UUID userId, UUID id) {
		return weather.findByIdAndPropertyUserId(id, userId).orElseThrow(() -> support.missing("WeatherHistory"));
	}

	private WeatherHistoryResponse response(WeatherHistory item) {
		return new WeatherHistoryResponse(item.getId(), item.getProperty().getId(), item.getObservedAt(), item.getTemperatureC(), item.getRelativeHumidity(), item.getPrecipitationMm(), item.getWindSpeedKmh(), item.getOrigin());
	}

	private void transfer(WeatherHistory item, UUID userId, WeatherHistoryInput input) {
		support.validate(input);
		item.setProperty(support.property(userId, input.propertyId()));
		if (input.temperatureC() == null && input.relativeHumidity() == null && input.precipitationMm() == null && input.windSpeedKmh() == null)
			throw support.invalid("observedAt", "Informe ao menos uma medida meteorológica.");
		item.setObservedAt(input.observedAt());
		item.setTemperatureC(input.temperatureC());
		item.setRelativeHumidity(input.relativeHumidity());
		item.setPrecipitationMm(input.precipitationMm());
		item.setWindSpeedKmh(input.windSpeedKmh());
		item.setOrigin(input.origin());
	}

	public PageResponse<WeatherHistoryResponse> listWeatherHistory(UUID userId, UUID propertyId, int page, int size) {
		support.property(userId, propertyId);
		return PageResponse.from(weather.findByPropertyIdAndPropertyUserId(propertyId, userId, support.page(page, size, Sort.by(Sort.Order.desc("observedAt"), Sort.Order.asc("id")))).map(this::response));
	}

	public WeatherHistoryResponse getWeatherHistory(UUID userId, UUID id) {
		return response(requireWeatherHistory(userId, id));
	}

	@Transactional
	public WeatherHistoryResponse createWeatherHistory(UUID userId, WeatherHistoryInput input) {
		WeatherHistory item = new WeatherHistory();
		transfer(item, userId, input);
		return response(weather.saveAndFlush(item));
	}

	@Transactional
	public WeatherHistoryResponse updateWeatherHistory(UUID userId, UUID id, WeatherHistoryInput input) {
		WeatherHistory item = requireWeatherHistory(userId, id);
		transfer(item, userId, input);
		weather.flush();
		return response(item);
	}

	@Transactional
	public void deleteWeatherHistory(UUID userId, UUID id) {
		requireWeatherHistory(userId, id);
		weather.deleteRecord(id);
	}

	private AiInteraction requireAiInteraction(UUID userId, UUID id) {
		return interactions.findByIdAndUserId(id, userId).orElseThrow(() -> support.missing("AiInteraction"));
	}

	private AiInteractionResponse response(AiInteraction item) {
		return new AiInteractionResponse(item.getId(), item.getProperty() == null ? null : item.getProperty().getId(), item.getDevice() == null ? null : item.getDevice().getId(), item.getQuestion(), item.getResponse(), item.getContext(), item.getModelName(), item.getCreatedAt());
	}

	private void transfer(AiInteraction item, UUID userId, AiInteractionInput input) {
		support.validate(input);
		item.setUser(support.user(userId));
		var property = input.propertyId() == null ? null : support.property(userId, input.propertyId());
		var device = input.deviceId() == null ? null : support.device(userId, input.deviceId());
		if (property != null && device != null && !device.getField().getProperty().getId().equals(property.getId()))
			throw support.invalid("deviceId", "O dispositivo deve pertencer à propriedade informada.");
		item.setProperty(property == null && device != null ? device.getField().getProperty() : property);
		item.setDevice(device);
		item.setQuestion(input.question());
		item.setResponse(input.response());
		item.setContext(input.context());
		item.setModelName(input.modelName());
	}

	public PageResponse<AiInteractionResponse> listAiInteraction(UUID userId, int page, int size) {
		
		return PageResponse.from(interactions.findByUserId(userId, support.page(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")))).map(this::response));
	}

	public AiInteractionResponse getAiInteraction(UUID userId, UUID id) {
		return response(requireAiInteraction(userId, id));
	}

	@Transactional
	public AiInteractionResponse createAiInteraction(UUID userId, AiInteractionInput input) {
		AiInteraction item = new AiInteraction();
		transfer(item, userId, input);
		return response(interactions.saveAndFlush(item));
	}

	@Transactional
	public AiInteractionResponse updateAiInteraction(UUID userId, UUID id, AiInteractionInput input) {
		AiInteraction item = requireAiInteraction(userId, id);
		transfer(item, userId, input);
		interactions.flush();
		return response(item);
	}

	@Transactional
	public void deleteAiInteraction(UUID userId, UUID id) {
		requireAiInteraction(userId, id);
		interactions.deleteRecord(id);
	}

	public UserResponse getUser(UUID userId) {
		var user = support.user(userId);
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
	}

	@Transactional
	public UserResponse updateUser(UUID userId, UserInput input) {
		support.validate(input);
		var user = support.user(userId);
		var email = input.email().toLowerCase(Locale.ROOT);
		users.findByEmailIgnoreCase(email).filter(existing -> !existing.getId().equals(userId)).ifPresent(existing -> {
			throw new ApiException(409, "CONFLICT", "Este e-mail já está cadastrado.");
		});
		user.setName(input.name());
		user.setEmail(email);
		users.flush();
		return getUser(userId);
	}
}
