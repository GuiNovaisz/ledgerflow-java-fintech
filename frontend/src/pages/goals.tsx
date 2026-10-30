import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useTranslation } from "react-i18next";
import { Plus, Target, Pencil, Trash2, CalendarDays } from "lucide-react";
import { api } from "../lib/api";
import { cents, inputMoney, money, percentage, shortDate } from "../lib/money";
import type { Goal } from "../types";
import { useNotice } from "../state";
import { Button } from "../components/ui/button";
import { Dialog } from "../components/ui/dialog";
import {
  Empty,
  ErrorMessage,
  Field,
  Loading,
  PageTitle,
} from "../components/common";

export default function GoalsPage() {
  const { t, i18n } = useTranslation();
  const client = useQueryClient();
  const notice = useNotice();
  const [editing, setEditing] = useState<Goal | null | undefined>();
  const [deleting, setDeleting] = useState<Goal | null>(null);
  const goals = useQuery({
    queryKey: ["goals"],
    queryFn: () => api<Goal[]>("/goals"),
  });
  const remove = useMutation({
    mutationFn: (goal: Goal) =>
      api<void>(`/goals/${goal.id}?version=${goal.version}`, {
        method: "DELETE",
      }),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: ["goals"] });
      setDeleting(null);
      notice(t("goalDeleted"));
    },
  });
  return (
    <>
      <PageTitle
        title={t("financialGoals")}
        caption={t("goalsCaption")}
        action={
          <Button onClick={() => setEditing(null)}>
            <Plus size={17} />
            {t("newGoal")}
          </Button>
        }
      />
      <p className="info-callout planning-notice">
        <Target size={19} />
        {t("goalNotice")}
      </p>
      {goals.isPending ? (
        <Loading />
      ) : goals.isError ? (
        <ErrorMessage error={goals.error} />
      ) : !goals.data.length ? (
        <Empty title="emptyGoals" body="emptyGoalsBody" />
      ) : (
        <div className="goals-grid">
          {goals.data.map((goal, index) => (
            <article className="panel goal-card" key={goal.id}>
              <div className="goal-card-top">
                <span className={`goal-icon tone-${index % 4}`}>
                  <Target size={25} />
                </span>
                <span
                  className={`status-chip ${goal.status === "COMPLETED" ? "active" : "planned"}`}
                >
                  {t(goal.status === "COMPLETED" ? "completed" : "inProgress")}
                </span>
              </div>
              <h2>{goal.name}</h2>
              <div className="goal-values">
                <strong>{money(goal.currentAmount, i18n.language)}</strong>
                <span>
                  {t("of")} {money(goal.targetAmount, i18n.language)}
                </span>
              </div>
              <div className="progress-track">
                <span
                  style={{
                    width:
                      percentage(goal.currentAmount, goal.targetAmount) + "%",
                  }}
                />
              </div>
              <div className="goal-caption">
                <small>
                  <CalendarDays size={14} />
                  {goal.dueDate
                    ? shortDate(goal.dueDate, i18n.language)
                    : t("noDeadline")}
                </small>
                <strong>
                  {percentage(goal.currentAmount, goal.targetAmount).toFixed(0)}
                  %
                </strong>
              </div>
              <div className="goal-actions">
                <Button
                  size="small"
                  variant="secondary"
                  onClick={() => setEditing(goal)}
                >
                  <Pencil size={14} />
                  {t("edit")}
                </Button>
                <Button
                  size="small"
                  variant="ghost"
                  onClick={() => {
                    remove.reset();
                    setDeleting(goal);
                  }}
                >
                  <Trash2 size={14} />
                  {t("delete")}
                </Button>
              </div>
            </article>
          ))}
        </div>
      )}
      {editing !== undefined && (
        <GoalForm
          key={editing?.id || "new"}
          goal={editing}
          onClose={() => setEditing(undefined)}
        />
      )}
      <Dialog
        open={!!deleting}
        onOpenChange={(open) => {
          if (!open) setDeleting(null);
        }}
        title={t("deleteGoal")}
        locked={remove.isPending}
      >
        <p>{t("deleteGoalNotice")}</p>
        <ErrorMessage error={remove.error} />
        <div className="dialog-actions">
          <Button variant="secondary" onClick={() => setDeleting(null)}>
            {t("cancel")}
          </Button>
          <Button
            variant="danger"
            disabled={remove.isPending}
            onClick={() => {
              if (deleting) remove.mutate(deleting);
            }}
          >
            {t("delete")}
          </Button>
        </div>
      </Dialog>
    </>
  );
}
function GoalForm({
  goal,
  onClose,
}: {
  goal: Goal | null;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const client = useQueryClient();
  const notice = useNotice();
  const schema = z.object({
    name: z.string().trim().min(1, t("nameRule")).max(100, t("nameRule")),
    targetAmount: z.string().refine((value) => {
      const amount = inputMoney(value);
      return amount !== null && cents(amount) > 0n;
    }, t("amountRule")),
    currentAmount: z
      .string()
      .refine((value) => inputMoney(value) !== null, t("currentAmountRule")),
    dueDate: z
      .string()
      .refine(
        (value) => !value || /^\d{4}-\d{2}-\d{2}$/.test(value),
        t("dateInvalid"),
      ),
  });
  const form = useForm({
    resolver: zodResolver(schema),
    defaultValues: {
      name: goal?.name ?? "",
      targetAmount: goal?.targetAmount ?? "",
      currentAmount: goal?.currentAmount ?? "0.00",
      dueDate: goal?.dueDate ?? "",
    },
  });
  const save = useMutation({
    mutationFn: (values: z.infer<typeof schema>) =>
      api<Goal>(goal ? "/goals/" + goal.id : "/goals", {
        method: goal ? "PUT" : "POST",
        body: JSON.stringify({
          ...values,
          targetAmount: inputMoney(values.targetAmount),
          currentAmount: inputMoney(values.currentAmount),
          dueDate: values.dueDate || null,
          ...(goal ? { version: goal.version } : {}),
        }),
      }),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: ["goals"] });
      notice(t("goalSaved"));
      onClose();
    },
  });
  return (
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open) onClose();
      }}
      title={t(goal ? "editGoal" : "newGoal")}
      locked={save.isPending}
    >
      <form
        className="form-stack"
        onSubmit={form.handleSubmit((values) => save.mutate(values))}
      >
        <Field
          label={t("goalName")}
          id="goal-name"
          error={form.formState.errors.name?.message}
        >
          <input id="goal-name" {...form.register("name")} />
        </Field>
        <div className="form-grid">
          {(["targetAmount", "currentAmount"] as const).map((key) => (
            <Field
              key={key}
              label={t(key) + " (BRL)"}
              id={key}
              error={form.formState.errors[key]?.message}
            >
              <input id={key} inputMode="decimal" {...form.register(key)} />
            </Field>
          ))}
        </div>
        <Field label={t("dueDate") + " (" + t("optional") + ")"} id="due-date">
          <input id="due-date" type="date" {...form.register("dueDate")} />
        </Field>
        <p className="info-callout">{t("goalNotice")}</p>
        <ErrorMessage error={save.error} />
        <div className="dialog-actions">
          <Button
            type="button"
            variant="secondary"
            onClick={onClose}
            disabled={save.isPending}
          >
            {t("cancel")}
          </Button>
          <Button disabled={save.isPending}>{t("save")}</Button>
        </div>
      </form>
    </Dialog>
  );
}
