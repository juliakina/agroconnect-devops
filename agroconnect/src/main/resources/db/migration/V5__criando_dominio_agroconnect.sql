-- ===== DOMINIO AGROCONNECT / CONTRATO MOBILE =====
-- Migração H2 do projeto do professor. Não executar no Oracle.
CREATE SCHEMA IF NOT EXISTS agroconnect;

CREATE TABLE agroconnect."Users"(
    "Id" UUID PRIMARY KEY,
    "Name" VARCHAR(150) NOT NULL,
    "Email" VARCHAR(180) NOT NULL UNIQUE,
    "PasswordHash" VARCHAR(255) NOT NULL,
    "CreatedAt" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE agroconnect."Properties"(
    "Id" UUID PRIMARY KEY,
    "UserId" UUID NOT NULL REFERENCES agroconnect."Users"("Id"),
    "Name" VARCHAR(150) NOT NULL,
    "City" VARCHAR(120) NOT NULL,
    "State" VARCHAR(2) NOT NULL,
    "AreaHectares" NUMERIC(57,28) NOT NULL CHECK("AreaHectares" > 0),
    "Description" VARCHAR(2000),
    "CreatedAt" TIMESTAMP WITH TIME ZONE NOT NULL,
    "UpdatedAt" TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE agroconnect."Fields"(
    "Id" UUID PRIMARY KEY,
    "PropertyId" UUID NOT NULL REFERENCES agroconnect."Properties"("Id") ON DELETE CASCADE,
    "Name" VARCHAR(150) NOT NULL,
    "AreaHectares" NUMERIC(57,28) NOT NULL CHECK("AreaHectares" > 0)
);

CREATE TABLE agroconnect."Plantings"(
    "Id" UUID PRIMARY KEY,
    "FieldId" UUID NOT NULL REFERENCES agroconnect."Fields"("Id") ON DELETE CASCADE,
    "CropName" VARCHAR(150) NOT NULL,
    "PlantedAt" TIMESTAMP WITH TIME ZONE NOT NULL,
    "Stage" VARCHAR(20) NOT NULL CHECK("Stage" IN('SEEDLING','VEGETATIVE','MATURATION','HARVEST'))
);

CREATE INDEX idx_properties_user ON agroconnect."Properties"("UserId", "Name", "Id");
CREATE INDEX idx_fields_property ON agroconnect."Fields"("PropertyId", "Name", "Id");
CREATE INDEX idx_plantings_field ON agroconnect."Plantings"("FieldId", "PlantedAt", "Id");

