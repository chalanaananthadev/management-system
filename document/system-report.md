# Applebay Institute Management System - Technical Report & Architecture Documentation

## 1. Executive Summary & Problem Scope
The Applebay Institute Management System is a full-stack enterprise web application designed to administer academic courses, student registrations, module enrolments, daily attendance registers, and tuition fee payments.

The system addresses critical academic operational challenges:
- **Unified Academic Administration**: Centralizing course management and student records.
- **Accurate Attendance Tracking & 80% Rule**: Calculating attendance percentage per course and identifying students eligible ($\ge 80\%$) vs. ineligible ($< 80\%$) for semester examinations.
- **Tuition Fee & Payment Module**: Enforcing that payments cannot exceed the course fee minus total paid, providing auto-generated sequential receipt numbers (`REC-YYYY-XXXX`), and tracking remaining dues.
- **Atomic Multi-Entity Transactions (Section 4.3)**: Guaranteeing that student registration, enrolment, and first payment succeed or fail together through transactional integrity and automatic rollback.

---

## 2. System Architecture & Tech Stack

```
+-------------------------------------------------------------+
|               Frontend Tier: Next.js 15 (React 19)          |
|  - Modern Dashboard with Live Metric Summaries             |
|  - Student Register, Course Catalogue & Enrolment Manager  |
|  - Attendance Marking Sheet & Student Records Breakdown     |
|  - Payment Modal & Atomic Onboarding Wizard                 |
+------------------------------+------------------------------+
                               | RESTful JSON (HTTP)
+------------------------------v------------------------------+
|              Backend Tier: Spring Boot 3.3.5 (Java 17/24)   |
|  - Spring MVC REST Controllers                              |
|  - Spring Data JPA / Hibernate ORM                          |
|  - Declarative @Transactional Boundary Control              |
|  - Custom Bean Validation & Global ApiExceptionHandler      |
+------------------------------+------------------------------+
                               | JDBC / SQL
+------------------------------v------------------------------+
|             Database Tier: PostgreSQL 18 (Port 5432)        |
|  - Relational Schema with Foreign Key Constraints           |
|  - Check Constraints (Status, Amount > 0)                    |
|  - Unique Constraints (NIC, Course Code, Enrolment, Session)|
+-------------------------------------------------------------+
```

---

## 3. Database Schema Specifications

### Relational Tables & Attributes
1. **`courses`**:
   - `id` (BIGSERIAL, PK)
   - `course_code` (VARCHAR(20), UNIQUE, NOT NULL)
   - `name` (VARCHAR(150), NOT NULL)
   - `level` (VARCHAR(50), NOT NULL)
   - `duration` (VARCHAR(50), NOT NULL)
   - `course_fee` (NUMERIC(10,2), NOT NULL DEFAULT 50000.00)

2. **`students`**:
   - `id` (BIGSERIAL, PK)
   - `name` (VARCHAR(150), NOT NULL)
   - `nic` (VARCHAR(20), UNIQUE, NOT NULL)
   - `email` (VARCHAR(150), NOT NULL)
   - `phone` (VARCHAR(20), NOT NULL)
   - `address` (VARCHAR(255), NOT NULL)

3. **`enrolments`**:
   - `id` (BIGSERIAL, PK)
   - `student_id` (BIGINT, FK -> students(id) ON DELETE CASCADE, NOT NULL)
   - `course_id` (BIGINT, FK -> courses(id) ON DELETE CASCADE, NOT NULL)
   - `enrolled_on` (DATE, NOT NULL DEFAULT CURRENT_DATE)
   - Constraint: `uk_enrolment_student_course UNIQUE (student_id, course_id)`

4. **`attendance`**:
   - `id` (BIGSERIAL, PK)
   - `student_id` (BIGINT, FK -> students(id) ON DELETE CASCADE, NOT NULL)
   - `course_id` (BIGINT, FK -> courses(id) ON DELETE CASCADE, NOT NULL)
   - `attendance_date` (DATE, NOT NULL)
   - `status` (VARCHAR(10), CHECK IN ('PRESENT', 'ABSENT'), NOT NULL)
   - Constraint: `uk_attendance_student_course_date UNIQUE (student_id, course_id, attendance_date)`

5. **`payments`**:
   - `id` (BIGSERIAL, PK)
   - `receipt_number` (VARCHAR(30), UNIQUE, NOT NULL)
   - `enrolment_id` (BIGINT, FK -> enrolments(id) ON DELETE CASCADE, NOT NULL)
   - `amount` (NUMERIC(10,2), CHECK (amount > 0), NOT NULL)
   - `payment_date` (DATE, NOT NULL DEFAULT CURRENT_DATE)
   - `payment_method` (VARCHAR(30), NOT NULL)

---

## 4. Key Business Logic Implementations

### 4.1 Payment Balance Enforcement, PDF Bill Generation & Customer E-Bill Dispatch
1. **Rule**: $\text{amount} \le (\text{course\_fee} - \text{total\_paid})$.
   - The backend retrieves the total amount previously paid for the given `enrolment_id`.
   - Remaining balance is computed as $\text{balance} = \text{course\_fee} - \text{total\_paid}$.
   - If $\text{amount} > \text{balance}$, a `BadRequestException` is thrown with exact financial details.
   - If $\text{amount} \le 0$, payment is rejected.
2. **Receipt Number Formatting**:
   - Receipts follow the standard structure `REC-YYYY-XXXX` (e.g. `REC-2026-0001`).
   - The number sequence auto-increments based on existing receipts for the calendar year.
3. **Official PDF Bill / Receipt Printing**:
   - A dedicated `ReceiptModal` provides an official electronic invoice containing institute letterhead, verification stamp, student ID, course details, itemized fee breakdown, cumulative amount paid, and outstanding balance.
   - Responsive print stylesheets (`@media print`) allow one-click export directly to an A4 PDF document or physical printer without website chrome or navigation artifacts.
4. **Customer E-Bill Email Notification**:
   - Endpoint `POST /api/payments/{id}/email` enables immediate dispatch of the electronic bill to the customer's email address.
   - The e-bill provides students with transparent visibility into their tuition payment history, recent installment receipt, and remaining financial obligations.
   - Direct desktop mail client integration (`mailto:`) is also provided as a fallback.

### 4.2 Attendance Calculation & 80% Rule
1. Attendance percentage is calculated as:
   $$\text{Attendance \%} = \frac{\text{Sessions Present}}{\text{Total Sessions Marked}} \times 100$$
2. **Exam Eligibility Rule**:
   - $\ge 80.0\% \implies$ **Eligible** (Green pill).
   - $< 80.0\% \implies$ **Ineligible / Attendance Warning** (Red/Yellow badge).

### 4.3 Atomic Multi-Step Transactions & Automatic Rollback (Section 4.3)
The brief specifies: *"Registration of a student, enrollment and first payment should succeed or fail together (transaction and rollback)"*.

1. **Unified Course Enrolment & Initial Payment**:
   - Implemented in `EnrolmentService.create(EnrolmentRequest)` with `@Transactional(rollbackFor = Exception.class)`.
   - Once a student is registered, when enrolling them into a course, the first payment is strictly required in the same unified step.
   - If the first payment is invalid (e.g., amount $\le 0$ or amount $>$ course fee), a `BadRequestException` is thrown, triggering an automatic rollback: **the enrolment is NOT created**.
   - If valid, both the enrolment record and the payment receipt (`REC-YYYY-XXXX`) are persisted atomically.

2. **Complete Student Onboarding Transaction**:
   - Implemented in `StudentService.onboard(StudentOnboardingRequest)` with `@Transactional(rollbackFor = Exception.class)`.
   - Atomically registers a new student, enrols them, and records the initial payment. If any validation fails, neither the student, enrolment, nor payment is persisted.

---

## 5. Submission Folder Hierarchy Verification (Section 6)
The project submission hierarchy is organized strictly according to Section 6:
```
submission/
├── frontend/
│   ├── app/
│   ├── components/
│   ├── lib/
│   ├── package.json
│   ├── next.config.js
│   └── .gitignore (excludes node_modules and .next)
├── backend/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── .gitignore (excludes target/)
├── database/
│   ├── schema.sql
│   ├── sample-data.sql
│   └── queries.sql
└── document/
    ├── api-test-table.md
    ├── er-diagram.md
    ├── data-flow-diagram.md
    └── system-report.md
```

