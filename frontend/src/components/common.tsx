import {
  AlertCircle,
  ArrowDownLeft,
  ArrowUpRight,
  ArrowLeftRight,
  Inbox,
  LoaderCircle,
  Wallet,
  Landmark,
  PiggyBank,
  TrendingUp,
} from "lucide-react";
import { useTranslation } from "react-i18next";
import { useState, type ReactNode } from "react";
import { ApiError } from "../lib/api";
import { dateTime, money } from "../lib/money";
import type { Account, Entry, Period } from "../types";
import { Button } from "./ui/button";

export function errorKey(error: unknown) {
  if (!(error instanceof ApiError)) return "genericError";
  const codes: Record<string, string> = {
    invalid_credentials: "invalidCredentials",
    email_already_registered: "emailRegistered",
    INVALID_REQUEST: "validationError",
    invalid_password_length: "passwordRule",
    INSUFFICIENT_FUNDS: "insufficientFunds",
    ACCOUNT_INACTIVE: "inactiveAccount",
    INACTIVE_ACCOUNT: "inactiveAccount",
    IDEMPOTENCY_CONFLICT: "idempotencyConflict",
    TIMEOUT: "timeout",
    NETWORK: "networkError",
    EXPORT_ERROR: "exportError",
    GOAL_VERSION_CONFLICT: "conflictError",
    RATE_LIMITED: "rateLimited",
    RATE_LIMIT_EXCEEDED: "rateLimited",
  };
  return (
    codes[error.code] ??
    (error.status === 400
      ? "validationError"
      : error.status === 403
        ? "permissionError"
        : error.status === 404
          ? "permissionError"
          : error.status === 409
            ? "conflictError"
            : error.status === 429
              ? "rateLimited"
              : "genericError")
  );
}
export function ErrorMessage({ error }: { error: unknown }) {
  const { t } = useTranslation();
  return error ? (
    <p className="error-message" role="alert">
      <AlertCircle size={17} />
      {t(errorKey(error))}
    </p>
  ) : null;
}
export function Loading() {
  const { t } = useTranslation();
  return (
    <div className="loading-state" role="status">
      <LoaderCircle className="spin" size={24} />
      <span>{t("loading")}</span>
    </div>
  );
}
export function Empty({
  title,
  body,
  action,
}: {
  title: string;
  body?: string;
  action?: ReactNode;
}) {
  const { t } = useTranslation();
  return (
    <div className="empty-state">
      <span className="empty-icon">
        <Inbox size={27} />
      </span>
      <h3>{t(title)}</h3>
      {body && <p>{t(body)}</p>}
      {action}
    </div>
  );
}
export function Panel({
  title,
  caption,
  action,
  children,
  className = "",
}: {
  title: string;
  caption?: string;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={`panel ${className}`}>
      <header className="panel-heading">
        <div>
          <h2>{title}</h2>
          {caption && <p>{caption}</p>}
        </div>
        {action}
      </header>
      {children}
    </section>
  );
}
export function PageTitle({
  title,
  caption,
  action,
}: {
  title: string;
  caption?: string;
  action?: ReactNode;
}) {
  return (
    <div className="page-title">
      <div>
        <h1>{title}</h1>
        {caption && <p>{caption}</p>}
      </div>
      {action}
    </div>
  );
}
export function Field({
  label,
  id,
  error,
  children,
  hint,
}: {
  label: string;
  id: string;
  error?: string;
  children: ReactNode;
  hint?: string;
}) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {children}
      {hint && (
        <small id={`${id}-hint`} className="muted">
          {hint}
        </small>
      )}
      {error && (
        <small className="field-error" id={`${id}-error`} role="alert">
          {error}
        </small>
      )}
    </div>
  );
}
export function AccountIcon({ index = 0 }: { index?: number }) {
  const icons = [Wallet, PiggyBank, TrendingUp, Landmark];
  const Icon = icons[index % icons.length];
  return (
    <span className={`account-icon tone-${index % 4}`}>
      <Icon size={23} />
    </span>
  );
}
export function Status({ active }: { active: boolean }) {
  const { t } = useTranslation();
  return (
    <span className={`status-chip ${active ? "active" : "inactive"}`}>
      {t(active ? "active" : "inactive")}
    </span>
  );
}
export function EntryIcon({ entry }: { entry: Entry }) {
  return (
    <span
      className={`entry-icon ${entry.type === "TRANSFER" ? "transfer" : entry.direction === "CREDIT" ? "income" : "expense"}`}
    >
      {entry.type === "TRANSFER" ? (
        <ArrowLeftRight size={18} />
      ) : entry.direction === "CREDIT" ? (
        <ArrowDownLeft size={18} />
      ) : (
        <ArrowUpRight size={18} />
      )}
    </span>
  );
}
export function EntryList({
  entries,
  onSelect,
}: {
  entries: Entry[];
  onSelect?: (entry: Entry) => void;
}) {
  const { t, i18n } = useTranslation();
  return (
    <div className="entry-list">
      {entries.map((entry) => (
        <button
          key={entry.id}
          className="entry-row"
          type="button"
          onClick={() => onSelect?.(entry)}
          disabled={!onSelect}
        >
          <EntryIcon entry={entry} />
          <span className="entry-main">
            <strong>{entry.description || t(`typeNames.${entry.type}`)}</strong>
            <small>
              {t(`categoryNames.${entry.category}`)} · {entry.accountName}
            </small>
          </span>
          <span
            className={`entry-amount ${entry.direction === "CREDIT" ? "positive" : "negative"}`}
          >
            <strong>
              {entry.direction === "CREDIT" ? "+" : "−"}{" "}
              {money(entry.amount, i18n.language)}
            </strong>
            <small>{dateTime(entry.createdAt, i18n.language)}</small>
          </span>
        </button>
      ))}
    </div>
  );
}
export function AccountSummary({
  account,
  index = 0,
}: {
  account: Account;
  index?: number;
}) {
  const { t, i18n } = useTranslation();
  return (
    <>
      <AccountIcon index={index} />
      <h3>{account.name}</h3>
      <p>
        {t("simulation")} · {account.currency}
      </p>
      <div className="account-bottom">
        <strong>{money(account.balance, i18n.language)}</strong>
        <Status active={account.status === "ACTIVE"} />
      </div>
    </>
  );
}
export function calendarPeriod(days: number): Period {
  const to = new Date();
  to.setUTCHours(0, 0, 0, 0);
  to.setUTCDate(to.getUTCDate() + 1);
  const from = new Date(to);
  from.setUTCDate(from.getUTCDate() - days);
  return { from: from.toISOString(), to: to.toISOString() };
}
export function usePeriod() {
  return useState<Period>(() => calendarPeriod(30));
}
export function PeriodPicker({
  value,
  onChange,
}: {
  value: Period;
  onChange: (value: Period) => void;
}) {
  const { t } = useTranslation();
  const [preset, setPreset] = useState("30");
  const [start, setStart] = useState(value.from.slice(0, 10));
  const [end, setEnd] = useState(
    new Date(Date.parse(value.to) - 1).toISOString().slice(0, 10),
  );
  const [invalid, setInvalid] = useState(false);
  function apply() {
    const from = new Date(start + "T00:00:00Z");
    const to = new Date(end + "T00:00:00Z");
    to.setUTCDate(to.getUTCDate() + 1);
    const days = (to.getTime() - from.getTime()) / 86400000;
    if (
      !Number.isFinite(days) ||
      days <= 0 ||
      days > 366 ||
      from.getUTCFullYear() < 1970
    ) {
      setInvalid(true);
      return;
    }
    setInvalid(false);
    onChange({ from: from.toISOString(), to: to.toISOString() });
  }
  return (
    <div className="period-picker">
      <select
        aria-label={t("period")}
        value={preset}
        onChange={(event) => {
          setPreset(event.target.value);
          if (event.target.value !== "custom") {
            setInvalid(false);
            onChange(calendarPeriod(Number(event.target.value)));
          }
        }}
      >
        <option value="7">{t("last7")}</option>
        <option value="30">{t("last30")}</option>
        <option value="90">{t("last90")}</option>
        <option value="custom">{t("custom")}</option>
      </select>
      {preset === "custom" && (
        <div className="custom-period">
          <label>
            {t("from")}
            <input
              type="date"
              value={start}
              onChange={(event) => setStart(event.target.value)}
            />
          </label>
          <label>
            {t("to")}
            <input
              type="date"
              value={end}
              onChange={(event) => setEnd(event.target.value)}
            />
          </label>
          <Button size="small" onClick={apply}>
            {t("apply")}
          </Button>
          {invalid && (
            <small className="field-error" role="alert">
              {t("dateInvalid")}
            </small>
          )}
        </div>
      )}
    </div>
  );
}
