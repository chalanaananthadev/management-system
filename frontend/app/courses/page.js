"use client";

import { useEffect, useState } from "react";
import { Banner, Field, PageHeader } from "../../components/ui";
import { api } from "../../lib/api";
import { formatCurrency } from "../../lib/format";

const EMPTY_FORM = { courseCode: "", name: "", level: "HND", duration: "", courseFee: "50000" };
const LEVELS = ["HND", "Certificate"];

export default function CoursesPage() {
  const [courses, setCourses] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  async function loadCourses() {
    const data = await api("/courses");
    setCourses(data);
  }

  useEffect(() => {
    loadCourses()
      .catch((loadError) => setError(loadError.message))
      .finally(() => setLoading(false));
  }, []);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  function startEdit(course) {
    setEditingId(course.id);
    setForm({
      courseCode: course.courseCode,
      name: course.name,
      level: course.level,
      duration: course.duration,
      courseFee: String(course.courseFee ?? 50000),
    });
    setSuccess("");
    setError("");
  }

  function cancelEdit() {
    setEditingId(null);
    setForm(EMPTY_FORM);
  }

  async function onSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const payload = {
        ...form,
        courseFee: Number(form.courseFee),
      };
      if (editingId) {
        await api(`/courses/${editingId}`, { method: "PUT", body: JSON.stringify(payload) });
        setSuccess("Course updated.");
      } else {
        await api("/courses", { method: "POST", body: JSON.stringify(payload) });
        setSuccess("Course saved.");
      }
      cancelEdit();
      await loadCourses();
    } catch (saveError) {
      setError(saveError.message);
    } finally {
      setSaving(false);
    }
  }

  async function onDelete(course) {
    if (!window.confirm(`Delete ${course.courseCode}? Enrolments, attendance and payments for this course will also be removed.`)) {
      return;
    }
    setError("");
    setSuccess("");
    try {
      await api(`/courses/${course.id}`, { method: "DELETE" });
      if (editingId === course.id) {
        cancelEdit();
      }
      setSuccess("Course deleted.");
      await loadCourses();
    } catch (deleteError) {
      setError(deleteError.message);
    }
  }

  const levelOptions = LEVELS.includes(form.level) ? LEVELS : [form.level, ...LEVELS];

  return (
    <>
      <PageHeader
        eyebrow="Courses"
        title="Course catalogue"
        text="Add, update and delete HND programmes and certificate courses. Each course needs a code, name, level, duration and fee."
      />
      <Banner error={error} success={success} />
      <div className="split">
        <form className="card" onSubmit={onSubmit}>
          <h2 className="section-title">{editingId ? "Update course" : "Add a course"}</h2>
          <div className="stack">
            <Field label="Course code">
              <input name="courseCode" value={form.courseCode} onChange={updateField} maxLength={20} required placeholder="HND-SE" />
            </Field>
            <Field label="Course name">
              <input name="name" value={form.name} onChange={updateField} maxLength={150} required placeholder="HND in Software Engineering" />
            </Field>
            <Field label="Level">
              <select name="level" value={form.level} onChange={updateField} required>
                {levelOptions.map((level) => (
                  <option key={level} value={level}>{level}</option>
                ))}
              </select>
            </Field>
            <Field label="Duration">
              <input name="duration" value={form.duration} onChange={updateField} maxLength={50} required placeholder="2 years" />
            </Field>
            <Field label="Course fee (LKR)">
              <input
                name="courseFee"
                type="number"
                step="0.01"
                min="1"
                value={form.courseFee}
                onChange={updateField}
                required
                placeholder="150000.00"
              />
            </Field>
          </div>
          <div className="actions">
            <button className="btn primary" type="submit" disabled={saving}>
              {saving ? "Saving…" : editingId ? "Update course" : "Save course"}
            </button>
            {editingId ? (
              <button className="btn ghost" type="button" onClick={cancelEdit}>Cancel</button>
            ) : null}
          </div>
        </form>
        <section className="card">
          <h2 className="section-title">All courses</h2>
          {loading ? <p className="empty">Loading courses…</p> : null}
          {!loading && courses.length === 0 ? <p className="empty">No courses yet. Add the first one with the form.</p> : null}
          {courses.length > 0 ? (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Code</th>
                    <th>Name</th>
                    <th>Level</th>
                    <th>Duration</th>
                    <th>Fee</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {courses.map((course) => (
                    <tr key={course.id}>
                      <td>{course.courseCode}</td>
                      <td>{course.name}</td>
                      <td><span className="pill">{course.level}</span></td>
                      <td>{course.duration}</td>
                      <td><strong>LKR {formatCurrency(course.courseFee)}</strong></td>
                      <td>
                        <div className="row-actions">
                          <button className="btn ghost" type="button" onClick={() => startEdit(course)}>Edit</button>
                          <button className="btn danger" type="button" onClick={() => onDelete(course)}>Delete</button>
                        </div>
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
