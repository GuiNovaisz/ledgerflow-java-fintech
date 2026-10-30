import { useTranslation } from "react-i18next";
import type { Entry } from "../types";
import { dateTime, money } from "../lib/money";
import { Dialog } from "./ui/dialog";
export function EntryDetail({
  entry,
  onClose,
}: {
  entry: Entry | null;
  onClose: () => void;
}) {
  const { t, i18n } = useTranslation();
  return (
    <Dialog
      open={!!entry}
      onOpenChange={(open) => {
        if (!open) onClose();
      }}
      title={t("details")}
    >
      {entry && (
        <dl className="detail-list">
          <div>
            <dt>{t("amount")}</dt>
            <dd
              className={entry.direction === "CREDIT" ? "positive" : "negative"}
            >
              {entry.direction === "CREDIT" ? "+" : "−"}
              {money(entry.amount, i18n.language)}
            </dd>
          </div>
          <div>
            <dt>{t("account")}</dt>
            <dd>{entry.accountName}</dd>
          </div>
          <div>
            <dt>{t("type")}</dt>
            <dd>{t(`typeNames.${entry.type}`)}</dd>
          </div>
          <div>
            <dt>{t("category")}</dt>
            <dd>{t(`categoryNames.${entry.category}`)}</dd>
          </div>
          <div>
            <dt>{t("description")}</dt>
            <dd>{entry.description || "—"}</dd>
          </div>
          <div>
            <dt>{t("date")}</dt>
            <dd>{dateTime(entry.createdAt, i18n.language)}</dd>
          </div>
          <div>
            <dt>{t("operationId")}</dt>
            <dd className="mono">{entry.operationId}</dd>
          </div>
        </dl>
      )}
    </Dialog>
  );
}
