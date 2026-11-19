ALTER TABLE agroconnect."Recommendations" ADD COLUMN "AiGenerated" BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE agroconnect."Recommendations" ADD COLUMN "InputFingerprint" VARCHAR(64);
ALTER TABLE agroconnect."AiInteractions" ALTER COLUMN "Question" CLOB NOT NULL;
ALTER TABLE agroconnect."AiInteractions" ALTER COLUMN "Response" CLOB;
ALTER TABLE agroconnect."AiInteractions" ALTER COLUMN "Context" CLOB;

