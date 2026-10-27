"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Banner, Field, PageHeader } from "../../components/ui";
import { api } from "../../lib/api";

const EMPTY_FORM = { name: "", nic: "", email: "", phone: "", address: "" };

export default function StudentsPage() {
  const [students, setStudents] = useState([]);
  const [query, setQuery] = useState("");
  const [activeQuery, setActiveQuery] = useState("");
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  async function loadStudents(name = activeQuery) {
    const path = name.trim() ? `/students?name=${encodeURIComponent(name.trim())}` : "/students";
    const data = await api(path);
    setStudents(data);
  }

  useEffect(() => {
    loadStudents("")
      .catch((loadError) => setError(loadError.message))
      .finally(() => setLoading(false));
  }, []);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  function startEdit(student) {
    setEditingId(student.id);
    setForm({
      name: student.name,
      nic: student.nic,
      email: student.email,
      phone: student.phone,
      address: student.address,
    });
    setError("");
    setSuccess("");
  }

  function cancelEdit() {
    setEditingId(null);
    setForm(EMPTY_FORM);
  }

  async function onSearch(event) {
    event.preventDefault();
    setLoading(true);
    setError("");
    setSuccess("");
    setActiveQuery(query);
    try {
      await loadStudents(query);
    } catch (searchError) {
      setError(searchError.message);
    } finally {
      setLoading(false);
    }
  }

  async function showAll() {
    setQuery("");
    setActiveQuery("");
    setLoading(true);
    setError("");
    try {
      await loadStudents("");
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setLoading(false);
    }
  }

  async function onSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      if (editingId) {
        await api(`/students/${editingId}`, { method: "PUT", body: JSON.stringify(form) });
        setSuccess("Student updated.");
      } else {
        await api("/students", { method: "POST", body: JSON.stringify(form) });
        setSuccess("Student registered.");
      }
      cancelEdit();
      await loadStudents(activeQuery);
    } catch (saveError) {
      setError(saveError.message);
    } finally {
      setSaving(false);
    }
  }

  async function onDelete(student) {
    if (!window.confirm(`Delete ${student.name}? Enrolments and attendance for this student will also be removed.`)) {
      return;
    }
    setError("");
    setSuccess("");
    try {
      await api(`/students/${student.id}`, { method: "DELETE" });
      if (editingId === student.id) {
        cancelEdit();
      }
      setSuccess("Student deleted.");
      await loadStudents(activeQuery);
    } catch (deleteError) {
      setError(deleteError.message);
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Students"
        title="Student register"
        text="Register a student with name, NIC, email, phone and address. Search the list by name, then update or delete a record."
      />
      <Banner error={error} success={success} />
      <div className="stack">
        <form className="card" onSubmit={onSubmit}>
          <h2 className="section-title">{editingId ? "Update student" : "Register a student"}</h2>
          <div className="form-grid">
            <Field label="Full name">
              <input name="name" value={form.name} onChange={updateField} maxLength={150} required />
            </Field>
            <Field label="NIC">
              <input name="nic" value={form.nic} onChange={updateField} maxLength={20} required />
            </Field>
            <Field label="Email">
              <input name="email" type="email" value={form.email} onChange={updateField} maxLength={150} required />
            </Field>
            <Field label="Phone">
              <input name="phone" type="tel" value={form.phone} onChange={updateField} maxLength={20} required />
            </Field>
            <Field label="Address" wide>
              <textarea name="address" value={form.address} onChange={updateField} maxLength={255} required />
            </Field>
          </div>
          <div className="actions">
            <button className="btn primary" type="submit" disabled={saving}>
              {saving ? "Saving…" : editingId ? "Update student" : "Register student"}
            </button>
            {editingId ? (
              <button className="btn ghost" type="button" onClick={cancelEdit}>Cancel</button>
            ) : null}
          </div>
        </form>

        <section className="card">
          <div className="toolbar" style={{ justifyContent: "space-between", marginBottom: 12 }}>
            <h2 className="section-title" style={{ margin: 0 }}>Students</h2>
            <form className="search-form" onSubmit={onSearch}>
              <Field label="Search by name">
                <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Type a name" />
              </Field>
              <button className="btn primary" type="submit">Search</button>
              {activeQuery ? (
                <button className="btn ghost" type="button" onClick={showAll}>Show all</button>
              ) : null}
            </form>
          </div>
          {loading ? <p className="empty">Loading students…</p> : null}
          {!loading && students.length === 0 ? (
            <p className="empty">{activeQuery ? `No students match “${activeQuery}”.` : "No students yet."}</p>
          ) : null}
          {students.length > 0 ? (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>NIC</th>
                    <th>Email</th>
                    <th>Phone</th>
                    <th>Address</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {students.map((student) => (
                    <tr key={student.id}>
                      <td>{student.name}</td>
                      <td>{student.nic}</td>
                      <td>{student.email}</td>
                      <td>{student.phone}</td>
                      <td>{student.address}</td>
                      <td>
                        <div className="row-actions">
                          <Link className="btn ghost" href={`/attendance/records?student=${student.id}`}>Attendance</Link>
                          <button className="btn ghost" type="button" onClick={() => startEdit(student)}>Edit</button>
                          <button className="btn danger" type="button" onClick={() => onDelete(student)}>Delete</button>
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
