ALTER TABLE agroconnect."Users" ADD COLUMN "Role" VARCHAR(10) DEFAULT 'USER' NOT NULL;
ALTER TABLE agroconnect."Users" ADD COLUMN "Active" BOOLEAN DEFAULT TRUE NOT NULL;
ALTER TABLE agroconnect."Users" ADD CONSTRAINT ck_users_role CHECK("Role" IN('USER','ADMIN'));
CREATE INDEX ix_sessions_user_expiry ON agroconnect."Sessions"("UserId", "ExpiresAt");

