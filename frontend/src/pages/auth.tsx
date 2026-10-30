import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import {
  ArrowRight,
  Check,
  ShieldCheck,
  Sparkles,
  LoaderCircle,
} from "lucide-react";
import { useTranslation } from "react-i18next";
import { api } from "../lib/api";
import type { User } from "../types";
import { useSession } from "../state";
import { Button } from "../components/ui/button";
import { ErrorMessage, Field, Loading } from "../components/common";
import { PreferenceControls, Logo } from "../components/layout";

export default function AuthPage() {
  const { t } = useTranslation();
  const { user, loading, establish, expired } = useSession();
  const location = useLocation();
  const navigate = useNavigate();
  const register = location.pathname === "/register";
  const [error, setError] = useState<unknown>();
  const schema = z.object({
    displayName: register
      ? z.string().trim().min(1, t("nameRule")).max(100, t("nameRule"))
      : z.string(),
    email: z.email(t("invalidEmail")).max(254, t("invalidEmail")),
    password: z.string().refine((value) => {
      const length = new TextEncoder().encode(value).length;
      return length >= 12 && length <= 72;
    }, t("passwordRule")),
  });
  const form = useForm({
    resolver: zodResolver(schema),
    defaultValues: { displayName: "", email: "", password: "" },
  });
  if (loading) return <Loading />;
  if (user) return <Navigate to="/dashboard" replace />;
  async function submit(values: z.infer<typeof schema>) {
    setError(undefined);
    try {
      const body = register
        ? values
        : { email: values.email, password: values.password };
      const identity = await api<User>(
        "/session/" + (register ? "register" : "login"),
        { method: "POST", body: JSON.stringify(body) },
      );
      await establish(identity);
      navigate("/dashboard", { replace: true });
    } catch (failure) {
      setError(failure);
    }
  }
  return (
    <main className="auth-page">
      <section className="auth-visual">
        <Logo />
        <div className="auth-visual-copy">
          <span className="eyebrow">
            <Sparkles size={15} />
            {t("tagline")}
          </span>
          <h1>{t("authQuote")}</h1>
          <p>{t("authIntro")}</p>
          <div className="auth-orbit" aria-hidden="true">
            <div className="orbit-ring one" />
            <div className="orbit-ring two" />
            <div className="orbit-center">
              LF<span>↗</span>
            </div>
            <span className="orbit-dot a" />
            <span className="orbit-dot b" />
          </div>
          <ul>
            {["authFeature1", "authFeature2", "authFeature3"].map((label) => (
              <li key={label}>
                <Check size={17} />
                {t(label)}
              </li>
            ))}
          </ul>
        </div>
        <p className="auth-footnote">
          <ShieldCheck size={17} />
          {t("simulationNote")}
        </p>
      </section>
      <section className="auth-form-side">
        <div className="auth-mobile-brand">
          <Logo />
        </div>
        <PreferenceControls />
        <div className="auth-form">
          <span className="eyebrow">
            {t("app")} / {t("simulation")}
          </span>
          <h2>{t(register ? "registerTitle" : "loginTitle")}</h2>
          <p>{t(register ? "registerSubtitle" : "loginSubtitle")}</p>
          {expired && (
            <p className="warning-callout" role="status">
              {t("sessionExpired")}
            </p>
          )}
          <form
            className="form-stack"
            onSubmit={form.handleSubmit(submit)}
            noValidate
          >
            {register && (
              <Field
                label={t("displayName")}
                id="display-name"
                error={form.formState.errors.displayName?.message}
              >
                <input
                  id="display-name"
                  autoComplete="name"
                  {...form.register("displayName")}
                  aria-invalid={!!form.formState.errors.displayName}
                />
              </Field>
            )}
            <Field
              label={t("email")}
              id="email"
              error={form.formState.errors.email?.message}
            >
              <input
                id="email"
                type="email"
                autoComplete="email"
                {...form.register("email")}
                aria-invalid={!!form.formState.errors.email}
              />
            </Field>
            <Field
              label={t("password")}
              id="password"
              error={form.formState.errors.password?.message}
              hint={register ? t("passwordRule") : undefined}
            >
              <input
                id="password"
                type="password"
                autoComplete={register ? "new-password" : "current-password"}
                {...form.register("password")}
                aria-invalid={!!form.formState.errors.password}
              />
            </Field>
            <ErrorMessage error={error} />
            <Button
              type="submit"
              data-testid="auth-submit"
              disabled={form.formState.isSubmitting}
            >
              {form.formState.isSubmitting ? (
                <LoaderCircle className="spin" size={18} />
              ) : (
                <ArrowRight size={18} />
              )}{" "}
              {t(register ? "register" : "login")}
            </Button>
          </form>
          <p className="auth-switch">
            {t(register ? "hasAccount" : "noAccount")}{" "}
            <Link
              to={register ? "/login" : "/register"}
              onClick={() => {
                setError(undefined);
                form.clearErrors();
              }}
            >
              {t(register ? "loginLink" : "registerLink")}
            </Link>
          </p>
          <p className="auth-local-notice">{t("simulationNote")}</p>
        </div>
      </section>
    </main>
  );
}
