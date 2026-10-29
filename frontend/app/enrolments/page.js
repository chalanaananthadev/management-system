"use client";

import { useEffect, useState } from "react";
import PaymentModal from "../../components/PaymentModal";
import ReceiptModal from "../../components/ReceiptModal";
import { Banner, Field, PageHeader } from "../../components/ui";
import { api } from "../../lib/api";
import { formatCurrency, formatDate, todayInputValue } from "../../lib/format";

const PAYMENT_METHODS = [
  { value: "CASH", label: "Cash" },
  { value: "CARD", label: "Debit / Credit Card" },
  { value: "BANK_TRANSFER", label: "Bank Transfer" },
  { value: "ONLINE", label: "Online Gateway" },
];

export default function EnrolmentsPage() {
  const [students, setStudents] = useState([]);
  const [courses, setCourses] = useState([]);
  const [enrolments, setEnrolments] = useState([]);
  const [selectedEnrolment, setSelectedEnrolment] = useState(null);
  const [viewingReceipt, setViewingReceipt] = useState(null);
  const [latestCreatedPayment, setLatestCreatedPayment] = useState(null);

  // Single unified enrolment form: Enrolment + First Payment
  const [form, setForm] = useState(() => ({
    studentId: "",
    courseId: "",
    enrolledOn: todayInputValue(),
    paymentAmount: "",
    paymentMethod: "BANK_TRANSFER",
    paymentDate: todayInputValue(),
  }));

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  async function loadEnrolments() {
    const data = await api("/enrolments");
    setEnrolments(data);
  }

  async function loadInitialData() {
    try {
      const [studentRows, courseRows, enrolmentRows] = await Promise.all([
        api("/students"),
        api("/courses"),
        api("/enrolments"),
      ]);
      setStudents(studentRows);
      setCourses(courseRows);
      setEnrolments(enrolmentRows);
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadInitialData();
  }, []);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  const selectedCourse = courses.find((c) => String(c.id) === String(form.courseId));

  function setFullCourseFee() {
    if (selectedCourse) {
      setForm((current) => ({ ...current, paymentAmount: String(selectedCourse.courseFee) }));
    }
  }

  async function onSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");

    const payAmt = Number(form.paymentAmount);
    if (!payAmt || payAmt <= 0) {
      setError("First payment amount must be greater than zero.");
      setSaving(false);
      return;
    }

    if (selectedCourse && payAmt > Number(selectedCourse.courseFee)) {
      setError(
        `First payment amount (LKR ${formatCurrency(payAmt)}) exceeds course fee (LKR ${formatCurrency(
          selectedCourse.courseFee
        )}).`
      );
      setSaving(false);
      return;
    }

    try {
      const res = await api("/enrolments", {
        method: "POST",
        body: JSON.stringify({
          studentId: Number(form.studentId),
          courseId: Number(form.courseId),
          enrolledOn: form.enrolledOn,
          paymentAmount: payAmt,
          paymentMethod: form.paymentMethod,
          paymentDate: form.paymentDate,
        }),
      });

      const receiptNotice = res.latestReceiptNumber ? ` Receipt: ${res.latestReceiptNumber}.` : "";
      setSuccess(
        `Student successfully enrolled in ${res.courseCode}! First payment of LKR ${formatCurrency(
          payAmt
        )} recorded.${receiptNotice}`
      );

      // Fetch newly created payment for instant PDF download or e-bill dispatch
      try {
        const paymentsData = await api(`/enrolments/${res.id}/payments`);
        if (paymentsData && paymentsData.payments && paymentsData.payments.length > 0) {
          const newest = paymentsData.payments[paymentsData.payments.length - 1];
          setLatestCreatedPayment(newest);
        }
      } catch (err) {
        // Fallback placeholder object if payment fetch fails
        setLatestCreatedPayment({
          receiptNumber: res.latestReceiptNumber,
          amount: payAmt,
          paymentDate: form.paymentDate,
          paymentMethod: form.paymentMethod,
          studentName: res.studentName,
          studentNic: res.studentNic,
          studentEmail: res.studentEmail,
          courseCode: res.courseCode,
          courseName: res.courseName,
          courseFee: res.courseFee,
          totalPaid: res.totalPaid,
          balanceRemaining: res.balanceRemaining,
        });
      }

      // Reset form
      setForm({
        studentId: "",
        courseId: "",
        enrolledOn: todayInputValue(),
        paymentAmount: "",
        paymentMethod: "BANK_TRANSFER",
        paymentDate: todayInputValue(),
      });

      await loadEnrolments();
    } catch (saveError) {
      setError(saveError.message);
    } finally {
      setSaving(false);
    }
  }

  async function onDelete(enrolment) {
    if (
      !window.confirm(
        `Remove ${enrolment.studentName} from ${enrolment.courseCode}? Any associated payments will also be deleted.`
      )
    ) {
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
        eyebrow="Enrolment & Fees"
        title="Course Enrolment & Tuition Payments"
        text="Enrol a registered student onto a course and record their initial tuition payment."
      />
      <Banner error={error} success={success} />

      {latestCreatedPayment && (
        <div
          style={{
            background: "var(--ok-soft)",
            padding: "14px 18px",
            borderRadius: 12,
            marginBottom: 16,
            border: "1px solid #86efac",
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            flexWrap: "wrap",
            gap: 12,
          }}
        >
          <div>
            <strong style={{ color: "var(--leaf-dark)", fontSize: 14 }}>
              ✓ Official Bill & Receipt {latestCreatedPayment.receiptNumber} Generated!
            </strong>
            <div style={{ fontSize: 12, color: "#374151", marginTop: 3 }}>
              Student: <strong>{latestCreatedPayment.studentName}</strong> · Paid: LKR {formatCurrency(latestCreatedPayment.amount)} · Balance: LKR {formatCurrency(latestCreatedPayment.balanceRemaining)}
            </div>
          </div>
          <div style={{ display: "flex", gap: 8 }}>
            <button
              type="button"
              className="btn primary"
              style={{ fontSize: 12, padding: "6px 14px" }}
              onClick={() => setViewingReceipt(latestCreatedPayment)}
            >
              🖨️ PDF Bill / Print
            </button>
            <button
              type="button"
              className="btn ghost"
              style={{ fontSize: 12, padding: "6px 14px", background: "white", color: "var(--leaf-dark)", fontWeight: 700 }}
              onClick={() => setViewingReceipt(latestCreatedPayment)}
            >
              📧 Send E-Bill to Customer
            </button>
          </div>
        </div>
      )}

      <div className="stack">
        {/* Single Unified Enrolment Form */}
        <form className="card" onSubmit={onSubmit}>
          <h2 className="section-title">Enrol a student</h2>
          <p style={{ fontSize: 13, color: "var(--muted)", margin: "-4px 0 16px 0" }}>
            The first payment should be made to enroll a student, partially or fully.
          </p>

          <div className="form-grid">
            <Field label="Registered student">
              <select name="studentId" value={form.studentId} onChange={updateField} required>
                <option value="">Select a student</option>
                {students.map((student) => (
                  <option key={student.id} value={student.id}>
                    {student.name} · {student.nic}
                  </option>
                ))}
              </select>
            </Field>

            <Field label="Course">
              <select name="courseId" value={form.courseId} onChange={updateField} required>
                <option value="">Select a course</option>
                {courses.map((course) => (
                  <option key={course.id} value={course.id}>
                    {course.courseCode} · {course.name} (Fee: LKR {formatCurrency(course.courseFee)})
                  </option>
                ))}
              </select>
            </Field>

            <Field label="Enrolment date">
              <input name="enrolledOn" type="date" value={form.enrolledOn} onChange={updateField} required />
            </Field>

            <Field
              label={
                selectedCourse
                  ? `First payment (LKR) - Max: ${formatCurrency(selectedCourse.courseFee)}`
                  : "First payment (LKR)"
              }
            >
              <div style={{ display: "flex", gap: 6 }}>
                <input
                  name="paymentAmount"
                  type="number"
                  step="0.01"
                  min="0.01"
                  max={selectedCourse ? selectedCourse.courseFee : undefined}
                  value={form.paymentAmount}
                  onChange={updateField}
                  required
                  placeholder={selectedCourse ? `e.g. ${selectedCourse.courseFee / 2}` : "e.g. 50000.00"}
                />
                {selectedCourse && (
                  <button
                    type="button"
                    className="btn ghost"
                    onClick={setFullCourseFee}
                    style={{ fontSize: 12, whiteSpace: "nowrap", padding: "6px 10px" }}
                  >
                    Full fee
                  </button>
                )}
              </div>
            </Field>

            <Field label="Payment method">
              <select name="paymentMethod" value={form.paymentMethod} onChange={updateField} required>
                {PAYMENT_METHODS.map((m) => (
                  <option key={m.value} value={m.value}>
                    {m.label}
                  </option>
                ))}
              </select>
            </Field>

            <Field label="Payment date">
              <input name="paymentDate" type="date" value={form.paymentDate} onChange={updateField} required />
            </Field>
          </div>

          <div className="actions">
            <button
              className="btn primary"
              type="submit"
              disabled={saving || students.length === 0 || courses.length === 0}
            >
              {saving ? "Enrolling & processing payment…" : "Enrol student & process first payment"}
            </button>
          </div>
        </form>

        {/* Current Enrolments Table with Financial Status */}
        <section className="card">
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
            <h2 className="section-title" style={{ margin: 0 }}>Current enrolments & payment status</h2>
            <span style={{ fontSize: 13, color: "var(--muted)" }}>Total Enrolments: {enrolments.length}</span>
          </div>

          {loading ? <p className="empty">Loading enrolments…</p> : null}
          {!loading && enrolments.length === 0 ? <p className="empty">No enrolments yet.</p> : null}

          {enrolments.length > 0 ? (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Student</th>
                    <th>Course</th>
                    <th>Course Fee</th>
                    <th>Total Paid</th>
                    <th>Balance</th>
                    <th>Payment Status</th>
                    <th>Enrolled</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {enrolments.map((enrolment) => {
                    const balance = Number(enrolment.balanceRemaining ?? 0);
                    const paid = Number(enrolment.totalPaid ?? 0);
                    let statusLabel = "Unpaid";
                    let statusClass = "unpaid";
                    if (balance === 0) {
                      statusLabel = "Fully Paid";
                      statusClass = "paid";
                    } else if (paid > 0) {
                      statusLabel = "Partial";
                      statusClass = "partial";
                    }

                    return (
                      <tr key={enrolment.id}>
                        <td>
                          <strong>{enrolment.studentName}</strong>
                          <div style={{ fontSize: 12, color: "var(--muted)" }}>{enrolment.studentNic}</div>
                        </td>
                        <td>
                          <span className="pill">{enrolment.courseCode}</span>
                          <div style={{ fontSize: 12, marginTop: 2 }}>{enrolment.courseName}</div>
                        </td>
                        <td>LKR {formatCurrency(enrolment.courseFee)}</td>
                        <td style={{ color: "var(--leaf-dark)", fontWeight: 700 }}>
                          LKR {formatCurrency(enrolment.totalPaid)}
                        </td>
                        <td style={{ color: balance > 0 ? "var(--gold)" : "var(--leaf-dark)", fontWeight: 700 }}>
                          LKR {formatCurrency(balance)}
                        </td>
                        <td>
                          <span className={`badge ${statusClass}`}>{statusLabel}</span>
                        </td>
                        <td>{formatDate(enrolment.enrolledOn)}</td>
                        <td>
                          <div className="row-actions">
                            <button
                              className="btn ghost"
                              type="button"
                              onClick={() => setSelectedEnrolment(enrolment)}
                              style={{ color: "var(--leaf-dark)", fontWeight: 700 }}
                            >
                              💳 Payments
                            </button>
                            <button className="btn danger" type="button" onClick={() => onDelete(enrolment)}>
                              Remove
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : null}
        </section>
      </div>

      {/* Payment Modal for viewing history or making subsequent installment payments */}
      {selectedEnrolment && (
        <PaymentModal
          enrolment={selectedEnrolment}
          onClose={() => setSelectedEnrolment(null)}
          onPaymentRecorded={loadEnrolments}
        />
      )}

      {/* Official Receipt & E-Bill Modal */}
      {viewingReceipt && (
        <ReceiptModal
          payment={viewingReceipt}
          onClose={() => setViewingReceipt(null)}
        />
      )}
    </>
  );
}
