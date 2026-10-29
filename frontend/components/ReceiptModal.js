"use client";

import { useState } from "react";
import { api } from "../lib/api";
import { formatCurrency, formatDate } from "../lib/format";
import { Banner } from "./ui";

export default function ReceiptModal({ payment, onClose }) {
  const [recipientEmail, setRecipientEmail] = useState(
    payment.studentEmail || ""
  );
  const [sending, setSending] = useState(false);
  const [sendSuccess, setSendSuccess] = useState("");
  const [sendError, setSendError] = useState("");

  const balance = Number(payment.balanceRemaining ?? 0);
  const isPaidInFull = balance === 0;

  async function onSendEmail(e) {
    if (e) e.preventDefault();
    if (!recipientEmail || !recipientEmail.includes("@")) {
      setSendError("Please provide a valid recipient email address.");
      return;
    }
    setSending(true);
    setSendError("");
    setSendSuccess("");
    try {
      const res = await api(`/payments/${payment.id}/email`, {
        method: "POST",
        body: JSON.stringify({ email: recipientEmail }),
      });
      setSendSuccess(
        `✓ Official E-Bill for receipt ${payment.receiptNumber} was successfully sent to ${recipientEmail}!`
      );
    } catch (err) {
      setSendError(err.message || "Failed to send e-bill email.");
    } finally {
      setSending(false);
    }
  }

  function onPrint() {
    window.print();
  }

  const mailtoSubject = encodeURIComponent(
    `[Applebay Institute] Official Payment Receipt - ${payment.receiptNumber} (${payment.courseCode})`
  );
  const mailtoBody = encodeURIComponent(
    `Dear ${payment.studentName},\n\n` +
      `Thank you for your payment to Applebay Institute of Higher Education.\n\n` +
      `--------------------------------------------------------\n` +
      `Receipt Number:        ${payment.receiptNumber}\n` +
      `Student Name:          ${payment.studentName} (NIC: ${payment.studentNic})\n` +
      `Course:                ${payment.courseCode} - ${payment.courseName}\n` +
      `Payment Date:          ${formatDate(payment.paymentDate)}\n` +
      `Payment Method:        ${payment.paymentMethod}\n` +
      `--------------------------------------------------------\n` +
      `Amount Paid This Bill: LKR ${formatCurrency(payment.amount)}\n` +
      `Course Total Fee:      LKR ${formatCurrency(payment.courseFee)}\n` +
      `Total Paid to Date:    LKR ${formatCurrency(payment.totalPaid)}\n` +
      `Remaining Balance:     LKR ${formatCurrency(payment.balanceRemaining)}\n` +
      `Status:                ${isPaidInFull ? "FULLY PAID" : "PARTIAL PAYMENT"}\n` +
      `--------------------------------------------------------\n\n` +
      `Applebay Institute Accounts Department\nadmissions@applebay.ac.lk`
  );
  const mailtoUrl = `mailto:${recipientEmail}?subject=${mailtoSubject}&body=${mailtoBody}`;

  return (
    <div className="modal-backdrop receipt-modal-backdrop" onClick={onClose}>
      <div
        className="modal receipt-modal"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: 760, padding: 0, overflow: "hidden" }}
      >
        {/* Top Control Bar (Hidden when printing/PDF exporting) */}
        <div className="receipt-actions-bar no-print">
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <button
              type="button"
              className="btn primary"
              onClick={onPrint}
              style={{ display: "inline-flex", alignItems: "center", gap: 6 }}
            >
              <span>🖨️</span>
              <strong>Download PDF / Print Bill</strong>
            </button>
            <a
              href={mailtoUrl}
              className="btn ghost"
              style={{ textDecoration: "none", fontSize: 13, padding: "8px 12px" }}
            >
              ✉️ Open in Mail Client
            </a>
          </div>
          <button
            type="button"
            className="btn ghost"
            onClick={onClose}
            style={{ padding: "6px 12px" }}
          >
            ✕ Close
          </button>
        </div>

        {/* E-Bill Email Dispatch Box (Hidden when printing) */}
        <div className="receipt-email-box no-print">
          <form onSubmit={onSendEmail} style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
            <span style={{ fontSize: 13, fontWeight: 700, color: "var(--ink)" }}>
              📧 Send E-Bill to Customer:
            </span>
            <input
              type="email"
              value={recipientEmail}
              onChange={(e) => setRecipientEmail(e.target.value)}
              placeholder="customer@email.com"
              required
              style={{
                flex: "1 1 220px",
                padding: "8px 12px",
                borderRadius: 8,
                border: "1px solid var(--line)",
                fontSize: 13,
                background: "white",
              }}
            />
            <button
              type="submit"
              className="btn"
              disabled={sending}
              style={{
                background: "var(--leaf-dark)",
                color: "white",
                fontWeight: 700,
                fontSize: 13,
                padding: "8px 14px",
              }}
            >
              {sending ? "Sending E-Bill…" : "Send E-Bill"}
            </button>
          </form>
          {sendSuccess ? (
            <div style={{ marginTop: 8, padding: "8px 12px", background: "var(--ok-soft)", color: "var(--leaf-dark)", borderRadius: 6, fontSize: 13, fontWeight: 600 }}>
              {sendSuccess}
            </div>
          ) : null}
          {sendError ? (
            <div style={{ marginTop: 8, padding: "8px 12px", background: "var(--danger-soft)", color: "var(--danger)", borderRadius: 6, fontSize: 13, fontWeight: 600 }}>
              {sendError}
            </div>
          ) : null}
        </div>

        {/* Printable Official Receipt / E-Bill Paper */}
        <div className="receipt-paper" id="receipt-print-area">
          {/* Institute Header */}
          <div className="receipt-head">
            <div className="receipt-brand">
              <div className="receipt-logo-mark">A</div>
              <div>
                <h1 className="receipt-institute-name">APPLEBAY INSTITUTE</h1>
                <p className="receipt-institute-sub">Higher Education & Professional Studies</p>
                <p className="receipt-institute-contact">
                  100 Galle Road, Colombo 03, Sri Lanka · admissions@applebay.ac.lk · +94 11 234 5678
                </p>
              </div>
            </div>
            <div className="receipt-meta">
              <span className="receipt-doc-title">PAYMENT RECEIPT / E-BILL</span>
              <div className="receipt-number">{payment.receiptNumber}</div>
              <div className="receipt-date">Date: {formatDate(payment.paymentDate)}</div>
              <div className={`receipt-stamp ${isPaidInFull ? "stamp-paid" : "stamp-partial"}`}>
                {isPaidInFull ? "✓ FULLY PAID" : "PARTIAL PAYMENT"}
              </div>
            </div>
          </div>

          <hr className="receipt-divider" />

          {/* Student & Course Details Grid */}
          <div className="receipt-grid">
            <div className="receipt-col">
              <span className="receipt-label">CUSTOMER / BILLED TO</span>
              <strong className="receipt-value-lg">{payment.studentName}</strong>
              <div className="receipt-sub-detail">NIC / ID: <strong>{payment.studentNic}</strong></div>
              <div className="receipt-sub-detail">Email: <strong>{payment.studentEmail || recipientEmail || "Not provided"}</strong></div>
            </div>
            <div className="receipt-col">
              <span className="receipt-label">COURSE & PAYMENT DETAILS</span>
              <strong className="receipt-value-lg">{payment.courseCode}</strong>
              <div className="receipt-sub-detail">{payment.courseName}</div>
              <div className="receipt-sub-detail">Payment Method: <strong>{payment.paymentMethod}</strong></div>
            </div>
          </div>

          {/* Financial Breakdown Table */}
          <div className="receipt-table-wrap">
            <table className="receipt-table">
              <thead>
                <tr>
                  <th>Description</th>
                  <th style={{ textAlign: "right" }}>Total Course Fee</th>
                  <th style={{ textAlign: "right" }}>Paid (This Receipt)</th>
                  <th style={{ textAlign: "right" }}>Total Paid to Date</th>
                  <th style={{ textAlign: "right" }}>Remaining Balance</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>
                    <strong>Tuition & Enrolment Fee</strong>
                    <div style={{ fontSize: 11, color: "#666" }}>
                      Academic Year 2026 · Course: {payment.courseCode}
                    </div>
                  </td>
                  <td style={{ textAlign: "right" }}>LKR {formatCurrency(payment.courseFee)}</td>
                  <td style={{ textAlign: "right", color: "var(--leaf-dark)", fontWeight: 700, fontSize: 15 }}>
                    LKR {formatCurrency(payment.amount)}
                  </td>
                  <td style={{ textAlign: "right" }}>LKR {formatCurrency(payment.totalPaid)}</td>
                  <td style={{ textAlign: "right", color: balance > 0 ? "var(--gold)" : "var(--leaf-dark)", fontWeight: 700 }}>
                    LKR {formatCurrency(balance)}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          {/* Summary Box */}
          <div className="receipt-summary-box">
            <div className="receipt-summary-text">
              <p>
                <strong>Official Academic Confirmation:</strong> This official computer-generated receipt confirms the payment recorded above for student <strong>{payment.studentName}</strong>.
              </p>
              <p style={{ marginTop: 4 }}>
                Please retain this receipt for student administration, exam admittance eligibility (minimum 80% attendance required), and future installment records.
              </p>
            </div>
            <div className="receipt-summary-total">
              <span style={{ fontSize: 11, textTransform: "uppercase", color: "#666", fontWeight: 700 }}>
                Amount Paid (This Transaction)
              </span>
              <div style={{ fontSize: 24, fontWeight: 800, color: "var(--leaf-dark)", marginTop: 2 }}>
                LKR {formatCurrency(payment.amount)}
              </div>
              <div style={{ fontSize: 12, marginTop: 4, color: balance > 0 ? "#b45309" : "#15803d", fontWeight: 600 }}>
                {balance > 0 ? `Balance Due: LKR ${formatCurrency(balance)}` : "All Dues Cleared"}
              </div>
            </div>
          </div>

          {/* Footer Note */}
          <div className="receipt-footer">
            <div>
              <p style={{ margin: 0, fontWeight: 600, color: "#333" }}>Applebay Institute Financial Accounts Office</p>
              <p style={{ margin: "2px 0 0", color: "#777" }}>Computer-generated e-bill. Valid without physical signature.</p>
            </div>
            <div style={{ textAlign: "right" }}>
              <div className="receipt-verified-badge">✓ SYSTEM VERIFIED</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

