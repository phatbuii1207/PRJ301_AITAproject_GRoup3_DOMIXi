-- Compatibility with the team's AI persistence update; safe for existing databases.
IF COL_LENGTH('dbo.AI_OPTIMIZATIONS', 'new_complexity') IS NULL
BEGIN
    ALTER TABLE dbo.AI_OPTIMIZATIONS ADD new_complexity varchar(20) NULL;
END;
