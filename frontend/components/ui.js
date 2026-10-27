export function PageHeader({ eyebrow, title, text }) {
  return (
    <header className="page-header">
      {eyebrow ? <p className="eyebrow">{eyebrow}</p> : null}
      <h1>{title}</h1>
      {text ? <p>{text}</p> : null}
    </header>
  );
}

export function Banner({ error, success }) {
  if (!error && !success) {
    return null;
  }
  return (
    <div className={error ? "banner error" : "banner success"} role="status">
      {error || success}
    </div>
  );
}

export function Field({ label, children, wide = false }) {
  return (
    <label className={wide ? "field wide" : "field"}>
      <span>{label}</span>
      {children}
    </label>
  );
}
