-- Store only instructor-provided Java driver source. Existing ZIP configurations remain valid.
BEGIN;
ALTER TABLE "GRADING_CONFIGS" ADD COLUMN IF NOT EXISTS driver_source text NOT NULL DEFAULT '';
COMMIT;
