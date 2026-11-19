ALTER TABLE agroconnect."Alerts" ALTER COLUMN "BasedOnJson" CLOB NOT NULL;
ALTER TABLE agroconnect."Recommendations" ALTER COLUMN "BasedOnJson" CLOB NOT NULL;
ALTER TABLE agroconnect."AlertAudits" ALTER COLUMN "OldValues" CLOB;
ALTER TABLE agroconnect."AlertAudits" ALTER COLUMN "NewValues" CLOB;
ALTER TABLE agroconnect."IdempotencyRecords" ALTER COLUMN "ResponseJson" CLOB NOT NULL;
ALTER TABLE agroconnect."Properties" ALTER COLUMN "Description" CLOB;
ALTER TABLE agroconnect."Recommendations" ALTER COLUMN "Message" CLOB NOT NULL;
ALTER TABLE agroconnect."Recommendations" ALTER COLUMN "Reason" CLOB NOT NULL;
