"use client";

import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { formatCurrency, formatDate, todayInputValue } from "../lib/format";
import { Banner, Field } from "./ui";
import ReceiptModal from "./ReceiptModal";

const PAYMENT_METHODS = [
  { value: "CASH", label: "Cash" },
  { value: "CARD", label: "Debit / Credit Card" },
  { value: "BANK_TRANSFER", label: "Bank Transfer" },
  { value: "ONLINE", label: "Online Gateway" },
];

export default function PaymentModal({ enrolment, onClose, onPaymentRecorded }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [amount, setAmount] = useState("");
  const [paymentMethod, setPaymentMethod] = useState("CASH");
  const [paymentDate, setPaymentDate] = useState(() => todayInputValue());
  const [latestPayment, setLatestPayment] = useState(null);
  const [viewingReceipt, setViewingReceipt] = useState(null);

  async function loadData() {
    try {
      const res = await api(`/enrolments/${enrolment.id}/payments`);
      setData(res);
      if (res && res.balanceRemaining > 0) {
        setAmount("");
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, [enrolment.id]);

  function fillFullBalance() {
    if (data && data.balanceRemaining > 0) {
      setAmount(String(data.balanceRemaining));
    }
  }

  async function onRecordPayment(e) {
    e.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");
    setLatestPayment(null);

    const payVal = Number(amount);
    if (!payVal || payVal <= 0) {
      setError("Please enter a valid payment amount greater than zero.");
      setSaving(false);
      return;
    }

    if (data && payVal > data.balanceRemaining) {
      setError(`Payment amount (LKR ${formatCurrency(payVal)}) cannot exceed remaining balance (LKR ${formatCurrency(data.balanceRemaining)}).`);
      setSaving(false);
      return;
    }

    try {
      const paymentResponse = await api("/payments", {
        method: "POST",
        body: JSON.stringify({
          enrolmentId: enrolment.id,
          amount: payVal,
          paymentMethod,
          paymentDate,
        }),
      });

      setSuccess(`Payment recorded successfully! Receipt: ${paymentResponse.receiptNumber}`);
      setLatestPayment(paymentResponse);
      setAmount("");
      await loadData();
      if (onPaymentRecorded) {
        onPaymentRecorded();
      }
    } catch (payError) {
      setError(payError.message);
    } finally {
      setSaving(false);
    }
  }

  const balance = data ? Number(data.balanceRemaining) : Number(enrolment.balanceRemaining ?? 0);
  const fee = data ? Number(data.courseFee) : Number(enrolment.courseFee ?? 0);
  const paid = data ? Number(data.totalPaid) : Number(enrolment.totalPaid ?? 0);

  return (
    <>
      <div className="modal-backdrop" onClick={onClose}>
        <div className="modal" onClick={(e) => e.stopPropagation()}>
          <div className="modal-header">
            <div>
              <span className="pill">{enrolment.courseCode}</span>
              <h2 style={{ margin: "4px 0 0", fontSize: 20 }}>
                Payments · {enrolment.studentName}
              </h2>
              <p style={{ margin: "2px 0 0", color: "var(--muted)", fontSize: 13 }}>
                {enrolment.courseName} · Student NIC: {enrolment.studentNic}
              </p>
            </div>
            <button className="btn ghost" type="button" onClick={onClose} style={{ padding: "6px 12px" }}>
              ✕ Close
            </button>
          </div>

          <Banner error={error} success={success} />

          {/* Prompt to view/print/email newly generated receipt */}
          {latestPayment && (
            <div style={{ background: "var(--ok-soft)", padding: "12px 16px", borderRadius: 10, marginBottom: 16, display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 10 }}>
              <span style={{ color: "var(--leaf-dark)", fontWeight: 700, fontSize: 13 }}>
                ✓ Receipt <strong>{latestPayment.receiptNumber}</strong> generated!
              </span>
              <div style={{ display: "flex", gap: 8 }}>
                <button
                  type="button"
                  className="btn primary"
                  style={{ fontSize: 12, padding: "5px 12px" }}
                  onClick={() => setViewingReceipt(latestPayment)}
                >
                  🖨️ PDF Bill / Print
                </button>
                <button
                  type="button"
                  className="btn ghost"
                  style={{ fontSize: 12, padding: "5px 12px", background: "white" }}
                  onClick={() => setViewingReceipt(latestPayment)}
                >
                  📧 Send E-Bill
                </button>
              </div>
            </div>
          )}

          {/* Financial Stat Cards */}
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 10, marginBottom: 16 }}>
            <div style={{ background: "white", padding: 12, borderRadius: 10, border: "1px solid var(--line)" }}>
              <span style={{ fontSize: 11, color: "var(--muted)", textTransform: "uppercase", fontWeight: 700 }}>Course Fee</span>
              <div style={{ fontSize: 18, fontWeight: 700, marginTop: 4 }}>LKR {formatCurrency(fee)}</div>
            </div>
            <div style={{ background: "white", padding: 12, borderRadius: 10, border: "1px solid var(--line)" }}>
              <span style={{ fontSize: 11, color: "var(--muted)", textTransform: "uppercase", fontWeight: 700 }}>Total Paid</span>
              <div style={{ fontSize: 18, fontWeight: 700, color: "var(--leaf-dark)", marginTop: 4 }}>LKR {formatCurrency(paid)}</div>
            </div>
            <div style={{ background: "white", padding: 12, borderRadius: 10, border: "1px solid var(--line)" }}>
              <span style={{ fontSize: 11, color: "var(--muted)", textTransform: "uppercase", fontWeight: 700 }}>Remaining Balance</span>
              <div style={{ fontSize: 18, fontWeight: 700, color: balance > 0 ? "var(--gold)" : "var(--leaf-dark)", marginTop: 4 }}>
                LKR {formatCurrency(balance)}
              </div>
            </div>
          </div>

          {/* Record New Payment Form */}
          {balance > 0 ? (
            <form onSubmit={onRecordPayment} style={{ background: "white", padding: 16, borderRadius: 12, border: "1px solid var(--line)", marginBottom: 18 }}>
              <h3 style={{ margin: "0 0 12px", fontSize: 15, fontWeight: 700 }}>Record new installment</h3>
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                <Field label="Amount to pay (LKR)">
                  <div style={{ display: "flex", gap: 6 }}>
                    <input
                      type="number"
                      step="0.01"
                      min="0.01"
                      max={balance}
                      value={amount}
                      onChange={(e) => setAmount(e.target.value)}
                      placeholder={`Max ${formatCurrency(balance)}`}
                      required
                    />
                    <button type="button" className="btn ghost" onClick={fillFullBalance} style={{ fontSize: 12, whiteSpace: "nowrap", padding: "6px 10px" }}>
                      Full
                    </button>
                  </div>
                </Field>
                <Field label="Payment method">
                  <select value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)}>
                    {PAYMENT_METHODS.map((m) => (
                      <option key={m.value} value={m.value}>{m.label}</option>
                    ))}
                  </select>
                </Field>
                <Field label="Payment date">
                  <input type="date" value={paymentDate} onChange={(e) => setPaymentDate(e.target.value)} required />
                </Field>
                <div style={{ display: "flex", alignItems: "flex-end" }}>
                  <button className="btn primary" type="submit" disabled={saving || !amount} style={{ width: "100%" }}>
                    {saving ? "Processing…" : "Issue Receipt & Pay"}
                  </button>
                </div>
              </div>
            </form>
          ) : (
            <div style={{ background: "var(--ok-soft)", padding: 14, borderRadius: 10, marginBottom: 18, color: "var(--leaf-dark)", fontWeight: 600, fontSize: 14 }}>
              ✓ Course fee is fully settled for this enrolment.
            </div>
          )}

          {/* Past Payment Receipts Table */}
          <h3 style={{ margin: "0 0 8px", fontSize: 15, fontWeight: 700 }}>Receipt history</h3>
          {loading ? <p style={{ fontSize: 13, color: "var(--muted)" }}>Loading payments…</p> : null}
          {!loading && (!data || !data.payments || data.payments.length === 0) ? (
            <p style={{ fontSize: 13, color: "var(--muted)", margin: 0 }}>No payments recorded yet for this enrolment.</p>
          ) : null}
          {data && data.payments && data.payments.length > 0 ? (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Receipt #</th>
                    <th>Date</th>
                    <th>Method</th>
                    <th style={{ textAlign: "right" }}>Amount</th>
                    <th style={{ textAlign: "right" }}>Customer E-Bill</th>
                  </tr>
                </thead>
                <tbody>
                  {data.payments.map((p) => (
                    <tr key={p.id}>
                      <td><strong style={{ fontFamily: "monospace", color: "var(--leaf-dark)" }}>{p.receiptNumber}</strong></td>
                      <td>{formatDate(p.paymentDate)}</td>
                      <td><span className="pill">{p.paymentMethod}</span></td>
                      <td style={{ textAlign: "right", fontWeight: 700 }}>LKR {formatCurrency(p.amount)}</td>
                      <td style={{ textAlign: "right" }}>
                        <div style={{ display: "inline-flex", gap: 6 }}>
                          <button
                            type="button"
                            className="btn ghost"
                            style={{ fontSize: 12, padding: "4px 8px" }}
                            onClick={() => setViewingReceipt(p)}
                            title="View and download/print official PDF bill"
                          >
                            📄 View / PDF
                          </button>
                          <button
                            type="button"
                            className="btn ghost"
                            style={{ fontSize: 12, padding: "4px 8px", color: "var(--leaf-dark)", fontWeight: 700 }}
                            onClick={() => setViewingReceipt(p)}
                            title="Send electronic bill to customer's email"
                          >
                            📧 E-Bill
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </div>
      </div>

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
