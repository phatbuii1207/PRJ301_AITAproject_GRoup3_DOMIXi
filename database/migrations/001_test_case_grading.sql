-- Run against the application's SQL Server / Azure SQL database. Additive and rerunnable.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID('dbo.GRADING_CONFIGS', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.GRADING_CONFIGS (
        assignment_id int NOT NULL PRIMARY KEY,
        main_class nvarchar(200) NOT NULL,
        config_version int NOT NULL,
        CONSTRAINT FK_GRADING_CONFIG_ASSIGNMENT FOREIGN KEY (assignment_id)
            REFERENCES dbo.ASSIGNMENTS(assignment_id) ON DELETE CASCADE
    );
END;
IF OBJECT_ID('dbo.GRADING_TEST_CASES', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.GRADING_TEST_CASES (
        assignment_id int NOT NULL,
        case_order int NOT NULL,
        case_name nvarchar(80) NOT NULL,
        stdin_text nvarchar(max) NOT NULL,
        expected_output nvarchar(max) NOT NULL,
        weight decimal(8,2) NOT NULL CHECK (weight > 0 AND weight <= 1000),
        is_hidden bit NOT NULL,
        PRIMARY KEY (assignment_id, case_order),
        CONSTRAINT FK_GRADING_TEST_CONFIG FOREIGN KEY (assignment_id)
            REFERENCES dbo.GRADING_CONFIGS(assignment_id) ON DELETE CASCADE
    );
END;
IF OBJECT_ID('dbo.GRADING_REPORTS', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.GRADING_REPORTS (
        submission_id int NOT NULL PRIMARY KEY,
        main_class nvarchar(200) NOT NULL,
        config_version int NOT NULL,
        verdict varchar(32) NOT NULL,
        score decimal(4,2) NULL CHECK (score >= 0 AND score <= 10),
        message nvarchar(500) NOT NULL,
        graded_at datetime2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_GRADING_REPORT_SUBMISSION FOREIGN KEY (submission_id)
            REFERENCES dbo.SUBMISSIONS(submission_id) ON DELETE CASCADE
    );
END;
IF OBJECT_ID('dbo.GRADING_CASE_RESULTS', 'U') IS NULL
BEGIN
    -- Snapshot: changing/deleting the current test bank must not rewrite past results.
    CREATE TABLE dbo.GRADING_CASE_RESULTS (
        submission_id int NOT NULL,
        case_order int NOT NULL,
        case_name nvarchar(80) NOT NULL,
        stdin_text nvarchar(max) NOT NULL,
        expected_output nvarchar(max) NOT NULL,
        weight decimal(8,2) NOT NULL,
        is_hidden bit NOT NULL,
        verdict varchar(32) NOT NULL,
        actual_output nvarchar(max) NOT NULL,
        diagnostics nvarchar(max) NOT NULL,
        execution_time_ms bigint NOT NULL,
        PRIMARY KEY (submission_id, case_order),
        CONSTRAINT FK_GRADING_CASE_REPORT FOREIGN KEY (submission_id)
            REFERENCES dbo.GRADING_REPORTS(submission_id) ON DELETE CASCADE
    );
END;
COMMIT TRANSACTION;
