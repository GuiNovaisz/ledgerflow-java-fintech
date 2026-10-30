import {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  Check,
  CheckCircle2,
  Info,
  LoaderCircle,
  ShieldCheck,
} from "lucide-react";
import { useTranslation } from "react-i18next";
import { api, ApiError } from "../lib/api";
import { cents, inputMoney, money } from "../lib/money";
import {
  categories,
  type Account,
  type Category,
  type Operation,
  type OperationType,
} from "../types";
import { useSession } from "../state";
import { Dialog } from "./ui/dialog";
import { Button } from "./ui/button";
import { ErrorMessage, Field, Loading } from "./common";

interface Launch {
  type: OperationType;
  accountId?: string;
}
const OperationContext = createContext<
  (type: OperationType, accountId?: string) => void
>(() => {});
export const useOperation = () => useContext(OperationContext);
export function OperationProvider({ children }: { children: ReactNode }) {
  const [launch, setLaunch] = useState<Launch | null>(null);
  return (
    <OperationContext.Provider
      value={(type, accountId) => {
        if (!launch) setLaunch({ type, accountId });
      }}
    >
      {children}
      {launch && (
        <OperationModal
          key={launch.type}
          launch={launch}
          onClose={() => setLaunch(null)}
        />
      )}
    </OperationContext.Provider>
  );
}
interface Values {
  source: string;
  target: string;
  externalId: string;
  amount: string;
  category: Category;
  description: string;
}
interface RequestSnapshot {
  key: string;
  path: string;
  body: Record<string, string>;
  sourceName: string;
  targetName: string;
}
function OperationModal({
  launch,
  onClose,
}: {
  launch: Launch;
  onClose: () => void;
}) {
  const { t, i18n } = useTranslation();
  const { user } = useSession();
  const client = useQueryClient();
  const accounts = useQuery({
    queryKey: ["accounts"],
    queryFn: () => api<Account[]>("/accounts"),
    enabled: !!user,
  });
  const [step, setStep] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>();
  const [uncertain, setUncertain] = useState(false);
  const [checkingMessage, setCheckingMessage] = useState(false);
  const [result, setResult] = useState<Operation>();
  const [snapshot, setSnapshot] = useState<RequestSnapshot>();
  const submitting = useRef(false);
  const title = t(
    launch.type === "TRANSFER"
      ? "newTransfer"
      : launch.type === "DEPOSIT"
        ? "newDeposit"
        : "newWithdrawal",
  );
  const schema = z
    .object({
      source: z.string().min(1, t("required")),
      target: z.string(),
      externalId: z.string(),
      amount: z.string().refine((value) => {
        const parsed = inputMoney(value);
        return parsed !== null && cents(parsed) > 0n;
      }, t("amountRule")),
      category: z.enum(categories),
      description: z.string().max(255, t("descriptionRule")),
    })
    .superRefine((value, context) => {
      if (launch.type !== "TRANSFER") return;
      const target =
        value.target === "external" ? value.externalId : value.target;
      if (!z.uuid().safeParse(target).success)
        context.addIssue({
          code: "custom",
          path: [value.target === "external" ? "externalId" : "target"],
          message: t("invalidUuid"),
        });
      if (target === value.source)
        context.addIssue({
          code: "custom",
          path: ["target"],
          message: t("sameAccount"),
        });
    });
  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      source: launch.accountId ?? "",
      target: "",
      externalId: "",
      amount: "",
      category: launch.type === "DEPOSIT" ? "SALARY" : "OTHER",
      description: "",
    },
  });
  const target = useWatch({ control: form.control, name: "target" });
  useEffect(() => {
    if (!(busy || uncertain)) return;
    const block = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = "";
    };
    window.addEventListener("beforeunload", block);
    return () => window.removeEventListener("beforeunload", block);
  }, [busy, uncertain]);
  function prepare(values: Values) {
    const amount = inputMoney(values.amount)!;
    const destination =
      values.target === "external" ? values.externalId : values.target;
    const body: Record<string, string> =
      launch.type === "TRANSFER"
        ? {
            sourceAccountId: values.source,
            targetAccountId: destination,
            amount,
            category: values.category,
            description: values.description,
          }
        : {
            accountId: values.source,
            amount,
            category: values.category,
            description: values.description,
          };
    setSnapshot({
      key: crypto.randomUUID(),
      path:
        launch.type === "TRANSFER"
          ? "/transactions/transfers"
          : launch.type === "DEPOSIT"
            ? "/transactions/deposits"
            : "/transactions/withdrawals",
      body,
      sourceName:
        accounts.data?.find((account) => account.id === values.source)?.name ??
        values.source,
      targetName:
        accounts.data?.find((account) => account.id === destination)?.name ??
        destination,
    });
    setError(undefined);
    setStep(1);
  }
  async function complete(operation: Operation) {
    setResult(operation);
    setUncertain(false);
    setError(undefined);
    setStep(2);
    await client.invalidateQueries({
      predicate: (query) => query.queryKey[0] !== "session",
    });
  }
  async function submit() {
    if (!snapshot || submitting.current) return;
    submitting.current = true;
    setBusy(true);
    setError(undefined);
    setCheckingMessage(false);
    try {
      const operation = await api<Operation>(snapshot.path, {
        method: "POST",
        headers: { "Idempotency-Key": snapshot.key },
        body: JSON.stringify(snapshot.body),
      });
      await complete(operation);
    } catch (failure) {
      setError(failure);
      if (
        !(failure instanceof ApiError) ||
        failure.status === 0 ||
        failure.status >= 500
      ) {
        setUncertain(true);
      } else if (!uncertain) {
        setUncertain(false);
      }
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  }
  async function check() {
    if (!snapshot || submitting.current) return;
    submitting.current = true;
    setBusy(true);
    setError(undefined);
    setCheckingMessage(false);
    try {
      await complete(
        await api<Operation>(
          "/operations/by-key/" + encodeURIComponent(snapshot.key),
        ),
      );
    } catch (failure) {
      if (failure instanceof ApiError && failure.status === 404)
        setCheckingMessage(true);
      else setError(failure);
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  }
  return (
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open) onClose();
      }}
      title={title}
      locked={busy || uncertain}
    >
      <div data-testid="operation-modal">
        <div className="wizard-steps">
          {["operationData", "operationConfirmation", "operationResult"].map(
            (label, index) => (
              <div key={label} className={step >= index ? "current" : ""}>
                <span>{step > index ? <Check size={13} /> : index + 1}</span>
                <small>{t(label)}</small>
              </div>
            ),
          )}
        </div>
        {accounts.isPending ? (
          <Loading />
        ) : accounts.isError ? (
          <ErrorMessage error={accounts.error} />
        ) : step === 0 ? (
          <form onSubmit={form.handleSubmit(prepare)} className="form-stack">
            <Field
              label={t(
                launch.type === "TRANSFER" ? "sourceAccount" : "account",
              )}
              id="operation-source"
              error={form.formState.errors.source?.message}
            >
              <select
                id="operation-source"
                data-testid="operation-source"
                {...form.register("source")}
              >
                <option value="">{t("account")}</option>
                {accounts.data
                  ?.filter((account) => account.status === "ACTIVE")
                  .map((account) => (
                    <option key={account.id} value={account.id}>
                      {account.name} ({money(account.balance, i18n.language)})
                    </option>
                  ))}
              </select>
            </Field>
            {launch.type === "TRANSFER" && (
              <>
                <Field
                  label={t("targetAccount")}
                  id="operation-target"
                  error={form.formState.errors.target?.message}
                >
                  <select
                    id="operation-target"
                    data-testid="operation-target"
                    {...form.register("target")}
                  >
                    <option value="">{t("targetAccount")}</option>
                    {accounts.data
                      ?.filter((account) => account.status === "ACTIVE")
                      .map((account) => (
                        <option key={account.id} value={account.id}>
                          {account.name}
                        </option>
                      ))}
                    <option value="external">{t("otherAccount")}</option>
                  </select>
                </Field>
                {target === "external" && (
                  <Field
                    label={t("targetId")}
                    id="external-id"
                    error={form.formState.errors.externalId?.message}
                  >
                    <input
                      id="external-id"
                      {...form.register("externalId")}
                      placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                      autoComplete="off"
                    />
                  </Field>
                )}
              </>
            )}
            <div className="form-grid">
              <Field
                label={`${t("amount")} (BRL)`}
                id="operation-amount"
                error={form.formState.errors.amount?.message}
              >
                <input
                  id="operation-amount"
                  data-testid="operation-amount"
                  inputMode="decimal"
                  placeholder="0,00"
                  {...form.register("amount")}
                />
              </Field>
              <Field label={t("category")} id="operation-category">
                <select id="operation-category" {...form.register("category")}>
                  {categories.map((category) => (
                    <option key={category} value={category}>
                      {t(`categoryNames.${category}`)}
                    </option>
                  ))}
                </select>
              </Field>
            </div>
            <Field
              label={`${t("description")} (${t("optional")})`}
              id="operation-description"
              error={form.formState.errors.description?.message}
            >
              <input
                id="operation-description"
                data-testid="operation-description"
                maxLength={255}
                {...form.register("description")}
              />
            </Field>
            <p className="info-callout">
              <Info size={18} />
              {t(
                launch.type === "TRANSFER"
                  ? "transferNotice"
                  : "operationNotice",
              )}
            </p>
            <div className="dialog-actions">
              <Button type="button" variant="secondary" onClick={onClose}>
                {t("cancel")}
              </Button>
              <Button type="submit" data-testid="operation-continue">
                {t("continue")}
              </Button>
            </div>
          </form>
        ) : step === 1 && snapshot ? (
          <div className="form-stack">
            <div className="confirm-amount">
              <span>{t("amount")}</span>
              <strong>{money(snapshot.body.amount, i18n.language)}</strong>
            </div>
            <dl className="detail-list">
              <div>
                <dt>
                  {t(launch.type === "TRANSFER" ? "sourceAccount" : "account")}
                </dt>
                <dd>{snapshot.sourceName}</dd>
              </div>
              {launch.type === "TRANSFER" && (
                <div>
                  <dt>{t("targetAccount")}</dt>
                  <dd className="break-word">{snapshot.targetName}</dd>
                </div>
              )}
              <div>
                <dt>{t("category")}</dt>
                <dd>{t(`categoryNames.${snapshot.body.category}`)}</dd>
              </div>
              <div>
                <dt>{t("description")}</dt>
                <dd>{snapshot.body.description || "—"}</dd>
              </div>
            </dl>
            {uncertain && (
              <div className="warning-callout" role="status">
                <strong>{t("uncertainTitle")}</strong>
                <p>{t("uncertainBody")}</p>
                {checkingMessage && <p>{t("notFoundYet")}</p>}
              </div>
            )}
            <ErrorMessage error={error} />
            <p className="info-callout">
              <ShieldCheck size={18} />
              {t("operationNotice")}
            </p>
            <div className="dialog-actions">
              {uncertain ? (
                <>
                  <Button
                    type="button"
                    variant="secondary"
                    disabled={busy}
                    data-testid="operation-check"
                    onClick={() => void check()}
                  >
                    {t("checkStatus")}
                  </Button>
                  <Button
                    type="button"
                    disabled={busy}
                    data-testid="operation-retry"
                    onClick={() => void submit()}
                  >
                    {busy ? <LoaderCircle size={17} className="spin" /> : null}
                    {t("retrySame")}
                  </Button>
                </>
              ) : (
                <>
                  <Button
                    type="button"
                    variant="secondary"
                    disabled={busy}
                    onClick={() => setStep(0)}
                  >
                    {t("back")}
                  </Button>
                  <Button
                    type="button"
                    data-testid="operation-confirm"
                    disabled={busy}
                    onClick={() => void submit()}
                  >
                    {busy ? (
                      <>
                        <LoaderCircle size={17} className="spin" />
                        {t("processing")}
                      </>
                    ) : (
                      t("confirm")
                    )}
                  </Button>
                </>
              )}
            </div>
          </div>
        ) : result ? (
          <div className="operation-success">
            <span>
              <CheckCircle2 size={45} />
            </span>
            <h3>{t("operationSuccess")}</h3>
            <p>{t("operationSuccessBody")}</p>
            <strong>{money(result.amount, i18n.language)}</strong>
            <small>{t("operationId")}</small>
            <code>{result.id}</code>
            <Button data-testid="operation-done" onClick={onClose}>
              {t("done")}
            </Button>
          </div>
        ) : null}
      </div>
    </Dialog>
  );
}
