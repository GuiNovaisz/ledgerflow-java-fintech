// Monetary calculations stay in integer cents. Number is used only to draw charts.
export function cents(value: string | number): bigint {
  const raw = String(value);
  if (!/^-?\d+(\.\d{1,2})?$/.test(raw)) throw new Error("Invalid decimal");
  const negative = raw.startsWith("-");
  const [whole, fraction = ""] = raw.replace("-", "").split(".");
  return (
    (BigInt(whole) * 100n + BigInt(fraction.padEnd(2, "0"))) *
    (negative ? -1n : 1n)
  );
}
export function money(value: string | number, locale = "pt-BR"): string {
  const amount = cents(value);
  const negative = amount < 0n;
  const absolute = negative ? -amount : amount;
  const whole = absolute / 100n;
  const fraction = String(absolute % 100n).padStart(2, "0");
  const parts = new Intl.NumberFormat(locale, {
    style: "currency",
    currency: "BRL",
    minimumFractionDigits: 2,
  }).formatToParts(whole);
  return (
    (negative ? "−" : "") +
    parts
      .map((part) => (part.type === "fraction" ? fraction : part.value))
      .join("")
  );
}
export function percentage(current: string, target: string) {
  const denominator = cents(target);
  return denominator > 0n
    ? Math.min(100, Number((cents(current) * 10000n) / denominator) / 100)
    : 0;
}
export function inputMoney(value: string) {
  const normalized = value.trim().replace(",", ".");
  if (!/^\d{1,15}(\.\d{1,2})?$/.test(normalized)) return null;
  return normalized;
}
export function dateTime(value: string, locale = "pt-BR") {
  return new Intl.DateTimeFormat(locale, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
export function shortDate(value: string, locale = "pt-BR") {
  return new Intl.DateTimeFormat(locale, {
    day: "2-digit",
    month: "short",
    timeZone: "UTC",
  }).format(new Date(value));
}
export function lastDays(days: number) {
  const to = new Date();
  const from = new Date(to);
  from.setUTCDate(from.getUTCDate() - days);
  return { from: from.toISOString(), to: to.toISOString() };
}
