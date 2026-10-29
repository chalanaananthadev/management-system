-- Sample courses, students, enrolments, attendance and payments.
-- Run after schema.sql:
--   psql -d applebay -f database/sample-data.sql

INSERT INTO courses (id, course_code, name, level, duration, course_fee) VALUES
    (1, 'HND-SE', 'HND in Software Engineering', 'HND', '2 years', 150000.00),
    (2, 'HND-BM', 'HND in Business Management', 'HND', '2 years', 120000.00),
    (3, 'CERT-WD', 'Certificate in Web Development', 'Certificate', '6 months', 45000.00),
    (4, 'CERT-DA', 'Certificate in Data Analytics', 'Certificate', '4 months', 35000.00)
ON CONFLICT (course_code) DO UPDATE SET
    course_fee = EXCLUDED.course_fee;

INSERT INTO students (id, name, nic, email, phone, address) VALUES
    (1, 'Nethmi Perera', '200145678901', 'nethmi.perera@applebay.edu', '0771234567', '12 Galle Road, Colombo'),
    (2, 'Kavindu Silva', '199912345678', 'kavindu.silva@applebay.edu', '0719876543', '45 Kandy Road, Kandy'),
    (3, 'Amaya Fernando', '200256789012', 'amaya.fernando@applebay.edu', '0765551212', '8 Beach Road, Galle'),
    (4, 'Ishara Jayawardena', '199834567890', 'ishara.jayawardena@applebay.edu', '0751112233', '22 Temple Street, Negombo')
ON CONFLICT (nic) DO NOTHING;

INSERT INTO enrolments (id, student_id, course_id, enrolled_on) VALUES
    (1, 1, 1, '2026-09-01'),
    (2, 2, 1, '2026-09-01'),
    (3, 3, 3, '2026-09-15'),
    (4, 4, 2, '2026-09-15'),
    (5, 1, 3, '2026-10-01')
ON CONFLICT ON CONSTRAINT uk_enrolment_student_course DO NOTHING;

INSERT INTO attendance (id, student_id, course_id, attendance_date, status) VALUES
    (1, 1, 1, '2026-10-15', 'PRESENT'),
    (2, 2, 1, '2026-10-15', 'ABSENT'),
    (3, 1, 1, '2026-10-20', 'PRESENT'),
    (4, 2, 1, '2026-10-20', 'PRESENT'),
    (5, 3, 3, '2026-10-16', 'PRESENT'),
    (6, 3, 3, '2026-10-20', 'ABSENT'),
    (7, 1, 3, '2026-10-16', 'PRESENT'),
    (8, 4, 2, '2026-10-15', 'PRESENT')
ON CONFLICT ON CONSTRAINT uk_attendance_student_course_date DO NOTHING;

INSERT INTO payments (id, receipt_number, enrolment_id, amount, payment_date, payment_method) VALUES
    (1, 'REC-2026-0001', 1, 50000.00, '2026-09-01', 'BANK_TRANSFER'),
    (2, 'REC-2026-0002', 1, 30000.00, '2026-10-05', 'CARD'),
    (3, 'REC-2026-0003', 2, 75000.00, '2026-09-02', 'CASH'),
    (4, 'REC-2026-0004', 3, 45000.00, '2026-09-15', 'ONLINE'),
    (5, 'REC-2026-0005', 4, 60000.00, '2026-09-16', 'BANK_TRANSFER'),
    (6, 'REC-2026-0006', 5, 20000.00, '2026-10-01', 'CASH')
ON CONFLICT (receipt_number) DO NOTHING;

SELECT setval(pg_get_serial_sequence('courses', 'id'), COALESCE((SELECT MAX(id) FROM courses), 1));
SELECT setval(pg_get_serial_sequence('students', 'id'), COALESCE((SELECT MAX(id) FROM students), 1));
SELECT setval(pg_get_serial_sequence('enrolments', 'id'), COALESCE((SELECT MAX(id) FROM enrolments), 1));
SELECT setval(pg_get_serial_sequence('attendance', 'id'), COALESCE((SELECT MAX(id) FROM attendance), 1));
SELECT setval(pg_get_serial_sequence('payments', 'id'), COALESCE((SELECT MAX(id) FROM payments), 1));
