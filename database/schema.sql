-- Applebay Institute
-- Creates the tables used by the Spring Boot backend.
-- Run this against the applebay database on the pgAdmin server:
--   psql -h localhost -p 5433 -U postgres -d applebay -f database/schema.sql

CREATE TABLE IF NOT EXISTS courses (
    id          BIGSERIAL PRIMARY KEY,
    course_code VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    level       VARCHAR(50)  NOT NULL,
    duration    VARCHAR(50)  NOT NULL
);

CREATE TABLE IF NOT EXISTS students (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(150) NOT NULL,
    nic     VARCHAR(20)  NOT NULL UNIQUE,
    email   VARCHAR(150) NOT NULL,
    phone   VARCHAR(20)  NOT NULL,
    address VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS enrolments (
    id          BIGSERIAL PRIMARY KEY,
    student_id  BIGINT NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    course_id   BIGINT NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    enrolled_on DATE   NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT uk_enrolment_student_course UNIQUE (student_id, course_id)
);

CREATE TABLE IF NOT EXISTS attendance (
    id               BIGSERIAL PRIMARY KEY,
    student_id       BIGINT      NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    course_id        BIGINT      NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    attendance_date  DATE        NOT NULL,
    status           VARCHAR(10) NOT NULL CHECK (status IN ('PRESENT', 'ABSENT')),
    CONSTRAINT uk_attendance_student_course_date UNIQUE (student_id, course_id, attendance_date)
);

-- The postgres user that pgAdmin uses owns these tables.
