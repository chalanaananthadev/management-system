# Applebay Institute - REST API Test Table

Comprehensive verification matrix of the Spring Boot REST endpoints.

| # | Endpoint | Method | Purpose / Scenario | Sample Request Payload | Expected Status | Expected Response / Behavior |
|---|---|---|---|---|---|---|
| 1 | `/api/courses` | `GET` | List all courses with fees | None | `200 OK` | Array of course objects including `courseFee` |
| 2 | `/api/courses` | `POST` | Create a new course | `{"courseCode":"CERT-AI","name":"Certificate in AI","level":"Certificate","duration":"6 months","courseFee":60000.00}` | `200 OK` | Created course object with auto-generated `id` |
| 3 | `/api/courses/{id}` | `GET` | Get single course details | None | `200 OK` | Course object |
| 4 | `/api/courses/{id}` | `PUT` | Update course details / fee | `{"courseCode":"CERT-AI","name":"Certificate in AI & ML","level":"Certificate","duration":"6 months","courseFee":65000.00}` | `200 OK` | Updated course object |
| 5 | `/api/courses/{id}` | `DELETE` | Remove course | None | `204 No Content` | Cascades deletion to enrolments, attendance & payments |
| 6 | `/api/students` | `GET` | List all students | Query param `?name=` | `200 OK` | Array of student objects |
| 7 | `/api/students` | `POST` | Register a new student | `{"name":"Sahan Dilshan","nic":"200012349999","email":"sahan@example.com","phone":"0712345678","address":"10 Temple Rd, Colombo"}` | `200 OK` | Created student object with unique NIC validation |
| 8 | `/api/students` | `POST` | Duplicate NIC rejection | `{"name":"Duplicate NIC","nic":"200012349999",...}` | `400 Bad Request` | `"A student with NIC 200012349999 already exists."` |
| 9 | `/api/students/onboard` | `POST` | **Atomic Onboarding (Success)**: Register student + Enrol + First Payment | `{"name":"Ruwan Silva","nic":"199712345678","email":"ruwan@example.com","phone":"0778899001","address":"77 Kandy Rd","courseId":1,"enrolledOn":"2026-10-29","paymentAmount":50000.00,"paymentMethod":"BANK_TRANSFER","paymentDate":"2026-10-29"}` | `200 OK` | Student, Enrolment, and Payment saved together; returns receipt and balance |
| 10 | `/api/students/onboard` | `POST` | **Atomic Onboarding (Rollback)**: Overpayment attempt > course fee | `{"name":"Fail Student","nic":"199599998888","email":"fail@example.com","phone":"0770001122","address":" Galle Rd","courseId":3,"enrolledOn":"2026-10-29","paymentAmount":90000.00,"paymentMethod":"CASH"}` | `400 Bad Request` | `"First payment amount (90000.00) exceeds course fee (45000.00)."`; **Transaction rolls back; student is NOT created!** |
| 11 | `/api/enrolments` | `GET` | List enrolments with financial summary | None | `200 OK` | Enrolments with `courseFee`, `totalPaid`, and `balanceRemaining` |
| 12 | `/api/enrolments` | `POST` | **Atomic Enrolment with First Payment (Success)** | `{"studentId":4,"courseId":4,"enrolledOn":"2026-10-29","paymentAmount":15000.00,"paymentMethod":"BANK_TRANSFER"}` | `201 Created` | Enrols student and creates initial payment receipt atomically |
| 13 | `/api/enrolments` | `POST` | **Atomic Enrolment (Rollback on fee exceeded)** | `{"studentId":4,"courseId":4,"paymentAmount":60000.00,"paymentMethod":"CASH"}` | `400 Bad Request` | `"First payment amount (60000.00) exceeds course fee (35000.00). Enrolment cancelled and rolled back."` (0 rows created) |
| 14 | `/api/enrolments` | `POST` | Duplicate enrolment check | `{"studentId":1,"courseId":1,"paymentAmount":50000.00,"paymentMethod":"CASH"}` | `400 Bad Request` | `"Nethmi Perera is already enrolled in HND-SE."` |
| 15 | `/api/enrolments/{id}` | `DELETE` | Remove an enrolment | None | `204 No Content` | Removes enrolment and associated payments |
| 16 | `/api/payments` | `POST` | Record subsequent installment payment (Valid) | `{"enrolmentId":1,"amount":20000.00,"paymentMethod":"CARD","paymentDate":"2026-10-29"}` | `201 Created` | Returns payment receipt with format `REC-2026-XXXX` |
| 17 | `/api/payments` | `POST` | Reject payment exceeding balance | `{"enrolmentId":1,"amount":999999.00,"paymentMethod":"CASH"}` | `400 Bad Request` | `"Payment amount exceeds remaining balance."` |
| 18 | `/api/payments` | `POST` | Reject non-positive amount | `{"enrolmentId":1,"amount":-500.00,"paymentMethod":"CASH"}` | `400 Bad Request` | `"Payment amount must be greater than zero."` |
| 19 | `/api/enrolments/{id}/payments` | `GET` | Retrieve payment history and balance for enrolment | None | `200 OK` | Complete statement: `courseFee`, `totalPaid`, `balanceRemaining`, and list of receipts |
| 19 | `/api/attendance` | `GET` | Attendance sheet for date & course | `?courseId=1&date=2026-10-15` | `200 OK` | Student list with marks (`PRESENT`, `ABSENT`, or `null`) |
| 20 | `/api/attendance` | `POST` | Bulk mark attendance | `{"courseId":1,"date":"2026-10-29","records":[{"studentId":1,"status":"PRESENT"},{"studentId":2,"status":"ABSENT"}]}` | `200 OK` | Updated attendance sheet |
| 21 | `/api/attendance/student/{id}` | `GET` | Student attendance breakdown & 80% rule | None | `200 OK` | Total, present, absent counts, percentage, and `isEligible` status flag |

