package br.com.fiap.agroconnect_api.service;

import br.com.fiap.agroconnect_api.model.Device;
import br.com.fiap.agroconnect_api.model.Reading;
import br.com.fiap.agroconnect_api.model.Sensor;
import br.com.fiap.agroconnect_api.record.DeviceInput;
import br.com.fiap.agroconnect_api.record.DeviceResponse;
import br.com.fiap.agroconnect_api.record.HistoryPoint;
import br.com.fiap.agroconnect_api.record.HistoryResponse;
import br.com.fiap.agroconnect_api.record.HistorySummary;
import br.com.fiap.agroconnect_api.record.LatestReadingsResponse;
import br.com.fiap.agroconnect_api.record.PageResponse;
import br.com.fiap.agroconnect_api.record.ReadingDetailsResponse;
import br.com.fiap.agroconnect_api.record.ReadingInput;
import br.com.fiap.agroconnect_api.record.ReadingResponse;
import br.com.fiap.agroconnect_api.record.ReadingSnapshot;
import br.com.fiap.agroconnect_api.record.SensorInput;
import br.com.fiap.agroconnect_api.record.SensorResponse;
import br.com.fiap.agroconnect_api.repository.DeviceRepository;
import br.com.fiap.agroconnect_api.repository.ReadingRepository;
import br.com.fiap.agroconnect_api.repository.SensorRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MonitoringService {

	@Autowired
	private DomainSupport support;
	@Autowired
	private DeviceRepository devices;
	@Autowired
	private SensorRepository sensors;
	@Autowired
	private ReadingRepository readings;
	@Autowired
	private AlertAuditService auditService;

	public static String deviceStatus(Instant lastSeenAt, Instant now) {
		if (lastSeenAt == null) return "UNKNOWN";
		Duration age = Duration.between(lastSeenAt, now);
		return age.compareTo(Duration.ofMinutes(5)) <= 0 ? "ONLINE"
				: age.compareTo(Duration.ofMinutes(30)) <= 0 ? "UNSTABLE" : "OFFLINE";
	}

	private DeviceResponse response(Device item) {
		return new DeviceResponse(item.getId(), item.getCode(), item.getName(), item.getField().getId(),
				deviceStatus(item.getLastSeenAt(), Instant.now()), item.getLastSeenAt(), item.getRssi(), item.getFirmwareVersion());
	}

	private SensorResponse response(Sensor item) {
		return new SensorResponse(item.getId(), item.getDevice().getId(), item.getType(), item.getLabel(), item.getUnit());
	}

	private ReadingDetailsResponse details(Reading item) {
		return new ReadingDetailsResponse(item.getId(), item.getSensor().getId(), item.getValue(), item.getQuality(),
				item.getRecordedAt(), item.getReceivedAt(), item.getSimulatedData(), item.getOrigin(), item.getObservations());
	}

	public PageResponse<DeviceResponse> listDevices(UUID userId, UUID fieldId, int page, int size) {
		return PageResponse.from(devices.findOwned(userId, fieldId, support.page(page, size, "code", "id")).map(this::response));
	}

	public DeviceResponse getDevice(UUID userId, UUID id) {
		return response(support.device(userId, id));
	}

	private void transfer(Device item, UUID userId, DeviceInput input) {
		support.validate(input);
		item.setField(support.field(userId, input.fieldId()));
		item.setCode(input.code());
		item.setName(input.name());
		item.setLastSeenAt(input.lastSeenAt());
		item.setRssi(input.rssi());
		item.setFirmwareVersion(input.firmwareVersion());
		item.setTransport(input.transport() == null ? "WIFI" : input.transport());
		item.setSimulatedData(input.simulatedData() == null || input.simulatedData());
		item.setSoilMoistureThreshold(input.soilMoistureThreshold() == null ? new BigDecimal("30") : input.soilMoistureThreshold());
	}

	@Transactional
	public DeviceResponse createDevice(UUID userId, DeviceInput input) {
		Device item = new Device();
		transfer(item, userId, input);
		return response(devices.saveAndFlush(item));
	}

	@Transactional
	public DeviceResponse updateDevice(UUID userId, UUID id, DeviceInput input) {
		Device item = support.device(userId, id);
		transfer(item, userId, input);
		devices.flush();
		return response(item);
	}

	@Transactional
	public void deleteDevice(UUID userId, UUID id) {
		support.device(userId, id);
		auditService.deviceDeletion(userId, id);
		devices.deleteRecord(id);
	}

	public PageResponse<SensorResponse> listSensors(UUID userId, UUID deviceId, int page, int size) {
		support.device(userId, deviceId);
		return PageResponse.from(sensors.findByDeviceIdAndDeviceFieldPropertyUserId(deviceId, userId,
				support.page(page, size, "type", "id")).map(this::response));
	}

	public SensorResponse getSensor(UUID userId, UUID id) {
		return response(support.sensor(userId, id));
	}

	private void transfer(Sensor item, UUID userId, SensorInput input) {
		support.validate(input);
		if (item.getId() != null && !item.getDevice().getId().equals(input.deviceId()))
			throw support.invalid("deviceId", "O dispositivo de um sensor existente não pode ser alterado.");
		item.setDevice(support.device(userId, input.deviceId()));
		item.setType(input.type());
		item.setLabel(input.label());
		item.setUnit(input.unit());
		item.setCode(input.code());
		item.setCalibrated(input.calibrated() == null || input.calibrated());
		item.setInstalledAt(input.installedAt());
	}

	@Transactional
	public SensorResponse createSensor(UUID userId, SensorInput input) {
		Sensor item = new Sensor();
		transfer(item, userId, input);
		return response(sensors.saveAndFlush(item));
	}

	@Transactional
	public SensorResponse updateSensor(UUID userId, UUID id, SensorInput input) {
		Sensor item = support.sensor(userId, id);
		transfer(item, userId, input);
		sensors.flush();
		return response(item);
	}

	@Transactional
	public void deleteSensor(UUID userId, UUID id) {
		support.sensor(userId, id);
		sensors.deleteRecord(id);
	}

	private Reading reading(UUID userId, UUID id) {
		return readings.findByIdAndSensorDeviceFieldPropertyUserId(id, userId).orElseThrow(() -> support.missing("Leitura"));
	}

	public PageResponse<ReadingDetailsResponse> listReadings(UUID userId, UUID sensorId, int page, int size) {
		support.sensor(userId, sensorId);
		return PageResponse.from(readings.findBySensorIdAndSensorDeviceFieldPropertyUserId(sensorId, userId,
				support.page(page, size, Sort.by(Sort.Order.desc("recordedAt"), Sort.Order.asc("id")))).map(this::details));
	}

	public ReadingDetailsResponse getReading(UUID userId, UUID id) {
		return details(reading(userId, id));
	}

	private void transfer(Reading item, UUID userId, ReadingInput input) {
		support.validate(input);
		if (item.getId() != null && !item.getSensor().getId().equals(input.sensorId()))
			throw support.invalid("sensorId", "O sensor de uma leitura existente não pode ser alterado.");
		item.setSensor(support.sensor(userId, input.sensorId()));
		item.setValue(input.value());
		item.setQuality(input.quality());
		item.setRecordedAt(input.recordedAt());
		item.setReceivedAt(input.receivedAt() == null ? input.recordedAt() : input.receivedAt());
		item.setSimulatedData(input.simulatedData() == null || input.simulatedData());
		item.setOrigin(input.origin() == null ? (item.getSimulatedData() ? "SIMULATED" : "REAL") : input.origin());
		if (("REAL".equals(item.getOrigin()) && item.getSimulatedData())
				|| ("SIMULATED".equals(item.getOrigin()) && !item.getSimulatedData()))
			throw support.invalid("simulatedData", "A origem deve ser compatível com o indicador de simulação.");
		item.setObservations(input.observations());
		Device device = item.getSensor().getDevice();
		if (!"ESTIMATED".equals(item.getOrigin()) && (device.getLastSeenAt() == null
				|| device.getLastSeenAt().isBefore(item.getReceivedAt()))) device.setLastSeenAt(item.getReceivedAt());
	}

	@Transactional
	public ReadingDetailsResponse createReading(UUID userId, ReadingInput input) {
		Reading item = new Reading();
		transfer(item, userId, input);
		return details(readings.saveAndFlush(item));
	}

	@Transactional
	public ReadingDetailsResponse updateReading(UUID userId, UUID id, ReadingInput input) {
		Reading item = reading(userId, id);
		transfer(item, userId, input);
		return details(item);
	}

	@Transactional
	public void deleteReading(UUID userId, UUID id) {
		reading(userId, id);
		readings.deleteRecord(id);
	}

	private List<Reading> latestRows(UUID deviceId, boolean validOnly) {
		List<Reading> rows = new ArrayList<>();
		for (Sensor sensor : sensors.findByDeviceIdOrderByTypeAscIdAsc(deviceId)) {
			// As evidências usam a última leitura válida, mesmo se uma falha chegou depois.
			var row = validOnly ? readings.findFirstBySensorIdAndQualityAndValueIsNotNullOrderByRecordedAtDescIdAsc(sensor.getId(), "VALID")
					: readings.findFirstBySensorIdOrderByRecordedAtDescIdAsc(sensor.getId());
			row.ifPresent(rows::add);
		}
		return rows;
	}

	public List<Reading> evidenceRows(UUID userId, UUID deviceId) {
		support.device(userId, deviceId);
		return latestRows(deviceId, true).stream().limit(10).toList();
	}

	public ReadingSnapshot snapshot(Reading row) {
		return new ReadingSnapshot(row.getSensor().getType(), row.getValue(), row.getSensor().getUnit(), row.getRecordedAt());
	}

	public LatestReadingsResponse latest(UUID userId, UUID deviceId) {
		support.device(userId, deviceId);
		var rows = latestRows(deviceId, false);
		var result = rows.stream().map(row -> new ReadingResponse(row.getSensor().getId(), row.getSensor().getType(),
				"VALID".equals(row.getQuality()) ? row.getValue() : null, row.getSensor().getUnit(),
				row.getQuality(), row.getRecordedAt())).toList();
		return new LatestReadingsResponse(deviceId, rows.stream().map(Reading::getRecordedAt).max(Comparator.naturalOrder()).orElse(null),
				result, rows.isEmpty() || rows.stream().anyMatch(Reading::getSimulatedData));
	}

	private BigDecimal avg(List<BigDecimal> values) {
		return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), MathContext.DECIMAL128);
	}

	private HistoryPoint point(Instant at, List<Reading> rows) {
		var values = rows.stream().filter(r -> "VALID".equals(r.getQuality()) && r.getValue() != null).map(Reading::getValue).toList();
		return values.isEmpty() ? new HistoryPoint(at, null, null, null, rows.size(), rows.get(0).getQuality())
				: new HistoryPoint(at, avg(values), values.stream().min(BigDecimal::compareTo).orElseThrow(),
						values.stream().max(BigDecimal::compareTo).orElseThrow(), rows.size(), "VALID");
	}

	public HistoryResponse history(UUID userId, UUID deviceId, UUID sensorId, String type,
			Instant from, Instant to, String interval) {
		support.device(userId, deviceId);
		if (sensorId == null && (type == null || type.isBlank())) throw support.invalid("sensorId", "Informe type ou sensorId.");
		interval = interval == null ? "raw" : interval;
		int minutes = switch (interval) { case "raw" -> 0; case "5m" -> 5; case "15m" -> 15; case "1h" -> 60; case "1d" -> 1440; default -> -1; };
		Instant end = to == null ? Instant.now() : to;
		Instant start = from == null ? end.minus(Duration.ofDays(1)) : from;
		if (minutes < 0 || !start.isBefore(end) || Duration.between(start, end).compareTo(Duration.ofDays(366)) > 0)
			throw support.invalid("interval", "Use raw, 5m, 15m, 1h ou 1d e período de até 366 dias.");
		Sensor sensor = sensors.findByDeviceIdOrderByTypeAscIdAsc(deviceId).stream()
				.filter(s -> (sensorId == null || sensorId.equals(s.getId())) && (type == null || type.isBlank() || type.equals(s.getType())))
				.min(Comparator.comparing(s -> s.getId().toString())).orElseThrow(() -> support.missing("Sensor neste dispositivo"));
		var rows = readings.findBySensorIdAndRecordedAtBetweenOrderByRecordedAtAscIdAsc(sensor.getId(), start, end, PageRequest.of(0, 50001));
		if (rows.size() > 50000) throw new ApiException(422, "HISTORY_TOO_LARGE", "Use um período menor para consultar o histórico.");
		List<HistoryPoint> items;
		if (minutes == 0) items = rows.stream().map(row -> point(row.getRecordedAt(), List.of(row))).toList();
		else {
			Map<Instant, List<Reading>> buckets = new TreeMap<>();
			long seconds = minutes * 60L;
			for (Reading row : rows) {
				Instant at = Instant.ofEpochSecond(Math.floorDiv(row.getRecordedAt().getEpochSecond(), seconds) * seconds);
				buckets.computeIfAbsent(at, key -> new ArrayList<>()).add(row);
			}
			items = buckets.entrySet().stream().map(e -> point(e.getKey(), e.getValue())).toList();
		}
		var values = rows.stream().filter(r -> "VALID".equals(r.getQuality()) && r.getValue() != null).map(Reading::getValue).toList();
		var summary = values.isEmpty() ? null : new HistorySummary(values.stream().min(BigDecimal::compareTo).orElseThrow(),
				values.stream().max(BigDecimal::compareTo).orElseThrow(), avg(values));
		return new HistoryResponse(deviceId, sensor.getId(), sensor.getType(), sensor.getUnit(), interval, items, summary,
				rows.isEmpty() || rows.stream().anyMatch(Reading::getSimulatedData));
	}
}
