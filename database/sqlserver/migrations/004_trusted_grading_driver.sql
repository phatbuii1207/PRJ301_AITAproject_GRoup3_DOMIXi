IF COL_LENGTH('dbo.GRADING_CONFIGS', 'driver_source') IS NULL
BEGIN
    ALTER TABLE dbo.GRADING_CONFIGS ADD driver_source nvarchar(max) NOT NULL
        CONSTRAINT DF_GRADING_CONFIGS_driver_source DEFAULT N'';
END;
