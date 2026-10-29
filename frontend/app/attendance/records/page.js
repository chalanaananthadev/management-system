"use client";

import { useEffect, useState } from "react";
import { Banner, Field, PageHeader } from "../../../components/ui";
import { api } from "../../../lib/api";
import { formatDate, formatPercent } from "../../../lib/format";

export default function AttendanceRecordsPage() {
  const [students, setStudents] = useState([]);
  const [studentId, setStudentId] = useState("");
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const requested = new URLSearchParams(window.location.search).get("student");
    api("/students")
      .then((rows) => {
        setStudents(rows);
        if (requested && rows.some((student) => String(student.id) === requested)) {
          setStudentId(requested);
        }
      })
      .catch((loadError) => setError(loadError.message));
  }, []);

  useEffect(() => {
    if (!studentId) {
      setSummary(null);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError("");

    api(`/attendance/student/${studentId}`)
      .then((data) => {
        if (!cancelled) {
          setSummary(data);
        }
      })
      .catch((loadError) => {
        if (!cancelled) {
          setError(loadError.message);
          setSummary(null);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [studentId]);

  return (
    <>
      <PageHeader
        eyebrow="Attendance"
        title="Attendance records"
        text="Choose a student to see every saved mark and the attendance percentage, overall and for each course."
      />
      <Banner error={error} />
      <div className="stack">
        <section className="card">
          <Field label="Student">
            <select value={studentId} onChange={(event) => setStudentId(event.target.value)}>
              <option value="">Select a student</option>
              {students.map((student) => (
                <option key={student.id} value={student.id}>{student.name} · {student.nic}</option>
              ))}
            </select>
          </Field>
        </section>

        {!studentId ? <p className="empty">Select a student to load attendance.</p> : null}
        {loading ? <p className="empty">Loading attendance…</p> : null}

        {summary && !loading ? (
          <>
            <section className="card percent-panel">
              <div>
                <p className="eyebrow">Overall</p>
                <div className="percent-value">{formatPercent(summary.percentage)}</div>
              </div>
              <div>
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <h2 className="section-title" style={{ margin: 0 }}>{summary.studentName}</h2>
                  {summary.isEligible !== null && summary.isEligible !== undefined && (
                    <span className={`badge ${summary.isEligible ? "eligible" : "ineligible"}`}>
                      {summary.isEligible ? "✓ Exam Eligible (≥ 80%)" : "⚠ Ineligible (< 80%)"}
                    </span>
                  )}
                </div>
                <p className="muted" style={{ marginTop: 4 }}>NIC {summary.nic}</p>
                <p>
                  {summary.presentCount} present · {summary.absentCount} absent · {summary.totalSessions}{" "}
                  {summary.totalSessions === 1 ? "session" : "sessions"}
                </p>
              </div>
            </section>

            <section className="card">
              <h2 className="section-title">By course</h2>
              {summary.courses.length === 0 ? <p className="empty">No attendance has been recorded for this student.</p> : null}
              {summary.courses.length > 0 ? (
                <div className="breakdown">
                  {summary.courses.map((course) => (
                    <article key={course.courseId}>
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                        <span className="pill">{course.courseCode}</span>
                        {course.isEligible !== null && course.isEligible !== undefined && (
                          <span className={`badge ${course.isEligible ? "eligible" : "ineligible"}`} style={{ fontSize: 11, minWidth: "auto" }}>
                            {course.isEligible ? "≥ 80%" : "< 80%"}
                          </span>
                        )}
                      </div>
                      <p style={{ margin: "8px 0 4px" }}>{course.courseName}</p>
                      <strong>{formatPercent(course.percentage)}</strong>
                      <span className="muted">{course.presentCount} of {course.totalSessions} present</span>
                    </article>
                  ))}
                </div>
              ) : null}
            </section>

            <section className="card">
              <h2 className="section-title">Records</h2>
              {summary.records.length === 0 ? <p className="empty">No attendance records yet.</p> : null}
              {summary.records.length > 0 ? (
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Date</th>
                        <th>Course</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {summary.records.map((record) => (
                        <tr key={record.id}>
                          <td>{formatDate(record.date)}</td>
                          <td>{record.courseCode} · {record.courseName}</td>
                          <td>
                            <span className={record.status === "PRESENT" ? "badge present" : "badge absent"}>
                              {record.status === "PRESENT" ? "Present" : "Absent"}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
            </section>
          </>
        ) : null}
      </div>
    </>
  );
}
