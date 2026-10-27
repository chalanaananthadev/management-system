"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Banner, Field, PageHeader } from "../../components/ui";
import { api } from "../../lib/api";
import { todayInputValue } from "../../lib/format";

export default function MarkAttendancePage() {
  const [courses, setCourses] = useState([]);
  const [courseId, setCourseId] = useState("");
  const [date, setDate] = useState(todayInputValue);
  const [sheet, setSheet] = useState(null);
  const [rows, setRows] = useState([]);
  const [loadingCourses, setLoadingCourses] = useState(true);
  const [loadingSheet, setLoadingSheet] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    api("/courses")
      .then(setCourses)
      .catch((loadError) => setError(loadError.message))
      .finally(() => setLoadingCourses(false));
  }, []);

  useEffect(() => {
    if (!courseId || !date) {
      setSheet(null);
      setRows([]);
      return;
    }

    let cancelled = false;
    setLoadingSheet(true);
    setError("");
    setSuccess("");

    api(`/attendance?courseId=${courseId}&date=${date}`)
      .then((data) => {
        if (cancelled) {
          return;
        }
        setSheet(data);
        setRows(data.students.map((student) => ({ ...student, status: student.status || "" })));
      })
      .catch((loadError) => {
        if (!cancelled) {
          setError(loadError.message);
          setSheet(null);
          setRows([]);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoadingSheet(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [courseId, date]);

  function setStatus(studentId, status) {
    setRows((current) => current.map((row) => (row.studentId === studentId ? { ...row, status } : row)));
  }

  function markAll(status) {
    setRows((current) => current.map((row) => ({ ...row, status })));
  }

  async function onSave(event) {
    event.preventDefault();
    if (rows.some((row) => !row.status)) {
      setError("Mark every student as Present or Absent.");
      setSuccess("");
      return;
    }
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const saved = await api("/attendance", {
        method: "POST",
        body: JSON.stringify({
          courseId: Number(courseId),
          date,
          records: rows.map((row) => ({ studentId: row.studentId, status: row.status })),
        }),
      });
      setSheet(saved);
      setRows(saved.students.map((student) => ({ ...student, status: student.status || "" })));
      setSuccess("Attendance saved.");
    } catch (saveError) {
      setError(saveError.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Attendance"
        title="Mark attendance"
        text="Choose a course and a date. Every enrolled student can be marked Present or Absent. Saving again on the same date updates the marks."
      />
      <Banner error={error} success={success} />
      <div className="stack">
        <section className="card">
          <div className="inline-form">
            <Field label="Course">
              <select value={courseId} onChange={(event) => setCourseId(event.target.value)} disabled={loadingCourses}>
                <option value="">Select a course</option>
                {courses.map((course) => (
                  <option key={course.id} value={course.id}>{course.courseCode} · {course.name}</option>
                ))}
              </select>
            </Field>
            <Field label="Date">
              <input type="date" value={date} onChange={(event) => setDate(event.target.value)} required />
            </Field>
          </div>
        </section>

        <section className="card">
          {!courseId ? <p className="empty">Select a course to load its register.</p> : null}
          {loadingSheet ? <p className="empty">Loading the register…</p> : null}
          {sheet && !loadingSheet && rows.length === 0 ? (
            <p className="empty">
              No students are enrolled in {sheet.courseCode}. <Link href="/enrolments">Enrol a student</Link> first.
            </p>
          ) : null}
          {sheet && rows.length > 0 ? (
            <form onSubmit={onSave}>
              <div className="toolbar" style={{ justifyContent: "space-between", marginBottom: 12 }}>
                <h2 className="section-title" style={{ margin: 0 }}>{sheet.courseCode} on {sheet.date}</h2>
                <div className="row-actions">
                  <button className="btn ghost" type="button" onClick={() => markAll("PRESENT")}>All present</button>
                  <button className="btn ghost" type="button" onClick={() => markAll("ABSENT")}>All absent</button>
                </div>
              </div>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Student</th>
                      <th>NIC</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((row) => (
                      <tr key={row.studentId}>
                        <td>{row.studentName}</td>
                        <td>{row.nic}</td>
                        <td>
                          <div className="segment" role="group" aria-label={`Attendance for ${row.studentName}`}>
                            <button
                              type="button"
                              className={row.status === "PRESENT" ? "on present" : undefined}
                              aria-pressed={row.status === "PRESENT"}
                              onClick={() => setStatus(row.studentId, "PRESENT")}
                            >
                              Present
                            </button>
                            <button
                              type="button"
                              className={row.status === "ABSENT" ? "on absent" : undefined}
                              aria-pressed={row.status === "ABSENT"}
                              onClick={() => setStatus(row.studentId, "ABSENT")}
                            >
                              Absent
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="actions">
                <button className="btn primary" type="submit" disabled={saving}>
                  {saving ? "Saving…" : "Save attendance"}
                </button>
              </div>
            </form>
          ) : null}
        </section>
      </div>
    </>
  );
}
