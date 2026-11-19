SET SERVEROUTPUT ON
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE

WHENEVER SQLERROR CONTINUE
DROP TRIGGER auditar_alertas;
DROP TRIGGER definir_proprietario_alerta;
DROP TRIGGER validar_leitura_sensor;
DROP PACKAGE pacote_alertas;
DROP PACKAGE pacote_monitoramento;

DROP TABLE AlertReadings CASCADE CONSTRAINTS;
DROP TABLE RecommendationReadings CASCADE CONSTRAINTS;
DROP TABLE IdempotencyRecords CASCADE CONSTRAINTS;
DROP TABLE PushTokens CASCADE CONSTRAINTS;
DROP TABLE Sessions CASCADE CONSTRAINTS;
DROP TABLE AlertAudits CASCADE CONSTRAINTS;
DROP TABLE WeatherHistory CASCADE CONSTRAINTS;
DROP TABLE AiInteractions CASCADE CONSTRAINTS;
DROP TABLE DeviceEvents CASCADE CONSTRAINTS;
DROP TABLE Crops CASCADE CONSTRAINTS;
DROP TABLE Recommendations CASCADE CONSTRAINTS;
DROP TABLE Alerts CASCADE CONSTRAINTS;
DROP TABLE Readings CASCADE CONSTRAINTS;
DROP TABLE Sensors CASCADE CONSTRAINTS;
DROP TABLE Devices CASCADE CONSTRAINTS;
DROP TABLE Plantings CASCADE CONSTRAINTS;
DROP TABLE Fields CASCADE CONSTRAINTS;
DROP TABLE Properties CASCADE CONSTRAINTS;
DROP TABLE Users CASCADE CONSTRAINTS;
WHENEVER SQLERROR EXIT SQL.SQLCODE

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

ALTER TABLE Plantings ADD CropId RAW(16) REFERENCES Crops(Id) ON DELETE SET NULL;
ALTER TABLE Plantings ADD Variety VARCHAR2(100 CHAR);
ALTER TABLE Plantings ADD Status VARCHAR2(20 CHAR) DEFAULT 'ACTIVE' NOT NULL;
ALTER TABLE Plantings ADD Observations VARCHAR2(1000 CHAR);

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

CREATE OR REPLACE PACKAGE pacote_monitoramento AS
    FUNCTION uuid_texto(p_id RAW) RETURN VARCHAR2 DETERMINISTIC;
    FUNCTION consultar_conectividade(
        p_dispositivo_id RAW,
        p_referencia TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP
    ) RETURN VARCHAR2;
    PROCEDURE registrar_leitura(
        p_sensor_id RAW,
        p_valor NUMBER,
        p_qualidade VARCHAR2,
        p_registrado_em TIMESTAMP WITH TIME ZONE,
        p_recebido_em TIMESTAMP WITH TIME ZONE,
        p_origem VARCHAR2,
        p_simulado NUMBER,
        p_leitura_id OUT RAW
    );
END pacote_monitoramento;
/

CREATE OR REPLACE PACKAGE BODY pacote_monitoramento AS
    FUNCTION uuid_texto(p_id RAW) RETURN VARCHAR2 DETERMINISTIC IS
        v_hex VARCHAR2(32) := LOWER(RAWTOHEX(p_id));
    BEGIN
        IF p_id IS NULL THEN RETURN NULL; END IF;
        RETURN SUBSTR(v_hex,1,8) || '-' || SUBSTR(v_hex,9,4) || '-' ||
            SUBSTR(v_hex,13,4) || '-' || SUBSTR(v_hex,17,4) || '-' || SUBSTR(v_hex,21,12);
    END uuid_texto;

    FUNCTION consultar_conectividade(
        p_dispositivo_id RAW,
        p_referencia TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP
    ) RETURN VARCHAR2 IS
        v_ultima_comunicacao TIMESTAMP WITH TIME ZONE;
    BEGIN
        IF p_referencia IS NULL THEN
            RAISE_APPLICATION_ERROR(-20001,'Informe a data de referencia.');
        END IF;
        SELECT LastSeenAt INTO v_ultima_comunicacao FROM Devices WHERE Id = p_dispositivo_id;
        IF v_ultima_comunicacao IS NULL OR v_ultima_comunicacao > p_referencia THEN
            RETURN 'UNKNOWN';
        ELSIF v_ultima_comunicacao >= p_referencia - INTERVAL '5' MINUTE THEN
            RETURN 'ONLINE';
        ELSIF v_ultima_comunicacao >= p_referencia - INTERVAL '30' MINUTE THEN
            RETURN 'UNSTABLE';
        ELSE
            RETURN 'OFFLINE';
        END IF;
    EXCEPTION WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20002,'Dispositivo nao encontrado.');
    END consultar_conectividade;

    PROCEDURE registrar_leitura(
        p_sensor_id RAW,
        p_valor NUMBER,
        p_qualidade VARCHAR2,
        p_registrado_em TIMESTAMP WITH TIME ZONE,
        p_recebido_em TIMESTAMP WITH TIME ZONE,
        p_origem VARCHAR2,
        p_simulado NUMBER,
        p_leitura_id OUT RAW
    ) IS
        v_dispositivo_id RAW(16);
    BEGIN
        SELECT DeviceId INTO v_dispositivo_id FROM Sensors WHERE Id = p_sensor_id;
        p_leitura_id := SYS_GUID();
        INSERT INTO Readings(Id,SensorId,Value,Quality,RecordedAt,ReceivedAt,SimulatedData,Origin)
        VALUES(p_leitura_id,p_sensor_id,p_valor,p_qualidade,p_registrado_em,p_recebido_em,p_simulado,p_origem);
        IF p_origem IN('SIMULATED','REAL') THEN
            UPDATE Devices SET LastSeenAt = CASE
                WHEN LastSeenAt IS NULL OR LastSeenAt < p_recebido_em
                THEN p_recebido_em ELSE LastSeenAt END
            WHERE Id = v_dispositivo_id;
        END IF;
    EXCEPTION WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20003,'Sensor nao encontrado.');
    END registrar_leitura;
END pacote_monitoramento;
/

CREATE OR REPLACE PACKAGE pacote_alertas AS
    FUNCTION classificar_umidade(p_valor NUMBER,p_qualidade VARCHAR2,p_limite NUMBER) RETURN VARCHAR2;
    PROCEDURE gerar_alerta(p_leitura_id RAW);
END pacote_alertas;
/

CREATE OR REPLACE PACKAGE BODY pacote_alertas AS
    FUNCTION classificar_umidade(p_valor NUMBER,p_qualidade VARCHAR2,p_limite NUMBER) RETURN VARCHAR2 IS
    BEGIN
        IF p_limite IS NULL OR p_limite < 0 OR p_limite > 100 THEN
            RAISE_APPLICATION_ERROR(-20004,'Limite de umidade invalido.');
        END IF;
        IF p_qualidade IS NULL OR p_qualidade <> 'VALID' OR p_valor IS NULL OR p_valor < 0 OR p_valor > 100 THEN
            RETURN 'INCONCLUSIVE';
        ELSIF p_valor < p_limite THEN
            RETURN 'LOW';
        ELSE
            RETURN 'ADEQUATE';
        END IF;
    END classificar_umidade;

    PROCEDURE gerar_alerta(p_leitura_id RAW) IS
        v_leitura Readings%ROWTYPE;
        v_dispositivo_id RAW(16);
        v_tipo Sensors.Type%TYPE;
        v_unidade Sensors.Unit%TYPE;
        v_limite NUMBER;
        v_quantidade NUMBER;
        v_alerta_id RAW(16);
        v_titulo VARCHAR2(120);
        v_gravidade VARCHAR2(20);
        v_motivo VARCHAR2(500);
        v_evidencias CLOB;
    BEGIN
        SELECT * INTO v_leitura FROM Readings WHERE Id = p_leitura_id;
        SELECT DeviceId,Type,Unit INTO v_dispositivo_id,v_tipo,v_unidade
        FROM Sensors WHERE Id = v_leitura.SensorId;
        SELECT SoilMoistureThreshold INTO v_limite FROM Devices WHERE Id = v_dispositivo_id FOR UPDATE;
        SELECT COUNT(*) INTO v_quantidade FROM AlertReadings ar
        JOIN Alerts a ON a.Id = ar.AlertId WHERE ar.ReadingId = p_leitura_id AND a.DeviceId = v_dispositivo_id;
        IF v_quantidade > 0 THEN RETURN; END IF;
        IF v_leitura.Quality = 'SENSOR_ERROR' THEN
            v_titulo := 'Falha na medicao do sensor';
            v_gravidade := 'INFO';
            v_motivo := 'Leitura sem valor, identificada como SENSOR_ERROR.';
        ELSIF v_tipo = 'SOIL_MOISTURE'
            AND classificar_umidade(v_leitura.Value,v_leitura.Quality,v_limite) = 'LOW' THEN
            v_titulo := 'Umidade do solo abaixo do limite configurado';
            v_gravidade := 'WARNING';
            v_motivo := 'Leitura valida abaixo do limite de demonstracao do dispositivo.';
        ELSE
            RETURN;
        END IF;
        SELECT JSON_ARRAY(JSON_OBJECT(
            'type' VALUE v_tipo,'value' VALUE v_leitura.Value,'unit' VALUE v_unidade,
            'recordedAt' VALUE TO_CHAR(SYS_EXTRACT_UTC(v_leitura.RecordedAt),'YYYY-MM-DD"T"HH24:MI:SS.FF9"Z"')
            NULL ON NULL RETURNING CLOB) RETURNING CLOB)
        INTO v_evidencias FROM dual;
        v_alerta_id := SYS_GUID();
        INSERT INTO Alerts(Id,DeviceId,Severity,Status,Title,Message,Reason,PushPending,BasedOnJson,CreatedAt)
        VALUES(v_alerta_id,v_dispositivo_id,v_gravidade,'UNREAD',v_titulo,
            'Verifique as condicoes reais e a data da leitura antes de decidir.',
            v_motivo,0,v_evidencias,v_leitura.ReceivedAt);
        INSERT INTO AlertReadings(AlertId,ReadingId) VALUES(v_alerta_id,p_leitura_id);
    EXCEPTION WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20005,'Leitura ou dispositivo nao encontrado.');
    END gerar_alerta;
END pacote_alertas;
/

CREATE OR REPLACE TRIGGER validar_leitura_sensor
BEFORE INSERT OR UPDATE ON Readings
FOR EACH ROW
BEGIN
    IF :NEW.Quality IS NULL OR :NEW.Quality NOT IN('VALID','OUT_OF_RANGE','SENSOR_ERROR','UNCALIBRATED') THEN
        RAISE_APPLICATION_ERROR(-20006,'Qualidade invalida.');
    END IF;
    IF (:NEW.Quality = 'VALID' AND :NEW.Value IS NULL)
        OR (:NEW.Quality = 'SENSOR_ERROR' AND :NEW.Value IS NOT NULL) THEN
        RAISE_APPLICATION_ERROR(-20007,'Valor incompatível com a qualidade da leitura.');
    END IF;
    IF :NEW.RecordedAt IS NULL OR :NEW.ReceivedAt IS NULL OR :NEW.ReceivedAt < :NEW.RecordedAt THEN
        RAISE_APPLICATION_ERROR(-20008,'Datas da leitura invalidas.');
    END IF;
    IF :NEW.Origin IS NULL OR :NEW.Origin NOT IN('SIMULATED','REAL','ESTIMATED')
        OR (:NEW.Origin = 'SIMULATED' AND :NEW.SimulatedData <> 1)
        OR (:NEW.Origin = 'REAL' AND :NEW.SimulatedData <> 0) THEN
        RAISE_APPLICATION_ERROR(-20009,'Origem incompatível com o indicador de simulacao.');
    END IF;
END;
/

CREATE OR REPLACE TRIGGER definir_proprietario_alerta
BEFORE INSERT OR UPDATE ON Alerts
FOR EACH ROW
BEGIN
    SELECT p.UserId INTO :NEW.OwnerUserId FROM Devices d
    JOIN Fields f ON f.Id = d.FieldId JOIN Properties p ON p.Id = f.PropertyId
    WHERE d.Id = :NEW.DeviceId;
END;
/

CREATE OR REPLACE TRIGGER auditar_alertas
AFTER INSERT OR UPDATE OR DELETE ON Alerts
FOR EACH ROW
DECLARE
    v_operacao VARCHAR2(10);
    v_antigos CLOB;
    v_novos CLOB;
BEGIN
    IF INSERTING THEN v_operacao := 'INSERT';
    ELSIF UPDATING THEN v_operacao := 'UPDATE';
    ELSE v_operacao := 'DELETE'; END IF;
    IF UPDATING OR DELETING THEN
        SELECT JSON_OBJECT(
            'id' VALUE pacote_monitoramento.uuid_texto(:OLD.Id),
            'deviceId' VALUE pacote_monitoramento.uuid_texto(:OLD.DeviceId),
            'severity' VALUE :OLD.Severity,'status' VALUE :OLD.Status,
            'title' VALUE :OLD.Title,'message' VALUE :OLD.Message,'reason' VALUE :OLD.Reason,
            'basedOn' VALUE :OLD.BasedOnJson FORMAT JSON,
            'createdAt' VALUE TO_CHAR(SYS_EXTRACT_UTC(:OLD.CreatedAt),'YYYY-MM-DD"T"HH24:MI:SS.FF9"Z"') RETURNING CLOB
        ) INTO v_antigos FROM dual;
    END IF;
    IF INSERTING OR UPDATING THEN
        SELECT JSON_OBJECT(
            'id' VALUE pacote_monitoramento.uuid_texto(:NEW.Id),
            'deviceId' VALUE pacote_monitoramento.uuid_texto(:NEW.DeviceId),
            'severity' VALUE :NEW.Severity,'status' VALUE :NEW.Status,
            'title' VALUE :NEW.Title,'message' VALUE :NEW.Message,'reason' VALUE :NEW.Reason,
            'basedOn' VALUE :NEW.BasedOnJson FORMAT JSON,
            'createdAt' VALUE TO_CHAR(SYS_EXTRACT_UTC(:NEW.CreatedAt),'YYYY-MM-DD"T"HH24:MI:SS.FF9"Z"') RETURNING CLOB
        ) INTO v_novos FROM dual;
    END IF;
    INSERT INTO AlertAudits(Id,AlertId,UserId,Operation,OccurredAt,DatabaseUser,OldValues,NewValues)
    VALUES(SYS_GUID(),COALESCE(:NEW.Id,:OLD.Id),COALESCE(:NEW.OwnerUserId,:OLD.OwnerUserId),v_operacao,
        SYSTIMESTAMP,SYS_CONTEXT('USERENV','SESSION_USER'),v_antigos,v_novos);
END;
/

DECLARE
    v_erros NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_erros FROM user_errors
    WHERE name IN('PACOTE_MONITORAMENTO','PACOTE_ALERTAS','VALIDAR_LEITURA_SENSOR',
        'DEFINIR_PROPRIETARIO_ALERTA','AUDITAR_ALERTAS');
    IF v_erros > 0 THEN
        RAISE_APPLICATION_ERROR(-20010,'Ha erros de compilacao. Consulte USER_ERRORS.');
    END IF;
END;
/
SELECT name,type,line,position,text FROM user_errors
WHERE name IN('PACOTE_MONITORAMENTO','PACOTE_ALERTAS','VALIDAR_LEITURA_SENSOR',
    'DEFINIR_PROPRIETARIO_ALERTA','AUDITAR_ALERTAS') ORDER BY name,sequence;

INSERT INTO Users(Id,Name,Email,PasswordHash,CreatedAt,Role,Active)
VALUES(HEXTORAW('10000000000040008000000000000001'),'Produtor Demonstracao SQL',
    'produtor.sql@agroconnect.test','!SQL_ONLY_LOCKED!',SYSTIMESTAMP,'USER',0);
INSERT INTO Users(Id,Name,Email,PasswordHash,CreatedAt,Role,Active)
VALUES(HEXTORAW('10000000000040008000000000000002'),'Administrador Demonstracao SQL',
    'admin.sql@agroconnect.test','!SQL_ONLY_LOCKED!',SYSTIMESTAMP,'ADMIN',0);

INSERT INTO Properties(Id,UserId,Name,City,State,AreaHectares,Description,CreatedAt,UpdatedAt)
VALUES(HEXTORAW('20000000000040008000000000000001'),HEXTORAW('10000000000040008000000000000001'),
    'Sitio Boa Esperanca SQL','Carapicuiba','SP',0.500,'Propriedade ficticia para entrega de banco.',SYSTIMESTAMP,SYSTIMESTAMP);

INSERT INTO Fields(Id,PropertyId,Name,AreaHectares)
VALUES(HEXTORAW('30000000000040008000000000000001'),HEXTORAW('20000000000040008000000000000001'),'Canteiro 01',0.003);
INSERT INTO Fields(Id,PropertyId,Name,AreaHectares)
VALUES(HEXTORAW('30000000000040008000000000000002'),HEXTORAW('20000000000040008000000000000001'),'Canteiro 02',0.0024);

INSERT INTO Crops(Id,UserId,Code,Name,ScientificName,Description)
VALUES(HEXTORAW('40000000000040008000000000000001'),HEXTORAW('10000000000040008000000000000001'),
    'ALFACE','Alface','Lactuca sativa','Cultura utilizada na demonstracao.');

INSERT INTO Plantings(Id,FieldId,CropId,CropName,PlantedAt,Stage,Variety,Status)
VALUES(HEXTORAW('50000000000040008000000000000001'),HEXTORAW('30000000000040008000000000000001'),
    HEXTORAW('40000000000040008000000000000001'),'Alface',
    TO_TIMESTAMP_TZ('2026-10-20 00:00:00 +00:00','YYYY-MM-DD HH24:MI:SS TZH:TZM'),'VEGETATIVE','Crespa','ACTIVE');
INSERT INTO Plantings(Id,FieldId,CropId,CropName,PlantedAt,Stage,Variety,Status)
VALUES(HEXTORAW('50000000000040008000000000000002'),HEXTORAW('30000000000040008000000000000002'),
    HEXTORAW('40000000000040008000000000000001'),'Alface',
    TO_TIMESTAMP_TZ('2026-10-24 00:00:00 +00:00','YYYY-MM-DD HH24:MI:SS TZH:TZM'),'VEGETATIVE','Americana','ACTIVE');

INSERT INTO Devices(Id,FieldId,Code,Name,LastSeenAt,Rssi,FirmwareVersion,Transport,SimulatedData,SoilMoistureThreshold)
VALUES(HEXTORAW('60000000000040008000000000000001'),HEXTORAW('30000000000040008000000000000001'),
    'DB-ESP32-001','Estacao do Canteiro 01',NULL,-61,'1.0.0','WIFI',1,30);
INSERT INTO Devices(Id,FieldId,Code,Name,LastSeenAt,Rssi,FirmwareVersion,Transport,SimulatedData,SoilMoistureThreshold)
VALUES(HEXTORAW('60000000000040008000000000000002'),HEXTORAW('30000000000040008000000000000002'),
    'DB-ESP32-002','Estacao do Canteiro 02',NULL,NULL,'1.0.0','WIFI',1,30);

INSERT INTO Sensors(Id,DeviceId,Type,Label,Unit,Code,Calibrated)
VALUES(HEXTORAW('70000000000040008000000000000001'),HEXTORAW('60000000000040008000000000000001'),
    'SOIL_MOISTURE','Umidade do solo','%','DB-SOLO-001',1);
INSERT INTO Sensors(Id,DeviceId,Type,Label,Unit,Code,Calibrated)
VALUES(HEXTORAW('70000000000040008000000000000002'),HEXTORAW('60000000000040008000000000000001'),
    'AIR_TEMPERATURE','Temperatura do ar','°C','DB-AR-001',1);
INSERT INTO Sensors(Id,DeviceId,Type,Label,Unit,Code,Calibrated)
VALUES(HEXTORAW('70000000000040008000000000000003'),HEXTORAW('60000000000040008000000000000001'),
    'AIR_HUMIDITY','Umidade do ar','%','DB-AR-002',1);
INSERT INTO Sensors(Id,DeviceId,Type,Label,Unit,Code,Calibrated)
VALUES(HEXTORAW('70000000000040008000000000000004'),HEXTORAW('60000000000040008000000000000001'),
    'LIGHT_ILLUMINANCE','Iluminancia','lux','DB-LUZ-001',1);
INSERT INTO Sensors(Id,DeviceId,Type,Label,Unit,Code,Calibrated)
VALUES(HEXTORAW('70000000000040008000000000000005'),HEXTORAW('60000000000040008000000000000002'),
    'SOIL_MOISTURE','Umidade do solo indisponivel','%','DB-SOLO-002',0);

DECLARE
    v_id RAW(16);
    v_referencia TIMESTAMP WITH TIME ZONE :=
        TO_TIMESTAMP_TZ('2026-11-15 14:04:00 +00:00','YYYY-MM-DD HH24:MI:SS TZH:TZM');
BEGIN
    pacote_monitoramento.registrar_leitura(HEXTORAW('70000000000040008000000000000001'),35.2,'VALID',v_referencia - INTERVAL '3' HOUR,v_referencia - INTERVAL '3' HOUR,'SIMULATED',1,v_id);
    pacote_monitoramento.registrar_leitura(HEXTORAW('70000000000040008000000000000001'),29.5,'VALID',v_referencia - INTERVAL '2' HOUR,v_referencia - INTERVAL '2' HOUR,'SIMULATED',1,v_id);
    pacote_monitoramento.registrar_leitura(HEXTORAW('70000000000040008000000000000001'),24.5,'VALID',v_referencia - INTERVAL '1' HOUR,v_referencia - INTERVAL '1' HOUR,'SIMULATED',1,v_id);
    pacote_monitoramento.registrar_leitura(HEXTORAW('70000000000040008000000000000001'),23.8,'VALID',v_referencia,v_referencia,'SIMULATED',1,v_id);
    pacote_monitoramento.registrar_leitura(HEXTORAW('70000000000040008000000000000002'),25.1,'VALID',v_referencia - INTERVAL '1' HOUR,v_referencia - INTERVAL '1' HOUR,'SIMULATED',1,v_id);
    pacote_monitoramento.registrar_leitura(HEXTORAW('70000000000040008000000000000002'),27.3,'VALID',v_referencia,v_referencia,'SIMULATED',1,v_id);
    pacote_monitoramento.registrar_leitura(HEXTORAW('70000000000040008000000000000003'),65,'VALID',v_referencia - INTERVAL '1' HOUR,v_referencia - INTERVAL '1' HOUR,'SIMULATED',1,v_id);
    pacote_monitoramento.registrar_leitura(HEXTORAW('70000000000040008000000000000003'),63,'VALID',v_referencia,v_referencia,'SIMULATED',1,v_id);
    pacote_monitoramento.registrar_leitura(HEXTORAW('70000000000040008000000000000004'),11000,'VALID',v_referencia - INTERVAL '1' HOUR,v_referencia - INTERVAL '1' HOUR,'SIMULATED',1,v_id);
    pacote_monitoramento.registrar_leitura(HEXTORAW('70000000000040008000000000000004'),12500,'VALID',v_referencia,v_referencia,'SIMULATED',1,v_id);
    pacote_monitoramento.registrar_leitura(HEXTORAW('70000000000040008000000000000005'),NULL,'SENSOR_ERROR',v_referencia - INTERVAL '2' DAY,v_referencia - INTERVAL '2' DAY,'SIMULATED',1,v_id);
    pacote_monitoramento.registrar_leitura(HEXTORAW('70000000000040008000000000000005'),NULL,'UNCALIBRATED',v_referencia - INTERVAL '3' DAY,v_referencia - INTERVAL '3' DAY,'SIMULATED',1,v_id);
END;
/

BEGIN
    FOR registro IN(SELECT r.Id FROM Readings r JOIN Sensors s ON s.Id = r.SensorId
        JOIN Devices d ON d.Id = s.DeviceId WHERE d.Code IN('DB-ESP32-001','DB-ESP32-002')
        ORDER BY r.RecordedAt,r.Id) LOOP
        pacote_alertas.gerar_alerta(registro.Id);
    END LOOP;
END;
/

INSERT INTO DeviceEvents(Id,DeviceId,Type,Description,OccurredAt,Origin)
VALUES(SYS_GUID(),HEXTORAW('60000000000040008000000000000002'),'DISCONNECTED',
    'Dispositivo sem comunicacao recente; dados simulados.',
    TO_TIMESTAMP_TZ('2026-11-13 14:05:00 +00:00','YYYY-MM-DD HH24:MI:SS TZH:TZM'),'SIMULATED');

DECLARE
    v_id RAW(16) := SYS_GUID();
BEGIN
    FOR registro IN(SELECT Id,BasedOnJson FROM Alerts
        WHERE DeviceId = HEXTORAW('60000000000040008000000000000001') AND Severity = 'WARNING'
        ORDER BY CreatedAt DESC,Id FETCH FIRST 1 ROW ONLY) LOOP
        INSERT INTO Recommendations(Id,DeviceId,Priority,Title,Message,Reason,Disclaimer,
            SimulatedData,ModelName,BasedOnJson,GeneratedAt,AiGenerated)
        VALUES(v_id,HEXTORAW('60000000000040008000000000000001'),'WARNING','Avaliar a necessidade de irrigacao',
            'Confira o solo e o sensor antes de decidir.','Umidade simulada abaixo do limite demonstrativo.',
            'Exemplo manual com dados simulados; nao representa uma chamada real ao modelo.',
            1,'DEMONSTRACAO_MANUAL',registro.BasedOnJson,SYSTIMESTAMP,0);
        INSERT INTO RecommendationReadings(RecommendationId,ReadingId)
        SELECT v_id,ReadingId FROM AlertReadings WHERE AlertId = registro.Id;
    END LOOP;
END;
/

INSERT INTO AiInteractions(Id,UserId,PropertyId,DeviceId,Question,Response,Context,ModelName,CreatedAt)
VALUES(SYS_GUID(),HEXTORAW('10000000000040008000000000000001'),HEXTORAW('20000000000040008000000000000001'),
    HEXTORAW('60000000000040008000000000000001'),'Como interpretar a umidade do solo?',
    'Exemplo manual: conferir as condicoes reais.','{"simulatedData":true,"source":"SQL_DEMO"}',
    'DEMONSTRACAO_MANUAL',SYSTIMESTAMP);

INSERT INTO WeatherHistory(Id,PropertyId,ObservedAt,TemperatureC,RelativeHumidity,PrecipitationMm,WindSpeedKmh,Origin)
VALUES(SYS_GUID(),HEXTORAW('20000000000040008000000000000001'),
    TO_TIMESTAMP_TZ('2026-11-15 14:00:00 +00:00','YYYY-MM-DD HH24:MI:SS TZH:TZM'),27.3,63,0,8.5,'SIMULATED');

DECLARE
    v_situacao VARCHAR2(20);
BEGIN
    FOR registro IN(SELECT Id,Code FROM Devices WHERE Code IN('DB-ESP32-001','DB-ESP32-002') ORDER BY Code) LOOP
        v_situacao := pacote_monitoramento.consultar_conectividade(registro.Id,
            TO_TIMESTAMP_TZ('2026-11-15 14:05:00 +00:00','YYYY-MM-DD HH24:MI:SS TZH:TZM'));
        IF v_situacao = 'OFFLINE' THEN
            DBMS_OUTPUT.PUT_LINE(registro.Code || ': OFFLINE - verificar conexao.');
        ELSE
            DBMS_OUTPUT.PUT_LINE(registro.Code || ': ' || v_situacao);
        END IF;
    END LOOP;
END;
/
COMMIT;

