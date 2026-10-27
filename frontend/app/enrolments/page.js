"use client";

import { useEffect, useState } from "react";
import { Banner, Field, PageHeader } from "../../components/ui";
import { api } from "../../lib/api";
import { formatDate, todayInputValue } from "../../lib/format";

export default function EnrolmentsPage() {
  const [students, setStudents] = useState([]);
  const [courses, setCourses] = useState([]);
  const [enrolments, setEnrolments] = useState([]);
  const [form, setForm] = useState(() => ({ studentId: "", courseId: "", enrolledOn: todayInputValue() }));
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  async function loadEnrolments() {
    const data = await api("/enrolments");
    setEnrolments(data);
  }

  useEffect(() => {
    Promise.all([api("/students"), api("/courses"), api("/enrolments")])
      .then(([studentRows, courseRows, enrolmentRows]) => {
        setStudents(studentRows);
        setCourses(courseRows);
        setEnrolments(enrolmentRows);
      })
      .catch((loadError) => setError(loadError.message))
      .finally(() => setLoading(false));
  }, []);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function onSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      await api("/enrolments", {
        method: "POST",
        body: JSON.stringify({
          studentId: Number(form.studentId),
          courseId: Number(form.courseId),
          enrolledOn: form.enrolledOn,
        }),
      });
      setForm((current) => ({ studentId: "", courseId: "", enrolledOn: current.enrolledOn }));
      setSuccess("Student enrolled.");
      await loadEnrolments();
    } catch (saveError) {
      setError(saveError.message);
    } finally {
      setSaving(false);
    }
  }

  async function onDelete(enrolment) {
    if (!window.confirm(`Remove ${enrolment.studentName} from ${enrolment.courseCode}?`)) {
      return;
    }
    setError("");
    setSuccess("");
    try {
      await api(`/enrolments/${enrolment.id}`, { method: "DELETE" });
      setSuccess("Enrolment removed.");
      await loadEnrolments();
    } catch (deleteError) {
      setError(deleteError.message);
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Enrolment"
        title="Enrol a student"
        text="Place a registered student onto a course. A student can be enrolled once in each course."
      />
      <Banner error={error} success={success} />
      <div className="stack">
        <form className="card" onSubmit={onSubmit}>
          <div className="inline-form">
            <Field label="Student">
              <select name="studentId" value={form.studentId} onChange={updateField} required>
                <option value="">Select a student</option>
                {students.map((student) => (
                  <option key={student.id} value={student.id}>{student.name} · {student.nic}</option>
                ))}
              </select>
            </Field>
            <Field label="Course">
              <select name="courseId" value={form.courseId} onChange={updateField} required>
                <option value="">Select a course</option>
                {courses.map((course) => (
                  <option key={course.id} value={course.id}>{course.courseCode} · {course.name}</option>
                ))}
              </select>
            </Field>
            <Field label="Enrolment date">
              <input name="enrolledOn" type="date" value={form.enrolledOn} onChange={updateField} required />
            </Field>
            <button className="btn primary" type="submit" disabled={saving || students.length === 0 || courses.length === 0}>
              {saving ? "Saving…" : "Enrol student"}
            </button>
          </div>
        </form>
        <section className="card">
          <h2 className="section-title">Current enrolments</h2>
          {loading ? <p className="empty">Loading enrolments…</p> : null}
          {!loading && enrolments.length === 0 ? <p className="empty">No enrolments yet.</p> : null}
          {enrolments.length > 0 ? (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Student</th>
                    <th>NIC</th>
                    <th>Course</th>
                    <th>Enrolled</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {enrolments.map((enrolment) => (
                    <tr key={enrolment.id}>
                      <td>{enrolment.studentName}</td>
                      <td>{enrolment.studentNic}</td>
                      <td>{enrolment.courseCode} · {enrolment.courseName}</td>
                      <td>{formatDate(enrolment.enrolledOn)}</td>
                      <td>
                        <button className="btn danger" type="button" onClick={() => onDelete(enrolment)}>Remove</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>
      </div>
    </>
  );
}
