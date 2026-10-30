import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  Area,
  AreaChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  ArrowDownToLine,
  ArrowLeftRight,
  ArrowUpFromLine,
  Download,
  Wallet,
  Target,
  TrendingUp,
  ArrowDownLeft,
  ArrowUpRight,
} from "lucide-react";
import { api, downloadCsv, queryString } from "../lib/api";
import { money, percentage, shortDate } from "../lib/money";
import type {
  Account,
  Dashboard as DashboardData,
  Entry,
  Goal,
} from "../types";
import { useNotice } from "../state";
import { useOperation } from "../components/operation";
import {
  AccountSummary,
  Empty,
  EntryList,
  ErrorMessage,
  Loading,
  Panel,
  PeriodPicker,
  usePeriod,
} from "../components/common";
import { EntryDetail } from "../components/entry-detail";
export const categoryColors = [
  "#2f91ff",
  "#00b7c8",
  "#31d89f",
  "#ffb648",
  "#fa6e7c",
  "#a285fa",
  "#da79e4",
  "#879ab0",
];
export default function DashboardPage() {
  const { t, i18n } = useTranslation();
  const [period, setPeriod] = usePeriod();
  const [selected, setSelected] = useState<Entry | null>(null);
  const [exportError, setExportError] = useState<unknown>();
  const [exporting, setExporting] = useState(false);
  const notice = useNotice();
  const openOperation = useOperation();
  const dashboard = useQuery({
    queryKey: ["dashboard", period],
    queryFn: ({ signal }) =>
      api<DashboardData>("/dashboard?" + queryString({ ...period }), {
        signal,
      }),
  });
  const accounts = useQuery({
    queryKey: ["accounts"],
    queryFn: () => api<Account[]>("/accounts"),
  });
  const goals = useQuery({
    queryKey: ["goals"],
    queryFn: () => api<Goal[]>("/goals"),
  });
  const data = dashboard.data;
  const series =
    data?.balanceSeries.map((point) => ({
      ...point,
      value: Number(point.balance),
    })) ?? [];
  const categoryData = Object.entries(data?.expensesByCategory ?? {}).map(
    ([name, amount]) => ({ name, amount: amount!, value: Number(amount) }),
  );
  async function exportStatement() {
    if (exporting) return;
    setExporting(true);
    setExportError(undefined);
    try {
      await downloadCsv(queryString({ ...period }));
      notice(t("exportReady"));
    } catch (failure) {
      setExportError(failure);
    } finally {
      setExporting(false);
    }
  }
  if (dashboard.isPending) return <Loading />;
  if (dashboard.isError || !data)
    return (
      <>
        <ErrorMessage error={dashboard.error} />
        <button className="text-link" onClick={() => void dashboard.refetch()}>
          {t("retry")}
        </button>
      </>
    );
  const stats = [
    {
      key: "totalBalance",
      value: money(data.totalBalance, i18n.language),
      caption: "available",
      icon: Wallet,
      tone: "green",
    },
    {
      key: "income",
      value: money(data.income, i18n.language),
      caption: "inPeriod",
      icon: ArrowDownLeft,
      tone: "green",
    },
    {
      key: "expenses",
      value: money(data.expenses, i18n.language),
      caption: "inPeriod",
      icon: ArrowUpRight,
      tone: "red",
    },
    {
      key: "transactionCount",
      value: new Intl.NumberFormat(i18n.language).format(data.transactionCount),
      caption: "realActivity",
      icon: ArrowLeftRight,
      tone: "blue",
    },
  ];
  return (
    <div className="dashboard-page">
      <div className="mobile-dashboard-heading">
        <div>
          <h1>{t("dashboard")}</h1>
          <p>{t("overview")}</p>
        </div>
      </div>
      <div className="dashboard-period">
        <span className="eyebrow">
          {t("simulation")} <span className="live-dot" />
        </span>
        <PeriodPicker value={period} onChange={setPeriod} />
      </div>
      <section className="stats-grid" aria-label={t("overview")}>
        {stats.map(({ key, value, caption, icon: Icon, tone }, index) => (
          <article className={`stat-card ${tone}`} key={key}>
            <span className="stat-icon">
              <Icon size={25} />
            </span>
            <div className="stat-text">
              <p>{t(key)}</p>
              <strong
                data-testid={
                  key === "totalBalance" ? "total-balance" : undefined
                }
              >
                {value}
              </strong>
              <small>{t(caption)}</small>
            </div>
            <div className="stat-visual" aria-hidden="true">
              {index === 0 ? (
                <TrendingUp size={43} />
              ) : (
                <>
                  <i />
                  <i />
                  <i />
                  <i />
                  <i />
                  <i />
                </>
              )}
            </div>
          </article>
        ))}
      </section>
      <div className="dashboard-main-grid">
        <Panel
          title={t("balanceEvolution")}
          caption={t("balanceCaption")}
          className="balance-panel"
        >
          <div
            className="balance-chart"
            role="img"
            aria-label={`${t("balanceEvolution")}: ${money(data.totalBalance, i18n.language)}`}
          >
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart
                data={series}
                margin={{ top: 18, right: 12, left: 2, bottom: 0 }}
              >
                <defs>
                  <linearGradient
                    id="balance-gradient"
                    x1="0"
                    y1="0"
                    x2="0"
                    y2="1"
                  >
                    <stop offset="0%" stopColor="#1685ff" stopOpacity={0.4} />
                    <stop offset="100%" stopColor="#1685ff" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis
                  dataKey="date"
                  tickFormatter={(value) => shortDate(value, i18n.language)}
                  minTickGap={36}
                  tickLine={false}
                  axisLine={false}
                />
                <YAxis
                  width={55}
                  tickFormatter={(value) =>
                    new Intl.NumberFormat(i18n.language, {
                      notation: "compact",
                      maximumFractionDigits: 1,
                    }).format(value)
                  }
                  tickLine={false}
                  axisLine={false}
                />
                <Tooltip
                  content={({ active, payload }) => {
                    const point = payload?.[0]?.payload as
                      { date: string; balance: string } | undefined;
                    return active && point ? (
                      <div className="chart-tooltip">
                        <strong>{money(point.balance, i18n.language)}</strong>
                        <span>{shortDate(point.date, i18n.language)}</span>
                      </div>
                    ) : null;
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="value"
                  stroke="#2f96ff"
                  fill="url(#balance-gradient)"
                  strokeWidth={2.5}
                  isAnimationActive={false}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
          <small className="chart-note">{t("utc")}</small>
        </Panel>
        <Panel
          title={t("categoryExpenses")}
          caption={t("categoryCaption")}
          className="category-panel"
        >
          {categoryData.length ? (
            <div className="donut-layout">
              <div
                className="donut-chart"
                role="img"
                aria-label={t("categoryExpenses")}
              >
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={categoryData}
                      innerRadius="69%"
                      outerRadius="91%"
                      dataKey="value"
                      paddingAngle={2}
                      stroke="none"
                      isAnimationActive={false}
                    >
                      {categoryData.map((item, index) => (
                        <Cell
                          key={item.name}
                          fill={categoryColors[index % categoryColors.length]}
                        />
                      ))}
                    </Pie>
                    <Tooltip
                      content={({ active, payload }) => {
                        const item = payload?.[0]?.payload as
                          { name: string; amount: string } | undefined;
                        return active && item ? (
                          <div className="chart-tooltip">
                            <span>{t(`categoryNames.${item.name}`)}</span>
                            <strong>{money(item.amount, i18n.language)}</strong>
                          </div>
                        ) : null;
                      }}
                    />
                  </PieChart>
                </ResponsiveContainer>
                <div className="donut-center">
                  <strong>{money(data.expenses, i18n.language)}</strong>
                  <small>{t("total")}</small>
                </div>
              </div>
              <ul className="category-legend">
                {categoryData.map((item, index) => (
                  <li key={item.name}>
                    <span
                      style={{
                        background:
                          categoryColors[index % categoryColors.length],
                      }}
                    />
                    <span>{t(`categoryNames.${item.name}`)}</span>
                    <strong>
                      {percentage(item.amount, data.expenses).toFixed(0)}%
                    </strong>
                  </li>
                ))}
              </ul>
            </div>
          ) : (
            <Empty title="emptyCategory" body="emptyCategoryBody" />
          )}
        </Panel>
        <Panel
          title={t("financialGoals")}
          caption={t("goalsCaption")}
          action={
            <Link className="text-link" to="/goals">
              {t("viewAll")}
            </Link>
          }
          className="goals-panel"
        >
          {goals.isPending ? (
            <Loading />
          ) : goals.isError ? (
            <ErrorMessage error={goals.error} />
          ) : goals.data?.length ? (
            <div className="compact-goals">
              {goals.data.slice(0, 3).map((goal, index) => (
                <Link to="/goals" className="compact-goal" key={goal.id}>
                  <span className={`goal-icon tone-${index % 4}`}>
                    <Target size={24} />
                  </span>
                  <div>
                    <strong>{goal.name}</strong>
                    <small>
                      {money(goal.currentAmount, i18n.language)} /{" "}
                      {money(goal.targetAmount, i18n.language)}
                    </small>
                    <div className="progress-track">
                      <span
                        style={{
                          width: `${percentage(goal.currentAmount, goal.targetAmount)}%`,
                        }}
                      />
                    </div>
                  </div>
                  <small className="goal-percent">
                    {percentage(goal.currentAmount, goal.targetAmount).toFixed(
                      0,
                    )}
                    %
                  </small>
                </Link>
              ))}
            </div>
          ) : (
            <Empty
              title="emptyGoals"
              action={
                <Link to="/goals" className="text-link">
                  {t("newGoal")}
                </Link>
              }
            />
          )}
        </Panel>
      </div>
      <div className="dashboard-bottom-grid">
        <Panel
          title={t("myAccounts")}
          caption={t("accountsCaption")}
          action={
            <Link to="/accounts" className="text-link">
              {t("viewAll")}
            </Link>
          }
        >
          {accounts.isPending ? (
            <Loading />
          ) : accounts.isError ? (
            <ErrorMessage error={accounts.error} />
          ) : accounts.data?.length ? (
            <div className="mini-accounts">
              {accounts.data.slice(0, 3).map((account, index) => (
                <Link
                  className="mini-account"
                  key={account.id}
                  to={"/accounts/" + account.id}
                >
                  <AccountSummary account={account} index={index} />
                </Link>
              ))}
            </div>
          ) : (
            <Empty
              title="emptyAccounts"
              action={
                <Link to="/accounts" className="text-link">
                  {t("createAccount")}
                </Link>
              }
            />
          )}
        </Panel>
        <Panel
          title={t("recentTransactions")}
          action={
            <Link to="/transactions" className="text-link">
              {t("viewAll")}
            </Link>
          }
        >
          {data.recentEntries.length ? (
            <EntryList
              entries={data.recentEntries.slice(0, 4)}
              onSelect={setSelected}
            />
          ) : (
            <Empty title="emptyTransactions" body="emptyTransactionsBody" />
          )}
        </Panel>
        <Panel title={t("quickActions")} caption={t("quickCaption")}>
          <div className="quick-grid">
            <button
              aria-label={t("newTransfer")}
              onClick={() => openOperation("TRANSFER")}
            >
              <span className="quick-icon blue">
                <ArrowLeftRight size={21} />
              </span>
              <span>
                <strong>{t("newTransfer")}</strong>
                <small>{t("sendMoney")}</small>
              </span>
            </button>
            <button
              aria-label={t("newDeposit")}
              onClick={() => openOperation("DEPOSIT")}
            >
              <span className="quick-icon green">
                <ArrowDownToLine size={21} />
              </span>
              <span>
                <strong>{t("newDeposit")}</strong>
                <small>{t("addFunds")}</small>
              </span>
            </button>
            <button
              aria-label={t("newWithdrawal")}
              onClick={() => openOperation("WITHDRAWAL")}
            >
              <span className="quick-icon red">
                <ArrowUpFromLine size={21} />
              </span>
              <span>
                <strong>{t("newWithdrawal")}</strong>
                <small>{t("removeFunds")}</small>
              </span>
            </button>
            <button
              aria-label={t("export")}
              disabled={exporting}
              onClick={() => void exportStatement()}
            >
              <span className="quick-icon blue">
                <Download size={21} />
              </span>
              <span>
                <strong>{t("export")}</strong>
                <small>{t("downloadCsv")}</small>
              </span>
            </button>
          </div>
          <ErrorMessage error={exportError} />
        </Panel>
      </div>
      <p className="muted report-explanation">{t("reportNote")}</p>
      <EntryDetail entry={selected} onClose={() => setSelected(null)} />
    </div>
  );
}
