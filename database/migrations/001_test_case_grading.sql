-- PostgreSQL additive grading extension. Base schema must exist.
BEGIN;
CREATE TABLE IF NOT EXISTS "GRADING_CONFIGS" (
        assignment_id int NOT NULL PRIMARY KEY,
        main_class varchar(200) NOT NULL,
        config_version int NOT NULL,
        CONSTRAINT FK_GRADING_CONFIG_ASSIGNMENT FOREIGN KEY (assignment_id)
            REFERENCES "ASSIGNMENTS"(assignment_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "GRADING_TEST_CASES" (
        assignment_id int NOT NULL,
        case_order int NOT NULL,
        case_name varchar(80) NOT NULL,
        stdin_text text NOT NULL,
        expected_output text NOT NULL,
        weight decimal(8,2) NOT NULL CHECK (weight > 0 AND weight <= 1000),
        is_hidden boolean NOT NULL,
        PRIMARY KEY (assignment_id, case_order),
        CONSTRAINT FK_GRADING_TEST_CONFIG FOREIGN KEY (assignment_id)
            REFERENCES "GRADING_CONFIGS"(assignment_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "GRADING_REPORTS" (
        submission_id int NOT NULL PRIMARY KEY,
        main_class varchar(200) NOT NULL,
        config_version int NOT NULL,
        verdict varchar(32) NOT NULL,
        score decimal(4,2) NULL CHECK (score >= 0 AND score <= 10),
        message varchar(500) NOT NULL,
        graded_at timestamp without time zone NOT NULL DEFAULT timezone('UTC', CURRENT_TIMESTAMP),
        CONSTRAINT FK_GRADING_REPORT_SUBMISSION FOREIGN KEY (submission_id)
            REFERENCES "SUBMISSIONS"(submission_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "GRADING_CASE_RESULTS" (
        submission_id int NOT NULL,
        case_order int NOT NULL,
        case_name varchar(80) NOT NULL,
        stdin_text text NOT NULL,
        expected_output text NOT NULL,
        weight decimal(8,2) NOT NULL,
        is_hidden boolean NOT NULL,
        verdict varchar(32) NOT NULL,
        actual_output text NOT NULL,
        diagnostics text NOT NULL,
        execution_time_ms bigint NOT NULL,
        PRIMARY KEY (submission_id, case_order),
        CONSTRAINT FK_GRADING_CASE_REPORT FOREIGN KEY (submission_id)
            REFERENCES "GRADING_REPORTS"(submission_id) ON DELETE CASCADE
);
COMMIT;
