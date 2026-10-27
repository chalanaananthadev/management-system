"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { PageHeader } from "../components/ui";
import { api } from "../lib/api";
import { formatDate } from "../lib/format";

export default function DashboardPage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    Promise.all([api("/courses"), api("/students"), api("/enrolments")])
      .then(([courses, students, enrolments]) => {
        if (!cancelled) {
          setData({ courses, students, enrolments });
        }
      })
      .catch((loadError) => {
        if (!cancelled) {
          setError(loadError.message);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <>
      <PageHeader
        eyebrow="Applebay Institute"
        title="Student records"
        text="Keep courses, student registration, enrolment and attendance in one place. Every figure below is read from the database through the API."
      />
      {error ? <div className="banner error">{error}</div> : null}
      <section className="stats">
        <Link className="card stat" href="/courses">
          <span>Courses</span>
          <strong>{data ? data.courses.length : "–"}</strong>
          <em>View courses</em>
        </Link>
        <Link className="card stat" href="/students">
          <span>Students</span>
          <strong>{data ? data.students.length : "–"}</strong>
          <em>View students</em>
        </Link>
        <Link className="card stat" href="/enrolments">
          <span>Enrolments</span>
          <strong>{data ? data.enrolments.length : "–"}</strong>
          <em>View enrolments</em>
        </Link>
        <Link className="card stat" href="/attendance">
          <span>Attendance</span>
          <strong>Today</strong>
          <em>Mark a register</em>
        </Link>
      </section>
      <section className="card">
        <h2 className="section-title">Latest enrolments</h2>
        {!data ? <p className="empty">Loading records…</p> : null}
        {data && data.enrolments.length === 0 ? <p className="empty">No students are enrolled yet.</p> : null}
        {data && data.enrolments.length > 0 ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Student</th>
                  <th>Course</th>
                  <th>Enrolled</th>
                </tr>
              </thead>
              <tbody>
                {data.enrolments.slice(0, 5).map((enrolment) => (
                  <tr key={enrolment.id}>
                    <td>{enrolment.studentName}</td>
                    <td>{enrolment.courseCode} · {enrolment.courseName}</td>
                    <td>{formatDate(enrolment.enrolledOn)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>
    </>
  );
}
