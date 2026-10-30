import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useTranslation } from "react-i18next";
import { ArrowLeft, Copy, Plus, Wallet } from "lucide-react";
import { api } from "../lib/api";
import { money } from "../lib/money";
import type { Account } from "../types";
import { useNotice } from "../state";
import { Button } from "../components/ui/button";
import { Dialog } from "../components/ui/dialog";
import {
  AccountSummary,
  Empty,
  ErrorMessage,
  Field,
  Loading,
  PageTitle,
  Status,
} from "../components/common";
import { useOperation } from "../components/operation";
import TransactionsPage from "./transactions";

export default function AccountsPage() {
  const { t } = useTranslation();
  const client = useQueryClient();
  const notice = useNotice();
  const [open, setOpen] = useState(false);
  const accounts = useQuery({
    queryKey: ["accounts"],
    queryFn: () => api<Account[]>("/accounts"),
  });
  const schema = z.object({
    name: z.string().trim().min(1, t("nameRule")).max(100, t("nameRule")),
  });
  const form = useForm({
    resolver: zodResolver(schema),
    defaultValues: { name: "" },
  });
  const create = useMutation({
    mutationFn: (values: z.infer<typeof schema>) =>
      api<Account>("/accounts", {
        method: "POST",
        body: JSON.stringify(values),
      }),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: ["accounts"] });
      setOpen(false);
      form.reset();
      notice(t("accountSaved"));
    },
  });
  return (
    <>
      <PageTitle
        title={t("accounts")}
        caption={t("accountsCaption")}
        action={
          <Button
            onClick={() => {
              create.reset();
              setOpen(true);
            }}
          >
            <Plus size={17} />
            {t("createAccount")}
          </Button>
        }
      />
      {accounts.isPending ? (
        <Loading />
      ) : accounts.isError ? (
        <ErrorMessage error={accounts.error} />
      ) : accounts.data.length ? (
        <div className="accounts-grid">
          {accounts.data.map((account, index) => (
            <Link
              to={"/accounts/" + account.id}
              key={account.id}
              className="account-card"
            >
              <AccountSummary account={account} index={index} />
              <span className="account-link">{t("accountDetails")} ↗</span>
            </Link>
          ))}
        </div>
      ) : (
        <Empty title="emptyAccounts" body="emptyAccountsBody" />
      )}
      <Dialog
        open={open}
        onOpenChange={setOpen}
        title={t("createAccount")}
        locked={create.isPending}
      >
        <form
          className="form-stack"
          onSubmit={form.handleSubmit((values) => create.mutate(values))}
        >
          <Field
            label={t("accountName")}
            id="account-name"
            error={form.formState.errors.name?.message}
          >
            <input
              id="account-name"
              data-testid="account-name"
              autoFocus
              {...form.register("name")}
            />
          </Field>
          <p className="info-callout">{t("simulationNote")}</p>
          <ErrorMessage error={create.error} />
          <div className="dialog-actions">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setOpen(false)}
              disabled={create.isPending}
            >
              {t("cancel")}
            </Button>
            <Button
              data-testid="create-account-submit"
              disabled={create.isPending}
            >
              {t(create.isPending ? "processing" : "create")}
            </Button>
          </div>
        </form>
      </Dialog>
    </>
  );
}
export function AccountDetailPage() {
  const { id } = useParams();
  const { t, i18n } = useTranslation();
  const client = useQueryClient();
  const notice = useNotice();
  const launch = useOperation();
  const [confirm, setConfirm] = useState(false);
  const [copyError, setCopyError] = useState<unknown>();
  const account = useQuery({
    queryKey: ["account", id],
    queryFn: () => api<Account>("/accounts/" + id),
  });
  const change = useMutation({
    mutationFn: () =>
      api<Account>(`/accounts/${id}/status`, {
        method: "PATCH",
        body: JSON.stringify({
          status: account.data?.status === "ACTIVE" ? "INACTIVE" : "ACTIVE",
        }),
      }),
    onSuccess: async () => {
      await client.invalidateQueries();
      setConfirm(false);
      notice(t("statusUpdated"));
    },
  });
  if (account.isPending) return <Loading />;
  if (account.isError || !account.data)
    return <ErrorMessage error={account.error} />;
  const data = account.data;
  return (
    <>
      <Link className="back-link" to="/accounts">
        <ArrowLeft size={17} />
        {t("goBack")}
      </Link>
      <PageTitle title={data.name} caption={t("accountDetails")} />
      <section className="account-detail-card panel">
        <div className="detail-balance">
          <span className="stat-icon">
            <Wallet size={26} />
          </span>
          <div>
            <span>{t("balance")}</span>
            <strong>{money(data.balance, i18n.language)}</strong>
          </div>
          <Status active={data.status === "ACTIVE"} />
        </div>
        <div className="account-id">
          <span>{t("accountId")}</span>
          <code>{data.id}</code>
          <Button
            size="small"
            variant="secondary"
            onClick={() => {
              void navigator.clipboard
                .writeText(data.id)
                .then(() => notice(t("copied")))
                .catch(setCopyError);
            }}
          >
            <Copy size={14} />
            {t("copy")}
          </Button>
        </div>
        <ErrorMessage error={copyError} />
        <div className="detail-actions">
          <Button
            disabled={data.status !== "ACTIVE"}
            onClick={() => launch("TRANSFER", data.id)}
          >
            {t("newTransfer")}
          </Button>
          <Button
            variant="secondary"
            disabled={data.status !== "ACTIVE"}
            onClick={() => launch("DEPOSIT", data.id)}
          >
            {t("newDeposit")}
          </Button>
          <Button
            variant="secondary"
            disabled={data.status !== "ACTIVE"}
            onClick={() => launch("WITHDRAWAL", data.id)}
          >
            {t("newWithdrawal")}
          </Button>
          <Button
            variant="ghost"
            onClick={() => {
              change.reset();
              setConfirm(true);
            }}
          >
            {t(data.status === "ACTIVE" ? "deactivate" : "activate")}
          </Button>
        </div>
      </section>
      <TransactionsPage accountId={id} embedded />
      <Dialog
        open={confirm}
        onOpenChange={setConfirm}
        title={t("confirmStatus")}
        locked={change.isPending}
      >
        <p>{t("inactiveNotice")}</p>
        <ErrorMessage error={change.error} />
        <div className="dialog-actions">
          <Button variant="secondary" onClick={() => setConfirm(false)}>
            {t("cancel")}
          </Button>
          <Button onClick={() => change.mutate()} disabled={change.isPending}>
            {t(data.status === "ACTIVE" ? "deactivate" : "activate")}
          </Button>
        </div>
      </Dialog>
    </>
  );
}
