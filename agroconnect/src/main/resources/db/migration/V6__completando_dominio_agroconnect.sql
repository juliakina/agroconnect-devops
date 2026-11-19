-- ===== DOMINIO COMPLETO / H2 =====
ALTER TABLE agroconnect."Users" ALTER COLUMN "Email" VARCHAR(254) NOT NULL;
CREATE TABLE agroconnect."Devices"(
    "Id" UUID PRIMARY KEY,
    "FieldId" UUID NOT NULL REFERENCES agroconnect."Fields"("Id") ON DELETE CASCADE,
    "Code" VARCHAR(60) NOT NULL,
    "Name" VARCHAR(150) NOT NULL,
    "LastSeenAt" TIMESTAMP WITH TIME ZONE,
    "Rssi" INTEGER,
    "FirmwareVersion" VARCHAR(40) NOT NULL,
    "Transport" VARCHAR(20) NOT NULL,
    "SimulatedData" BOOLEAN NOT NULL,
    "SoilMoistureThreshold" NUMERIC(20,6) NOT NULL
);

CREATE TABLE agroconnect."Sensors"(
    "Id" UUID PRIMARY KEY,
    "DeviceId" UUID NOT NULL REFERENCES agroconnect."Devices"("Id") ON DELETE CASCADE,
    "Type" VARCHAR(50) NOT NULL,
    "Label" VARCHAR(150) NOT NULL,
    "Unit" VARCHAR(20) NOT NULL,
    "Code" VARCHAR(255),
    "Calibrated" BOOLEAN NOT NULL,
    "InstalledAt" TIMESTAMP WITH TIME ZONE
);

CREATE TABLE agroconnect."Readings"(
    "Id" UUID PRIMARY KEY,
    "SensorId" UUID NOT NULL REFERENCES agroconnect."Sensors"("Id") ON DELETE CASCADE,
    "Value" NUMERIC(12,3),
    "Quality" VARCHAR(20) NOT NULL,
    "RecordedAt" TIMESTAMP WITH TIME ZONE NOT NULL,
    "ReceivedAt" TIMESTAMP WITH TIME ZONE NOT NULL,
    "SimulatedData" BOOLEAN NOT NULL,
    "Origin" VARCHAR(20) NOT NULL,
    "Observations" VARCHAR(255)
);

CREATE TABLE agroconnect."Alerts"(
    "Id" UUID PRIMARY KEY,
    "DeviceId" UUID NOT NULL REFERENCES agroconnect."Devices"("Id") ON DELETE CASCADE,
    "Severity" VARCHAR(20) NOT NULL,
    "Status" VARCHAR(20) NOT NULL,
    "Title" VARCHAR(120) NOT NULL,
    "Message" VARCHAR(500) NOT NULL,
    "Reason" VARCHAR(500) NOT NULL,
    "PushPending" BOOLEAN NOT NULL,
    "BasedOnJson" VARCHAR(20000) NOT NULL,
    "CreatedAt" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE agroconnect."Recommendations"(
    "Id" UUID PRIMARY KEY,
    "DeviceId" UUID NOT NULL REFERENCES agroconnect."Devices"("Id") ON DELETE CASCADE,
    "SourceAlertId" UUID,
    "Priority" VARCHAR(20) NOT NULL,
    "Title" VARCHAR(180) NOT NULL,
    "Message" VARCHAR(2000) NOT NULL,
    "Reason" VARCHAR(2000) NOT NULL,
    "Disclaimer" VARCHAR(1000) NOT NULL,
    "SimulatedData" BOOLEAN NOT NULL,
    "ModelName" VARCHAR(255),
    "BasedOnJson" VARCHAR(20000) NOT NULL,
    "GeneratedAt" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE agroconnect."Crops"(
    "Id" UUID PRIMARY KEY,
    "UserId" UUID NOT NULL REFERENCES agroconnect."Users"("Id"),
    "Code" VARCHAR(40) NOT NULL,
    "Name" VARCHAR(100) NOT NULL,
    "ScientificName" VARCHAR(255),
    "Description" VARCHAR(255)
);

CREATE TABLE agroconnect."DeviceEvents"(
    "Id" UUID PRIMARY KEY,
    "DeviceId" UUID NOT NULL REFERENCES agroconnect."Devices"("Id") ON DELETE CASCADE,
    "Type" VARCHAR(30) NOT NULL,
    "Description" VARCHAR(1000) NOT NULL,
    "OccurredAt" TIMESTAMP WITH TIME ZONE NOT NULL,
    "Origin" VARCHAR(20) NOT NULL
);

CREATE TABLE agroconnect."AiInteractions"(
    "Id" UUID PRIMARY KEY,
    "UserId" UUID NOT NULL REFERENCES agroconnect."Users"("Id"),
    "PropertyId" UUID REFERENCES agroconnect."Properties"("Id") ON DELETE SET NULL,
    "DeviceId" UUID REFERENCES agroconnect."Devices"("Id") ON DELETE SET NULL,
    "Question" VARCHAR(20000) NOT NULL,
    "Response" VARCHAR(20000),
    "Context" VARCHAR(20000),
    "ModelName" VARCHAR(255),
    "CreatedAt" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE agroconnect."WeatherHistory"(
    "Id" UUID PRIMARY KEY,
    "PropertyId" UUID NOT NULL REFERENCES agroconnect."Properties"("Id") ON DELETE CASCADE,
    "ObservedAt" TIMESTAMP WITH TIME ZONE NOT NULL,
    "TemperatureC" NUMERIC(20,6),
    "RelativeHumidity" NUMERIC(20,6),
    "PrecipitationMm" NUMERIC(20,6),
    "WindSpeedKmh" NUMERIC(20,6),
    "Origin" VARCHAR(20) NOT NULL
);

CREATE TABLE agroconnect."AlertAudits"(
    "Id" UUID PRIMARY KEY,
    "AlertId" UUID NOT NULL,
    "UserId" UUID NOT NULL,
    "Operation" VARCHAR(10) NOT NULL,
    "OccurredAt" TIMESTAMP WITH TIME ZONE NOT NULL,
    "DatabaseUser" VARCHAR(254) NOT NULL,
    "OldValues" VARCHAR(20000),
    "NewValues" VARCHAR(20000)
);

CREATE TABLE agroconnect."Sessions"(
    "Id" UUID PRIMARY KEY,
    "UserId" UUID NOT NULL REFERENCES agroconnect."Users"("Id") ON DELETE CASCADE,
    "ExpiresAt" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE agroconnect."PushTokens"(
    "Id" UUID PRIMARY KEY,
    "UserId" UUID NOT NULL REFERENCES agroconnect."Users"("Id"),
    "SessionId" UUID NOT NULL REFERENCES agroconnect."Sessions"("Id"),
    "Token" VARCHAR(512) NOT NULL,
    "Platform" VARCHAR(10) NOT NULL,
    "UpdatedAt" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE agroconnect."IdempotencyRecords"(
    "Id" UUID PRIMARY KEY,
    "UserId" UUID NOT NULL REFERENCES agroconnect."Users"("Id"),
    "Key" VARCHAR(200) NOT NULL,
    "RequestHash" VARCHAR(128) NOT NULL,
    "StatusCode" INTEGER NOT NULL,
    "Location" VARCHAR(1000) NOT NULL,
    "ResponseJson" VARCHAR(20000) NOT NULL,
    "CreatedAt" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE agroconnect."RecommendationReadings"(
    "RecommendationId" UUID NOT NULL REFERENCES agroconnect."Recommendations"("Id") ON DELETE CASCADE,
    "ReadingId" UUID NOT NULL REFERENCES agroconnect."Readings"("Id") ON DELETE CASCADE,
    PRIMARY KEY("RecommendationId","ReadingId")
);

CREATE TABLE agroconnect."AlertReadings"(
    "AlertId" UUID NOT NULL REFERENCES agroconnect."Alerts"("Id") ON DELETE CASCADE,
    "ReadingId" UUID NOT NULL REFERENCES agroconnect."Readings"("Id") ON DELETE CASCADE,
    PRIMARY KEY("AlertId","ReadingId")
);

ALTER TABLE agroconnect."Plantings" ADD COLUMN "CropId" UUID REFERENCES agroconnect."Crops"("Id") ON DELETE SET NULL;
ALTER TABLE agroconnect."Plantings" ADD COLUMN "Variety" VARCHAR(100);
ALTER TABLE agroconnect."Plantings" ADD COLUMN "Status" VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL;
ALTER TABLE agroconnect."Plantings" ADD COLUMN "Observations" VARCHAR(1000);

CREATE UNIQUE INDEX ux_devices_code ON agroconnect."Devices"("Code");
CREATE UNIQUE INDEX ux_sensors_code ON agroconnect."Sensors"("Code");
CREATE UNIQUE INDEX ux_crops_owner_code ON agroconnect."Crops"("UserId", "Code");
CREATE UNIQUE INDEX ux_push_token ON agroconnect."PushTokens"("Token");
CREATE UNIQUE INDEX ux_idempotency_key ON agroconnect."IdempotencyRecords"("UserId", "Key");
CREATE UNIQUE INDEX ux_recommendation_alert ON agroconnect."Recommendations"("SourceAlertId");
CREATE INDEX ix_devices_field ON agroconnect."Devices"("FieldId");
CREATE INDEX ix_sensors_device ON agroconnect."Sensors"("DeviceId");
CREATE INDEX ix_readings_sensor_time ON agroconnect."Readings"("SensorId","RecordedAt","Id");
CREATE INDEX ix_alerts_device ON agroconnect."Alerts"("DeviceId","Status","CreatedAt");
CREATE INDEX ix_recommendations_device ON agroconnect."Recommendations"("DeviceId");
CREATE INDEX ix_events_device ON agroconnect."DeviceEvents"("DeviceId","OccurredAt");
CREATE INDEX ix_weather_property ON agroconnect."WeatherHistory"("PropertyId","ObservedAt");
CREATE INDEX ix_ai_user ON agroconnect."AiInteractions"("UserId","CreatedAt");
CREATE INDEX ix_audit_owner ON agroconnect."AlertAudits"("UserId","OccurredAt");

