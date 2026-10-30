import { useState, useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import {
  Download,
  Search,
  ChevronLeft,
  ChevronRight,
  Plus,
} from "lucide-react";
import { api, downloadCsv, queryString } from "../lib/api";
import { dateTime, money } from "../lib/money";
import { categories, type Account, type Entry, type Page } from "../types";
import { useNotice } from "../state";
import { Button } from "../components/ui/button";
import {
  Empty,
  EntryIcon,
  EntryList,
  ErrorMessage,
  Loading,
  PageTitle,
  PeriodPicker,
  usePeriod,
} from "../components/common";
import { EntryDetail } from "../components/entry-detail";
import { useOperation } from "../components/operation";

export default function TransactionsPage({
  accountId,
  embedded = false,
  reports = false,
}: { accountId?: string; embedded?: boolean; reports?: boolean } = {}) {
  const { t, i18n } = useTranslation();
  const [params] = useSearchParams();
  const [query, setQuery] = useState(params.get("q") || "");
  const [search, setSearch] = useState(query);
  const [account, setAccount] = useState(accountId || "");
  const [category, setCategory] = useState(params.get("category") || "");
  const [type, setType] = useState("");
  const [direction, setDirection] = useState("");
  const [period, setPeriod] = usePeriod();
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Entry | null>(null);
  const [exportError, setExportError] = useState<unknown>();
  const [exporting, setExporting] = useState(false);
  const notice = useNotice();
  const launch = useOperation();
  useEffect(() => {
    setQuery(params.get("q") || "");
    setCategory(params.get("category") || "");
    setPage(0);
  }, [params]);
  useEffect(() => {
    const timer = setTimeout(() => {
      setSearch(query);
      setPage(0);
    }, 250);
    return () => clearTimeout(timer);
  }, [query]);
  const filters = {
    ...period,
    accountId: accountId || account,
    q: search,
    category,
    type,
    direction,
  };
  const entries = useQuery({
    queryKey: ["entries", filters, page],
    queryFn: ({ signal }) =>
      api<Page<Entry>>(
        "/entries?" + queryString({ ...filters, page, size: 10 }),
        { signal },
      ),
  });
  const accounts = useQuery({
    queryKey: ["accounts"],
    queryFn: () => api<Account[]>("/accounts"),
  });
  async function exportStatement() {
    if (exporting) return;
    setExporting(true);
    setExportError(undefined);
    try {
      await downloadCsv(queryString(filters));
      notice(t("exportReady"));
    } catch (error) {
      setExportError(error);
    } finally {
      setExporting(false);
    }
  }
  function update(setter: (value: string) => void, value: string) {
    setter(value);
    setPage(0);
  }
  return (
    <section className="transactions-page">
      {!embedded && (
        <PageTitle
          title={t(reports ? "reports" : "transactions")}
          caption={t(reports ? "reportsCaption" : "recentCaption")}
          action={
            <Button onClick={() => launch("TRANSFER")}>
              <Plus size={17} />
              {t("newTransfer")}
            </Button>
          }
        />
      )}
      <div className="panel statement-panel">
        <div className="statement-heading">
          <h2>{t(embedded ? "accountActivity" : "entries")}</h2>
          <div>
            <PeriodPicker
              value={period}
              onChange={(value) => {
                setPeriod(value);
                setPage(0);
              }}
            />
            <Button
              variant="secondary"
              size="small"
              onClick={() => void exportStatement()}
              disabled={exporting}
            >
              <Download size={16} />
              {t("export")}
            </Button>
          </div>
        </div>
        <div className="filters">
          <label className="search-field">
            <Search size={17} />
            <span className="sr-only">{t("searchLabel")}</span>
            <input
              type="search"
              placeholder={t("search")}
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              maxLength={100}
            />
          </label>
          {!accountId && (
            <select
              aria-label={t("account")}
              value={account}
              onChange={(event) => update(setAccount, event.target.value)}
            >
              <option value="">{t("allAccounts")}</option>
              {accounts.data?.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.name}
                </option>
              ))}
            </select>
          )}
          <select
            aria-label={t("category")}
            value={category}
            onChange={(event) => update(setCategory, event.target.value)}
          >
            <option value="">{t("allCategories")}</option>
            {categories.map((item) => (
              <option key={item} value={item}>
                {t(`categoryNames.${item}`)}
              </option>
            ))}
          </select>
          <select
            aria-label={t("type")}
            value={type}
            onChange={(event) => update(setType, event.target.value)}
          >
            <option value="">{t("allTypes")}</option>
            {["DEPOSIT", "WITHDRAWAL", "TRANSFER"].map((item) => (
              <option key={item} value={item}>
                {t(`typeNames.${item}`)}
              </option>
            ))}
          </select>
          <select
            aria-label={t("direction")}
            value={direction}
            onChange={(event) => update(setDirection, event.target.value)}
          >
            <option value="">{t("all")}</option>
            <option value="CREDIT">{t("credit")}</option>
            <option value="DEBIT">{t("debit")}</option>
          </select>
        </div>
        <ErrorMessage error={exportError} />
        {entries.isPending ? (
          <Loading />
        ) : entries.isError ? (
          <>
            <ErrorMessage error={entries.error} />
            <Button onClick={() => void entries.refetch()}>{t("retry")}</Button>
          </>
        ) : !entries.data.content.length ? (
          <Empty title="emptyTransactions" body="emptyTransactionsBody" />
        ) : (
          <>
            <div className="table-scroll">
              <table className="transaction-table">
                <thead>
                  <tr>
                    {[
                      "description",
                      "account",
                      "category",
                      "date",
                      "amount",
                      "details",
                    ].map((label) => (
                      <th key={label}>{t(label)}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {entries.data.content.map((entry) => (
                    <tr key={entry.id}>
                      <td>
                        <div className="table-description">
                          <EntryIcon entry={entry} />
                          <span>
                            <strong>
                              {entry.description ||
                                t(`typeNames.${entry.type}`)}
                            </strong>
                            <small>{t(`typeNames.${entry.type}`)}</small>
                          </span>
                        </div>
                      </td>
                      <td>{entry.accountName}</td>
                      <td>
                        <span className="category-tag">
                          {t(`categoryNames.${entry.category}`)}
                        </span>
                      </td>
                      <td className="table-date">
                        {dateTime(entry.createdAt, i18n.language)}
                      </td>
                      <td
                        className={
                          entry.direction === "CREDIT" ? "positive" : "negative"
                        }
                      >
                        {entry.direction === "CREDIT" ? "+" : "−"}{" "}
                        {money(entry.amount, i18n.language)}
                      </td>
                      <td>
                        <Button
                          variant="ghost"
                          size="small"
                          onClick={() => setSelected(entry)}
                          aria-label={
                            t("details") +
                            ": " +
                            (entry.description || t(`typeNames.${entry.type}`))
                          }
                        >
                          ↗
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="mobile-entries">
              <EntryList
                entries={entries.data.content}
                onSelect={setSelected}
              />
            </div>
            <div className="pagination">
              <small>
                {t("results", { count: entries.data.totalElements })}
              </small>
              <div>
                <Button
                  size="small"
                  variant="secondary"
                  disabled={page === 0}
                  onClick={() => setPage(page - 1)}
                  aria-label={t("previous")}
                >
                  <ChevronLeft size={16} />
                </Button>
                <span>
                  {t("page", {
                    page: page + 1,
                    total: Math.max(1, entries.data.totalPages),
                  })}
                </span>
                <Button
                  size="small"
                  variant="secondary"
                  disabled={page + 1 >= entries.data.totalPages}
                  onClick={() => setPage(page + 1)}
                  aria-label={t("next")}
                >
                  <ChevronRight size={16} />
                </Button>
              </div>
            </div>
          </>
        )}
        <p className="table-note">
          {t("utc")} · {t("reportNote")}
        </p>
      </div>
      <EntryDetail entry={selected} onClose={() => setSelected(null)} />
    </section>
  );
}
export function ReportsPage() {
  return <TransactionsPage reports />;
}
