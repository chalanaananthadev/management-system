# Applebay Institute - Data Flow & Transaction Sequence Diagrams

## 1. Context Diagram (Level 0 DFD)

```mermaid
flowchart LR
    Admin[Institute Administrator / Academic Officer]
    System((Applebay Institute Management System))
    DB[(PostgreSQL Database)]

    Admin -->|Course Details, Student Data, Enrolments, Attendance, Payments| System
    System -->|Course Catalogue, Student Records, Registers, Receipts, Eligibility Reports| Admin

    System <-->|Read & Write Relational Tables| DB
```

---

## 2. Level 1 Data Flow Diagram (DFD)

```mermaid
flowchart TD
    Officer[Administrator / Officer]

    subgraph SystemProcesses [System Processes]
        P1[1.0 Course Management]
        P2[2.0 Student Management]
        P3[3.0 Unified Enrolment & Payment Management]
        P4[4.0 Attendance Management]
        P5[5.0 Tuition Payment & Receipts]
    end

    subgraph DataStores [Data Stores]
        D1[(D1: Courses)]
        D2[(D2: Students)]
        D3[(D3: Enrolments)]
        D4[(D4: Attendance)]
        D5[(D5: Payments)]
    end

    Officer -->|Course Info| P1
    P1 -->|Store / Update Course| D1
    D1 -->|Course List & Fees| P1
    P1 -->|Course Reports| Officer

    Officer -->|Student Profile Info| P2
    P2 -->|Create / Update Student| D2
    D2 -->|Search Student Profile| P2
    P2 -->|Student Records| Officer

    Officer -->|Enrolment Request & Initial Payment| P3
    P3 -->|Validate Student & Course Fee| D1
    P3 -->|Validate Student & Course Fee| D2
    P3 -->|Save Enrolment & First Payment Atomically| D3
    P3 -->|Save Enrolment & First Payment Atomically| D5
    P3 -->|Enrolment Details & Receipt| Officer

    Officer -->|Daily Session Marks| P4
    P4 -->|Verify Active Enrolment| D3
    P4 -->|Store / Update Attendance| D4
    D4 -->|Calculate Attendance % & 80% Rule| P4
    P4 -->|Attendance Sheets & Exam Eligibility| Officer

    Officer -->|Subsequent Installment Payment| P5
    P5 -->|Check Remaining Balance| D1
    P5 -->|Check Remaining Balance| D3
    P5 -->|Read Total Paid| D5
    P5 -->|Generate Receipt & Record Payment| D5
    P5 -->|Payment Receipt & Outstanding Dues| Officer
```

---

## 3. Atomic Transaction & Data Flow (Section 4.3): Course Enrolment & Initial Payment

The brief specifies: *"Registration of a student, enrollment and first payment should succeed or fail together (transaction and rollback)"*.

Once a student is registered, when enrolling them into a course, the first payment must be paid simultaneously. If the initial payment amount is invalid (e.g. exceeds course fee, negative/zero), the transaction rolls back completely and the student is **not** enrolled.

```mermaid
sequenceDiagram
    autonumber
    actor Officer as Academic Officer / Frontend UI
    participant API as EnrolmentController (POST /api/enrolments)
    participant TxProxy as Spring @Transactional Proxy
    participant EnrolmentSvc as EnrolmentService
    participant PaymentSvc as PaymentService
    participant DB as PostgreSQL 18 Database

    Officer->>API: POST /api/enrolments (studentId, courseId, paymentAmount, paymentMethod)
    API->>TxProxy: begin transaction
    TxProxy->>EnrolmentSvc: create(request)

    Note over EnrolmentSvc,DB: Step 1: Validate Student & Course
    EnrolmentSvc->>DB: Check student exists & not already enrolled in course
    DB-->>EnrolmentSvc: Student & Course verified (courseFee = 150,000.00)

    Note over EnrolmentSvc,DB: Step 2: Validate Initial Payment
    alt Payment Amount Valid (0 < amount <= courseFee)
        EnrolmentSvc->>DB: INSERT INTO enrolments (student_id, course_id, enrolled_on)
        DB-->>EnrolmentSvc: Enrolment saved (id = 11)
        EnrolmentSvc->>PaymentSvc: generateReceiptNumber(year)
        PaymentSvc-->>EnrolmentSvc: Receipt generated (REC-2026-0008)
        EnrolmentSvc->>DB: INSERT INTO payments (enrolment_id = 11, amount = 15,000, receipt = REC-2026-0008)
        DB-->>EnrolmentSvc: Payment saved
        EnrolmentSvc-->>TxProxy: EnrolmentResponse with receipt
        TxProxy->>DB: COMMIT TRANSACTION
        API-->>Officer: 201 Created (Enrolment details, Course Fee, Total Paid, Balance, Receipt)
    else Payment Amount Invalid (amount > courseFee or amount <= 0)
        EnrolmentSvc-->>TxProxy: throw BadRequestException("Exceeds course fee. Enrolment cancelled.")
        TxProxy->>DB: ROLLBACK TRANSACTION
        Note over TxProxy,DB: Enrolment is undone! Student is NOT enrolled.
        API-->>Officer: 400 Bad Request ("First payment exceeds course fee. Enrolment cancelled and rolled back.")
    end
```
