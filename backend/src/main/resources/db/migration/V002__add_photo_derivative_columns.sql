ALTER TABLE photos
    ADD COLUMN IF NOT EXISTS thumbnail_fallback_url VARCHAR(1024);

ALTER TABLE photos
    ADD COLUMN IF NOT EXISTS placeholder_url VARCHAR(1024);

ALTER TABLE photos
    ADD COLUMN IF NOT EXISTS placeholder_fallback_url VARCHAR(1024);

ALTER TABLE photos
    ADD COLUMN IF NOT EXISTS placeholder_base64 TEXT;

ALTER TABLE photos
    ADD COLUMN IF NOT EXISTS thumbnail_variants JSONB;
