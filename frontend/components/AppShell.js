"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const LINKS = [
  { href: "/", label: "Dashboard" },
  { href: "/courses", label: "Courses" },
  { href: "/students", label: "Students" },
  { href: "/enrolments", label: "Enrolment" },
  { href: "/attendance", label: "Mark attendance" },
  { href: "/attendance/records", label: "Attendance records" },
];

export default function AppShell({ children }) {
  const pathname = usePathname();

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">
          <div className="mark" aria-hidden="true">
            <svg viewBox="0 0 32 32" width="22" height="22">
              <path
                fill="currentColor"
                d="M16.2 4.4c1.8 3.6 1.6 5.8-.2 7.8-2.8 1.1-5.6 3.7-5.6 7.6 0 3.9 2.8 6.8 6.2 6.8s6.2-2.9 6.2-6.8c0-2-.6-3.6-1.5-5 1.3-2.4 2.9-5.8 2.6-8.2-2.2 1-4.8.9-7.7-2.2z"
              />
            </svg>
          </div>
          <div>
            <strong>Applebay</strong>
            <span>Institute</span>
          </div>
        </div>
        <nav className="nav">
          {LINKS.map((link) => (
            <Link key={link.href} href={link.href} className={pathname === link.href ? "active" : undefined}>
              {link.label}
            </Link>
          ))}
        </nav>
        <p className="sidebar-note">Courses, students and attendance share one PostgreSQL database.</p>
      </aside>
      <main className="main">{children}</main>
    </div>
  );
}
