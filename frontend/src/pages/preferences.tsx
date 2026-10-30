import { useState } from "react";
import { useTranslation } from "react-i18next";
import { LogOut, ShieldCheck, UserRound } from "lucide-react";
import { useSession } from "../state";
import { PreferenceControls } from "../components/layout";
import { ErrorMessage, PageTitle, Panel } from "../components/common";
import { Button } from "../components/ui/button";
export default function PreferencesPage() {
  const { t } = useTranslation();
  const { user, logout } = useSession();
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  return (
    <>
      <PageTitle title={t("preferences")} caption={t("profileCaption")} />
      <div className="preferences-grid">
        <Panel title={t("personalInfo")} caption={t("readOnlyProfile")}>
          <span className="profile-icon">
            <UserRound size={30} />
          </span>
          <dl className="detail-list">
            <div>
              <dt>{t("displayName")}</dt>
              <dd>{user?.displayName}</dd>
            </div>
            <div>
              <dt>{t("email")}</dt>
              <dd>{user?.email}</dd>
            </div>
          </dl>
        </Panel>
        <Panel title={t("appearance")} caption={t("appearanceCaption")}>
          <PreferenceControls />
          <p className="muted">{t("languageCaption")}</p>
        </Panel>
        <Panel title={t("logout")} caption={t("logoutCaption")}>
          <p className="info-callout">
            <ShieldCheck size={21} />
            {t("securityCaption")}
          </p>
          <ErrorMessage error={error} />
          <Button
            variant="secondary"
            disabled={busy}
            onClick={() => {
              setBusy(true);
              void logout()
                .catch(setError)
                .finally(() => setBusy(false));
            }}
          >
            <LogOut size={17} />
            {t("logout")}
          </Button>
        </Panel>
      </div>
    </>
  );
}
