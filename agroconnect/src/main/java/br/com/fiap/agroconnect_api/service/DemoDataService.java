package br.com.fiap.agroconnect_api.service;

import br.com.fiap.agroconnect_api.model.Device;
import br.com.fiap.agroconnect_api.model.Field;
import br.com.fiap.agroconnect_api.model.Planting;
import br.com.fiap.agroconnect_api.model.Property;
import br.com.fiap.agroconnect_api.model.Reading;
import br.com.fiap.agroconnect_api.model.Sensor;
import br.com.fiap.agroconnect_api.record.AlertInput;
import br.com.fiap.agroconnect_api.repository.DeviceRepository;
import br.com.fiap.agroconnect_api.repository.FieldRepository;
import br.com.fiap.agroconnect_api.repository.PlantingRepository;
import br.com.fiap.agroconnect_api.repository.PropertyRepository;
import br.com.fiap.agroconnect_api.repository.ReadingRepository;
import br.com.fiap.agroconnect_api.repository.SensorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoDataService {

	@Autowired
	private DomainSupport support;
	@Autowired
	private PropertyRepository properties;
	@Autowired
	private FieldRepository fields;
	@Autowired
	private PlantingRepository plantings;
	@Autowired
	private DeviceRepository devices;
	@Autowired
	private SensorRepository sensors;
	@Autowired
	private ReadingRepository readings;
	@Autowired
	private AlertService alerts;
	@Autowired
	private Clock clock;

	@Transactional
	public void seed(UUID userId) {
		var user = support.user(userId);
		var onlineExisting = devices.findByCode("ESP32-001");
		var offlineExisting = devices.findByCode("ESP32-002");
		if (onlineExisting.isPresent() || offlineExisting.isPresent()) {
			boolean complete = onlineExisting.isPresent() && offlineExisting.isPresent()
					&& onlineExisting.get().getField().getProperty().getUser().getId().equals(userId)
					&& offlineExisting.get().getField().getProperty().getUser().getId().equals(userId);
			if (complete) return;
			throw new ApiException(409, "DEMO_DATA_CONFLICT", "Os códigos dos dispositivos de demonstração já estão em uso.");
		}
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		Property property = new Property();
		property.setUser(user);
		property.setName("Sítio Boa Esperança");
		property.setCity("Ibiúna");
		property.setState("SP");
		property.setAreaHectares(new BigDecimal("3.5"));
		property.setDescription("Propriedade de demonstração com dados simulados.");
		properties.save(property);
		Field field = field(property, "Talhão A - Hortaliças", "1.2");
		Field field2 = field(property, "Talhão B - Teste", "0.8");
		Planting planting = new Planting();
		planting.setField(field);
		planting.setCropName("Alface");
		planting.setPlantedAt(now.minus(18, ChronoUnit.DAYS));
		planting.setStage("VEGETATIVE");
		plantings.save(planting);
		Device device = device(field, "ESP32-001", "Sensor do Canteiro de Alface", now.minusSeconds(60), -61);
		Device offline = device(field2, "ESP32-002", "Estação de monitoramento offline", now.minusSeconds(10800), null);
		List<Sensor> rows = List.of(
				sensor(device, "SOIL_MOISTURE", "Umidade do solo", "%"),
				sensor(device, "AIR_TEMPERATURE", "Temperatura do ar", "°C"),
				sensor(device, "AIR_HUMIDITY", "Umidade do ar", "%"),
				sensor(device, "LIGHT_ILLUMINANCE", "Luminosidade", "lux"),
				sensor(offline, "SOIL_MOISTURE", "Umidade do solo", "%"));
		double[] values = {24.5, 26.2, 63, 12500};
		for (int sensor = 0; sensor < 4; sensor++) {
			for (int hour = 0; hour < 24; hour++) {
				double variation = Math.sin(hour / 3.0) * (sensor == 0 ? 2 : 1);
				BigDecimal value = BigDecimal.valueOf(values[sensor] + variation).setScale(3, RoundingMode.HALF_UP);
				reading(rows.get(sensor), value, "VALID", now.minus(23 - hour, ChronoUnit.HOURS));
			}
		}
		reading(rows.get(4), null, "SENSOR_ERROR", now.minusSeconds(10800));
		reading(rows.get(0), null, "UNCALIBRATED", now.minusSeconds(600));
		readings.flush();
		alerts.createAlert(userId, new AlertInput(device.getId(), "WARNING", "Umidade do solo abaixo do esperado",
				"A leitura recente indica umidade relativamente baixa.",
				"Valor demonstrativo abaixo da faixa de referência configurada para este cenário."));
	}

	private Field field(Property property, String name, String area) {
		Field item = new Field();
		item.setProperty(property);
		item.setName(name);
		item.setAreaHectares(new BigDecimal(area));
		return fields.save(item);
	}

	private Device device(Field field, String code, String name, Instant lastSeenAt, Integer rssi) {
		Device item = new Device();
		item.setField(field);
		item.setCode(code);
		item.setName(name);
		item.setLastSeenAt(lastSeenAt);
		item.setRssi(rssi);
		item.setFirmwareVersion("1.0.0");
		item.setSimulatedData(true);
		return devices.save(item);
	}

	private Sensor sensor(Device device, String type, String label, String unit) {
		Sensor item = new Sensor();
		item.setDevice(device);
		item.setType(type);
		item.setLabel(label);
		item.setUnit(unit);
		item.setCalibrated(true);
		return sensors.save(item);
	}

	private void reading(Sensor sensor, BigDecimal value, String quality, Instant recordedAt) {
		Reading item = new Reading();
		item.setSensor(sensor);
		item.setValue(value);
		item.setQuality(quality);
		item.setRecordedAt(recordedAt);
		item.setReceivedAt(recordedAt);
		item.setSimulatedData(true);
		item.setOrigin("SIMULATED");
		readings.save(item);
	}
}
