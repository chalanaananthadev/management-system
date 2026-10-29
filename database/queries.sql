-- Applebay Institute - Analytical and Operational SQL Queries

-- 1. Enrolment Payment Balances & Dues
-- Calculates course fee, total paid, remaining balance, and payment status for every enrolment.
SELECT 
    e.id AS enrolment_id,
    s.name AS student_name,
    s.nic AS student_nic,
    c.course_code,
    c.name AS course_name,
    c.course_fee,
    COALESCE(SUM(p.amount), 0.00) AS total_paid,
    (c.course_fee - COALESCE(SUM(p.amount), 0.00)) AS balance_remaining,
    CASE 
        WHEN COALESCE(SUM(p.amount), 0.00) >= c.course_fee THEN 'FULLY_PAID'
        WHEN COALESCE(SUM(p.amount), 0.00) > 0 THEN 'PARTIALLY_PAID'
        ELSE 'UNPAID'
    END AS payment_status
FROM enrolments e
JOIN students s ON e.student_id = s.id
JOIN courses c ON e.course_id = c.id
LEFT JOIN payments p ON p.enrolment_id = e.id
GROUP BY e.id, s.name, s.nic, c.course_code, c.name, c.course_fee
ORDER BY balance_remaining DESC;

-- 2. Students with Outstanding Fee Balances
SELECT 
    s.name AS student_name,
    s.phone AS student_phone,
    s.email AS student_email,
    c.course_code,
    c.course_fee,
    COALESCE(SUM(p.amount), 0.00) AS total_paid,
    (c.course_fee - COALESCE(SUM(p.amount), 0.00)) AS outstanding_balance
FROM enrolments e
JOIN students s ON e.student_id = s.id
JOIN courses c ON e.course_id = c.id
LEFT JOIN payments p ON p.enrolment_id = e.id
GROUP BY s.name, s.phone, s.email, c.course_code, c.course_fee
HAVING (c.course_fee - COALESCE(SUM(p.amount), 0.00)) > 0
ORDER BY outstanding_balance DESC;

-- 3. Attendance Percentage & Exam Eligibility (80% Rule)
-- Students with attendance >= 80% are eligible for final examinations.
SELECT 
    s.id AS student_id,
    s.name AS student_name,
    c.course_code,
    COUNT(a.id) AS total_sessions,
    COUNT(CASE WHEN a.status = 'PRESENT' THEN 1 END) AS present_count,
    COUNT(CASE WHEN a.status = 'ABSENT' THEN 1 END) AS absent_count,
    ROUND(
        (COUNT(CASE WHEN a.status = 'PRESENT' THEN 1 END)::numeric / NULLIF(COUNT(a.id), 0)) * 100, 
        1
    ) AS attendance_percentage,
    CASE 
        WHEN (COUNT(CASE WHEN a.status = 'PRESENT' THEN 1 END)::numeric / NULLIF(COUNT(a.id), 0)) * 100 >= 80.0 
        THEN 'ELIGIBLE'
        ELSE 'INELIGIBLE (BELOW 80%)'
    END AS exam_eligibility
FROM enrolments e
JOIN students s ON e.student_id = s.id
JOIN courses c ON e.course_id = c.id
LEFT JOIN attendance a ON a.student_id = s.id AND a.course_id = c.id
GROUP BY s.id, s.name, c.course_code
ORDER BY c.course_code, s.name;

-- 4. Course Enrolment & Popularity Summary
SELECT 
    c.course_code,
    c.name AS course_name,
    c.level,
    c.course_fee,
    COUNT(e.id) AS total_enrolments,
    COALESCE(SUM(p.amount), 0.00) AS total_revenue_collected
FROM courses c
LEFT JOIN enrolments e ON c.id = e.course_id
LEFT JOIN payments p ON p.enrolment_id = e.id
GROUP BY c.id, c.course_code, c.name, c.level, c.course_fee
ORDER BY total_enrolments DESC;

-- 5. Daily Attendance Sheet for a Course and Date
SELECT 
    s.name AS student_name,
    s.nic AS student_nic,
    c.course_code,
    COALESCE(a.status, 'NOT MARKED') AS attendance_status,
    '2026-10-15' AS session_date
FROM enrolments e
JOIN students s ON e.student_id = s.id
JOIN courses c ON e.course_id = c.id
LEFT JOIN attendance a ON a.student_id = s.id AND a.course_id = c.id AND a.attendance_date = '2026-10-15'
WHERE c.course_code = 'HND-SE'
ORDER BY s.name;

