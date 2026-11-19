-- ===== TABELAS DO CONTRATO MOBILE / ORACLE 19C OU SUPERIOR =====

CREATE TABLE Users(
    Id RAW(16) PRIMARY KEY,
    Name VARCHAR2(150 CHAR) NOT NULL,
    Email VARCHAR2(254 CHAR) NOT NULL UNIQUE,
    PasswordHash VARCHAR2(255 CHAR) NOT NULL,
    CreatedAt TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    Role VARCHAR2(10 CHAR) DEFAULT 'USER' NOT NULL CHECK(Role IN('USER','ADMIN')),
    Active NUMBER(1) DEFAULT 1 NOT NULL CHECK(Active IN(0,1))
);

CREATE TABLE Properties(
    Id RAW(16) PRIMARY KEY,
    UserId RAW(16) NOT NULL REFERENCES Users(Id),
    Name VARCHAR2(150 CHAR) NOT NULL,
    City VARCHAR2(120 CHAR) NOT NULL,
    State VARCHAR2(2 CHAR) NOT NULL,
    AreaHectares NUMBER NOT NULL CHECK(AreaHectares > 0),
    Description CLOB,
    CreatedAt TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    UpdatedAt TIMESTAMP(9) WITH TIME ZONE NOT NULL
);

CREATE TABLE Fields(
    Id RAW(16) PRIMARY KEY,
    PropertyId RAW(16) NOT NULL REFERENCES Properties(Id) ON DELETE CASCADE,
    Name VARCHAR2(150 CHAR) NOT NULL,
    AreaHectares NUMBER NOT NULL CHECK(AreaHectares > 0)
);

CREATE TABLE Plantings(
    Id RAW(16) PRIMARY KEY,
    FieldId RAW(16) NOT NULL REFERENCES Fields(Id) ON DELETE CASCADE,
    CropName VARCHAR2(150 CHAR) NOT NULL,
    PlantedAt TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    Stage VARCHAR2(20 CHAR) NOT NULL CHECK(Stage IN('SEEDLING','VEGETATIVE','MATURATION','HARVEST'))
);

CREATE TABLE Devices(
    Id RAW(16) PRIMARY KEY,
    FieldId RAW(16) NOT NULL REFERENCES Fields(Id) ON DELETE CASCADE,
    Code VARCHAR2(60 CHAR) NOT NULL,
    Name VARCHAR2(150 CHAR) NOT NULL,
    LastSeenAt TIMESTAMP(9) WITH TIME ZONE,
    Rssi NUMBER(10),
    FirmwareVersion VARCHAR2(40 CHAR) NOT NULL,
    Transport VARCHAR2(20 CHAR) NOT NULL,
    SimulatedData NUMBER(1) NOT NULL CHECK(SimulatedData IN(0,1)),
    SoilMoistureThreshold NUMBER(20,6) NOT NULL
);

CREATE TABLE Sensors(
    Id RAW(16) PRIMARY KEY,
    DeviceId RAW(16) NOT NULL REFERENCES Devices(Id) ON DELETE CASCADE,
    Type VARCHAR2(50 CHAR) NOT NULL,
    Label VARCHAR2(150 CHAR) NOT NULL,
    Unit VARCHAR2(20 CHAR) NOT NULL,
    Code VARCHAR2(255 CHAR),
    Calibrated NUMBER(1) NOT NULL CHECK(Calibrated IN(0,1)),
    InstalledAt TIMESTAMP(9) WITH TIME ZONE
);

CREATE TABLE Readings(
    Id RAW(16) PRIMARY KEY,
    SensorId RAW(16) NOT NULL REFERENCES Sensors(Id) ON DELETE CASCADE,
    Value NUMBER(12,3),
    Quality VARCHAR2(20 CHAR) NOT NULL,
    RecordedAt TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    ReceivedAt TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    SimulatedData NUMBER(1) NOT NULL CHECK(SimulatedData IN(0,1)),
    Origin VARCHAR2(20 CHAR) NOT NULL,
    Observations VARCHAR2(255 CHAR)
);

CREATE TABLE Alerts(
    Id RAW(16) PRIMARY KEY,
    DeviceId RAW(16) NOT NULL REFERENCES Devices(Id) ON DELETE CASCADE,
    Severity VARCHAR2(20 CHAR) NOT NULL,
    Status VARCHAR2(20 CHAR) NOT NULL,
    Title VARCHAR2(120 CHAR) NOT NULL,
    Message VARCHAR2(500 CHAR) NOT NULL,
    Reason VARCHAR2(500 CHAR) NOT NULL,
    PushPending NUMBER(1) NOT NULL CHECK(PushPending IN(0,1)),
    BasedOnJson CLOB NOT NULL,
    OwnerUserId RAW(16),
    CreatedAt TIMESTAMP(9) WITH TIME ZONE NOT NULL
);

CREATE TABLE Recommendations(
    Id RAW(16) PRIMARY KEY,
    DeviceId RAW(16) NOT NULL REFERENCES Devices(Id) ON DELETE CASCADE,
    SourceAlertId RAW(16),
    Priority VARCHAR2(20 CHAR) NOT NULL,
    Title VARCHAR2(180 CHAR) NOT NULL,
    Message CLOB NOT NULL,
    Reason CLOB NOT NULL,
    Disclaimer VARCHAR2(1000 CHAR) NOT NULL,
    SimulatedData NUMBER(1) NOT NULL CHECK(SimulatedData IN(0,1)),
    ModelName VARCHAR2(255 CHAR),
    BasedOnJson CLOB NOT NULL,
    GeneratedAt TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    AiGenerated NUMBER(1) DEFAULT 0 NOT NULL CHECK(AiGenerated IN(0,1)),
    InputFingerprint VARCHAR2(64 CHAR)
);

CREATE TABLE Crops(
    Id RAW(16) PRIMARY KEY,
    UserId RAW(16) NOT NULL REFERENCES Users(Id),
    Code VARCHAR2(40 CHAR) NOT NULL,
    Name VARCHAR2(100 CHAR) NOT NULL,
    ScientificName VARCHAR2(255 CHAR),
    Description VARCHAR2(255 CHAR)
);

CREATE TABLE DeviceEvents(
    Id RAW(16) PRIMARY KEY,
    DeviceId RAW(16) NOT NULL REFERENCES Devices(Id) ON DELETE CASCADE,
    Type VARCHAR2(30 CHAR) NOT NULL,
    Description VARCHAR2(1000 CHAR) NOT NULL,
    OccurredAt TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    Origin VARCHAR2(20 CHAR) NOT NULL
);

CREATE TABLE AiInteractions(
    Id RAW(16) PRIMARY KEY,
    UserId RAW(16) NOT NULL REFERENCES Users(Id),
    PropertyId RAW(16) REFERENCES Properties(Id) ON DELETE SET NULL,
    DeviceId RAW(16) REFERENCES Devices(Id) ON DELETE SET NULL,
    Question CLOB NOT NULL,
    Response CLOB,
    Context CLOB,
    ModelName VARCHAR2(255 CHAR),
    CreatedAt TIMESTAMP(9) WITH TIME ZONE NOT NULL
);

CREATE TABLE WeatherHistory(
    Id RAW(16) PRIMARY KEY,
    PropertyId RAW(16) NOT NULL REFERENCES Properties(Id) ON DELETE CASCADE,
    ObservedAt TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    TemperatureC NUMBER(20,6),
    RelativeHumidity NUMBER(20,6),
    PrecipitationMm NUMBER(20,6),
    WindSpeedKmh NUMBER(20,6),
    Origin VARCHAR2(20 CHAR) NOT NULL
);

CREATE TABLE AlertAudits(
    Id RAW(16) PRIMARY KEY,
    AlertId RAW(16) NOT NULL,
    UserId RAW(16) NOT NULL,
    Operation VARCHAR2(10 CHAR) NOT NULL,
    OccurredAt TIMESTAMP(9) WITH TIME ZONE NOT NULL,
    DatabaseUser VARCHAR2(254 CHAR) NOT NULL,
    OldValues CLOB,
    NewValues CLOB
);

CREATE TABLE Sessions(
    Id RAW(16) PRIMARY KEY,
    UserId RAW(16) NOT NULL REFERENCES Users(Id) ON DELETE CASCADE,
    ExpiresAt TIMESTAMP(9) WITH TIME ZONE NOT NULL
);

CREATE TABLE PushTokens(
    Id RAW(16) PRIMARY KEY,
    UserId RAW(16) NOT NULL REFERENCES Users(Id),
    SessionId RAW(16) NOT NULL REFERENCES Sessions(Id),
    Token VARCHAR2(512 CHAR) NOT NULL,
    Platform VARCHAR2(10 CHAR) NOT NULL,
    UpdatedAt TIMESTAMP(9) WITH TIME ZONE NOT NULL
);

CREATE TABLE IdempotencyRecords(
    Id RAW(16) PRIMARY KEY,
    UserId RAW(16) NOT NULL REFERENCES Users(Id),
    Key VARCHAR2(200 CHAR) NOT NULL,
    RequestHash VARCHAR2(128 CHAR) NOT NULL,
    StatusCode NUMBER(10) NOT NULL,
    Location VARCHAR2(1000 CHAR) NOT NULL,
    ResponseJson CLOB NOT NULL,
    CreatedAt TIMESTAMP(9) WITH TIME ZONE NOT NULL
);

CREATE TABLE RecommendationReadings(
    RecommendationId RAW(16) NOT NULL REFERENCES Recommendations(Id) ON DELETE CASCADE,
    ReadingId RAW(16) NOT NULL REFERENCES Readings(Id) ON DELETE CASCADE,
    PRIMARY KEY(RecommendationId,ReadingId)
);

CREATE TABLE AlertReadings(
    AlertId RAW(16) NOT NULL REFERENCES Alerts(Id) ON DELETE CASCADE,
    ReadingId RAW(16) NOT NULL REFERENCES Readings(Id) ON DELETE CASCADE,
    PRIMARY KEY(AlertId,ReadingId)
);

-- ===== COMPLEMENTOS DOS PLANTIOS =====

ALTER TABLE Plantings ADD CropId RAW(16) REFERENCES Crops(Id) ON DELETE SET NULL;
ALTER TABLE Plantings ADD Variety VARCHAR2(100 CHAR);
ALTER TABLE Plantings ADD Status VARCHAR2(20 CHAR) DEFAULT 'ACTIVE' NOT NULL;
ALTER TABLE Plantings ADD Observations VARCHAR2(1000 CHAR);

-- ===== INDICES =====

CREATE INDEX idx_properties_user ON Properties(UserId, Name, Id);
CREATE INDEX idx_fields_property ON Fields(PropertyId, Name, Id);
CREATE INDEX idx_plantings_field ON Plantings(FieldId, PlantedAt, Id);
CREATE UNIQUE INDEX ux_devices_code ON Devices(Code);
CREATE UNIQUE INDEX ux_sensors_code ON Sensors(Code);
CREATE UNIQUE INDEX ux_crops_owner_code ON Crops(UserId, Code);
CREATE UNIQUE INDEX ux_push_token ON PushTokens(Token);
CREATE UNIQUE INDEX ux_idempotency_key ON IdempotencyRecords(UserId, Key);
CREATE UNIQUE INDEX ux_recommendation_alert ON Recommendations(SourceAlertId);
CREATE INDEX ix_devices_field ON Devices(FieldId);
CREATE INDEX ix_sensors_device ON Sensors(DeviceId);
CREATE INDEX ix_readings_sensor_time ON Readings(SensorId,RecordedAt,Id);
CREATE INDEX ix_alerts_device ON Alerts(DeviceId,Status,CreatedAt);
CREATE INDEX ix_recommendations_device ON Recommendations(DeviceId);
CREATE INDEX ix_events_device ON DeviceEvents(DeviceId,OccurredAt);
CREATE INDEX ix_weather_property ON WeatherHistory(PropertyId,ObservedAt);
CREATE INDEX ix_ai_user ON AiInteractions(UserId,CreatedAt);
CREATE INDEX ix_audit_owner ON AlertAudits(UserId,OccurredAt);
CREATE INDEX ix_sessions_user_expiry ON Sessions(UserId, ExpiresAt);
