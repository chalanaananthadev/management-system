export function todayInputValue() {
  const now = new Date();
  const local = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

export function formatDate(value) {
  if (!value) {
    return "";
  }
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day).toLocaleDateString("en-GB", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

export function formatPercent(value) {
  if (value === null || value === undefined) {
    return "No records";
  }
  return `${Number(value).toFixed(1)}%`;
}

export function formatCurrency(value) {
  if (value === null || value === undefined) {
    return "0.00";
  }
  return Number(value).toLocaleString("en-LK", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}
