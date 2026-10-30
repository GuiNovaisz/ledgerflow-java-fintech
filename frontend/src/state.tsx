import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { api, clearCsrf, onSessionExpired, refreshCsrf } from "./lib/api";
import type { User } from "./types";

type Theme = "dark" | "light" | "system";
const PreferenceContext = createContext<{
  theme: Theme;
  setTheme: (theme: Theme) => void;
}>({ theme: "system", setTheme: () => {} });
export function PreferencesProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>(() => {
    try {
      const value = localStorage.getItem("ledgerflow-theme");
      return value === "light" || value === "dark" ? value : "system";
    } catch {
      return "system";
    }
  });
  const { i18n } = useTranslation();
  useEffect(() => {
    const media = matchMedia("(prefers-color-scheme: dark)");
    const apply = () => {
      document.documentElement.dataset.theme =
        theme === "system" ? (media.matches ? "dark" : "light") : theme;
    };
    apply();
    media.addEventListener("change", apply);
    try {
      localStorage.setItem("ledgerflow-theme", theme);
    } catch {
      /* Preferences are optional. */
    }
    return () => media.removeEventListener("change", apply);
  }, [theme]);
  useEffect(() => {
    document.documentElement.lang = i18n.language;
    try {
      localStorage.setItem("ledgerflow-language", i18n.language);
    } catch {
      /* Preferences are optional. */
    }
  }, [i18n.language]);
  return (
    <PreferenceContext.Provider value={{ theme, setTheme }}>
      {children}
    </PreferenceContext.Provider>
  );
}
export const usePreferences = () => useContext(PreferenceContext);

const SessionContext = createContext<{
  user: User | null;
  loading: boolean;
  expired: boolean;
  establish: (user: User) => Promise<void>;
  logout: () => Promise<void>;
}>({
  user: null,
  loading: true,
  expired: false,
  establish: async () => {},
  logout: async () => {},
});
export function SessionProvider({ children }: { children: ReactNode }) {
  const client = useQueryClient();
  const [expired, setExpired] = useState(false);
  const session = useQuery({
    queryKey: ["session"],
    queryFn: async () => {
      try {
        return await api<User>("/session/me");
      } catch {
        return null;
      }
    },
    staleTime: 60000,
    retry: false,
  });
  useEffect(
    () =>
      onSessionExpired(() => {
        setExpired(true);
        void client.cancelQueries();
        client.removeQueries({
          predicate: (query) => query.queryKey[0] !== "session",
        });
        client.getMutationCache().clear();
        client.setQueryData(["session"], null);
      }),
    [client],
  );
  async function establish(user: User) {
    clearCsrf();
    await refreshCsrf();
    await client.cancelQueries();
    client.removeQueries({
      predicate: (query) => query.queryKey[0] !== "session",
    });
    client.getMutationCache().clear();
    client.setQueryData(["session"], user);
    setExpired(false);
  }
  async function logout() {
    await api<void>("/session/logout", { method: "POST" });
    clearCsrf();
    await client.cancelQueries();
    client.removeQueries({
      predicate: (query) => query.queryKey[0] !== "session",
    });
    client.getMutationCache().clear();
    client.setQueryData(["session"], null);
    setExpired(false);
  }
  return (
    <SessionContext.Provider
      value={{
        user: session.data ?? null,
        loading: session.isPending,
        expired,
        establish,
        logout,
      }}
    >
      {children}
    </SessionContext.Provider>
  );
}
export const useSession = () => useContext(SessionContext);

const NoticeContext = createContext<(text: string) => void>(() => {});
export function NoticeProvider({ children }: { children: ReactNode }) {
  const [notice, setNotice] = useState("");
  useEffect(() => {
    if (!notice) return;
    const timer = setTimeout(() => setNotice(""), 4500);
    return () => clearTimeout(timer);
  }, [notice]);
  return (
    <NoticeContext.Provider value={setNotice}>
      {children}
      <div
        className={`toast ${notice ? "visible" : ""}`}
        role="status"
        aria-live="polite"
      >
        {notice}
      </div>
    </NoticeContext.Provider>
  );
}
export const useNotice = () => useContext(NoticeContext);
