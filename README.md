# Applebay Institute

Full-stack system for managing courses, students, enrolment and attendance.

The Next.js pages call the Spring Boot REST API, and the API reads and writes PostgreSQL.

## Prerequisites

- Java 17 or later
- Maven 3.9 or later
- Node.js 18 or later
- PostgreSQL 14 or later

Homebrew Java on macOS is not always on the PATH. If `java` is not found:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"
```

## Database

Backend login, also stored in `backend/src/main/resources/application.properties`:

This is the PostgreSQL 18 server registered in pgAdmin.

- Host: `localhost`
- Port: `5433`
- Database: `applebay`
- Username: `postgres`
- Password: `1234`

Create the role, database, tables and sample rows:

```bash
sh database/create-database.sh
```

Or run the scripts yourself:

```bash
psql -h localhost -p 5433 -U postgres -d postgres -c "CREATE DATABASE applebay OWNER postgres;"
psql -h localhost -p 5433 -U postgres -d applebay -f database/schema.sql
psql -h localhost -p 5433 -U postgres -d applebay -f database/sample-data.sql
```

## Backend

```bash
cd backend
mvn spring-boot:run
```

The API listens on http://localhost:8080.

## Frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:3000.

## Pages

| Page | Path | What it does |
| --- | --- | --- |
| Dashboard | `/` | Counts loaded from the API |
| Courses | `/courses` | Add, view, update and delete courses |
| Students | `/students` | Register, view, search, update and delete students |
| Enrolment | `/enrolments` | Enrol a student in a course |
| Mark attendance | `/attendance` | Present or Absent for a course on a date |
| Attendance records | `/attendance/records` | A student's records and attendance percentage |

## API

| Method | URL | Purpose |
| --- | --- | --- |
| GET | `/api/courses` | List courses |
| GET | `/api/courses/{id}` | Get one course |
| POST | `/api/courses` | Add a course |
| PUT | `/api/courses/{id}` | Update a course |
| DELETE | `/api/courses/{id}` | Delete a course |
| GET | `/api/students?name=` | List students, or search by name |
| GET | `/api/students/{id}` | Get one student |
| POST | `/api/students` | Register a student |
| PUT | `/api/students/{id}` | Update a student |
| DELETE | `/api/students/{id}` | Delete a student |
| GET | `/api/enrolments` | List enrolments |
| POST | `/api/enrolments` | Enrol a student |
| DELETE | `/api/enrolments/{id}` | Remove an enrolment |
| GET | `/api/attendance?courseId=&date=` | Attendance sheet for a course and date |
| POST | `/api/attendance` | Save Present or Absent marks |
| GET | `/api/attendance/student/{studentId}` | Records and percentage for one student |
