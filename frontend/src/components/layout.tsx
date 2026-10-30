import { useEffect, useRef, useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  ArrowDownToLine,
  ArrowLeftRight,
  ArrowUpFromLine,
  ChevronLeft,
  ChevronRight,
  ChartNoAxesCombined,
  House,
  Landmark,
  LayoutDashboard,
  LogOut,
  Menu,
  Moon,
  Search,
  Settings2,
  ShieldCheck,
  Sun,
  Tags,
  Target,
  UserRound,
  X,
} from "lucide-react";
import { usePreferences, useSession } from "../state";
import { useOperation } from "./operation";
import { ErrorMessage } from "./common";

export function Logo() {
  return (
    <Link className="brand" to="/dashboard" aria-label="LedgerFlow">
      <span className="brand-mark">
        <ArrowLeftRight size={21} />
      </span>
      <span>
        Ledger<span>Flow</span>
      </span>
    </Link>
  );
}
export function PreferenceControls() {
  const { t, i18n } = useTranslation();
  const { theme, setTheme } = usePreferences();
  return (
    <div className="preference-controls">
      <label className="theme-select">
        <span className="sr-only">{t("theme")}</span>
        {theme === "light" ? <Sun size={16} /> : <Moon size={16} />}
        <select
          data-testid="theme-select"
          aria-label={t("theme")}
          value={theme}
          onChange={(event) =>
            setTheme(event.target.value as "dark" | "light" | "system")
          }
        >
          <option value="dark">{t("dark")}</option>
          <option value="light">{t("light")}</option>
          <option value="system">{t("system")}</option>
        </select>
      </label>
      <select
        data-testid="language-select"
        aria-label={t("language")}
        value={i18n.language === "en" ? "en" : "pt-BR"}
        onChange={(event) => void i18n.changeLanguage(event.target.value)}
      >
        <option value="pt-BR">PT-BR</option>
        <option value="en">EN</option>
      </select>
    </div>
  );
}
const menu = [
  { to: "/dashboard", key: "dashboard", icon: LayoutDashboard },
  { to: "/accounts", key: "accounts", icon: Landmark },
  { to: "/transactions", key: "transactions", icon: ArrowLeftRight },
  { to: "/categories", key: "categories", icon: Tags },
  { to: "/goals", key: "goals", icon: Target },
  { to: "/reports", key: "reports", icon: ChartNoAxesCombined },
];
export default function Layout() {
  const { t } = useTranslation();
  const { user, logout } = useSession();
  const openOperation = useOperation();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [error, setError] = useState<unknown>();
  const [loggingOut, setLoggingOut] = useState(false);
  const searchRef = useRef<HTMLInputElement>(null);
  useEffect(() => {
    function shortcut(event: KeyboardEvent) {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        searchRef.current?.focus();
      }
      if (event.key === "Escape") setMobileOpen(false);
    }
    window.addEventListener("keydown", shortcut);
    return () => window.removeEventListener("keydown", shortcut);
  }, []);
  async function signOut() {
    if (loggingOut) return;
    setLoggingOut(true);
    try {
      await logout();
      navigate("/login", { replace: true });
    } catch (failure) {
      setError(failure);
    } finally {
      setLoggingOut(false);
    }
  }
  const initial =
    user?.displayName
      .split(" ")
      .filter(Boolean)
      .slice(0, 2)
      .map((name) => name[0])
      .join("")
      .toUpperCase() ?? "LF";
  return (
    <div className={`app-shell ${collapsed ? "sidebar-collapsed" : ""}`}>
      <a className="skip-link" href="#main-content">
        {t("skipContent")}
      </a>
      {mobileOpen && (
        <button
          className="sidebar-backdrop"
          aria-label={t("close")}
          onClick={() => setMobileOpen(false)}
        />
      )}
      <aside className={`sidebar ${mobileOpen ? "mobile-open" : ""}`}>
        <Logo />
        <button
          className="icon-button sidebar-mobile-close"
          aria-label={t("close")}
          onClick={() => setMobileOpen(false)}
        >
          <X size={20} />
        </button>
        <nav aria-label={t("navigation")}>
          {menu.map(({ to, key, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={() => setMobileOpen(false)}
              title={t(key)}
            >
              <Icon size={19} />
              <span>{t(key)}</span>
            </NavLink>
          ))}
          <div className="nav-group-label">{t("tools")}</div>
          <button
            onClick={() => {
              openOperation("TRANSFER");
              setMobileOpen(false);
            }}
            title={t("transfer")}
          >
            <ArrowLeftRight size={19} />
            <span>{t("transfer")}</span>
          </button>
          <button
            onClick={() => {
              openOperation("DEPOSIT");
              setMobileOpen(false);
            }}
            title={t("deposit")}
          >
            <ArrowDownToLine size={19} />
            <span>{t("deposit")}</span>
          </button>
          <button
            onClick={() => {
              openOperation("WITHDRAWAL");
              setMobileOpen(false);
            }}
            title={t("withdrawal")}
          >
            <ArrowUpFromLine size={19} />
            <span>{t("withdrawal")}</span>
          </button>
          <div className="nav-group-label">{t("settings")}</div>
          <NavLink
            to="/preferences"
            onClick={() => setMobileOpen(false)}
            title={t("profile")}
          >
            <UserRound size={19} />
            <span>{t("profile")}</span>
          </NavLink>
          <button
            onClick={() => void signOut()}
            disabled={loggingOut}
            title={t("logout")}
          >
            <LogOut size={19} />
            <span>{t("logout")}</span>
          </button>
        </nav>
        <div className="sidebar-footer">
          <p>
            <ShieldCheck size={16} />
            <span>{t("simulation")}</span>
          </p>
          <button
            className="collapse-button"
            onClick={() => setCollapsed(!collapsed)}
            title={t(collapsed ? "expand" : "collapse")}
          >
            {collapsed ? <ChevronRight size={17} /> : <ChevronLeft size={17} />}
            <span>{t("collapse")}</span>
          </button>
        </div>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <button
            className="icon-button mobile-open-button"
            aria-label={t("mobileMenu")}
            onClick={() => setMobileOpen(true)}
          >
            <Menu size={21} />
          </button>
          <div className="topbar-welcome">
            <h2>
              {t("hello", { name: user?.displayName.split(" ")[0] })}{" "}
              <span aria-hidden="true">✦</span>
            </h2>
            <p>{t("overview")}</p>
          </div>
          <form
            className="global-search"
            onSubmit={(event) => {
              event.preventDefault();
              navigate("/transactions?q=" + encodeURIComponent(search));
            }}
          >
            <Search size={17} />
            <input
              ref={searchRef}
              aria-label={t("searchLabel")}
              placeholder={t("search")}
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
            <kbd>⌘ K</kbd>
          </form>
          <PreferenceControls />
          <Link to="/preferences" className="user-menu">
            <span className="avatar">{initial}</span>
            <span>
              <strong>{user?.displayName}</strong>
              <small>{t("simulation")}</small>
            </span>
          </Link>
        </header>
        <main id="main-content" className="main-content">
          <ErrorMessage error={error} />
          <Outlet />
        </main>
        <footer className="app-footer">
          <ShieldCheck size={13} />
          {t("simulationNote")}
          <span>LedgerFlow / v0.2.0</span>
        </footer>
      </div>
      <nav className="mobile-nav" aria-label={t("navigation")}>
        <NavLink to="/dashboard">
          <House size={21} />
          <span>{t("home")}</span>
        </NavLink>
        <NavLink to="/accounts">
          <Landmark size={21} />
          <span>{t("accounts")}</span>
        </NavLink>
        <button
          className="mobile-primary"
          aria-label={t("newTransfer")}
          onClick={() => openOperation("TRANSFER")}
        >
          <span>+</span>
        </button>
        <NavLink to="/transactions">
          <ArrowLeftRight size={21} />
          <span>{t("transactions")}</span>
        </NavLink>
        <button onClick={() => setMobileOpen(true)}>
          <Settings2 size={21} />
          <span>{t("more")}</span>
        </button>
      </nav>
    </div>
  );
}
