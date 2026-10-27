import "./globals.css";
import AppShell from "../components/AppShell";

export const metadata = {
  title: "Applebay Institute",
  description: "Manage courses, students, enrolment and attendance",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>
        <AppShell>{children}</AppShell>
      </body>
    </html>
  );
}
