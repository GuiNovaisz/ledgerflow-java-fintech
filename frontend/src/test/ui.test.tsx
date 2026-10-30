import { beforeEach, afterEach, describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import i18n from "../i18n";
import { PreferencesProvider, SessionProvider } from "../state";
import { PreferenceControls } from "../components/layout";
import {
  Empty,
  Loading,
  PeriodPicker,
  calendarPeriod,
} from "../components/common";
import AuthPage from "../pages/auth";
import App from "../App";
beforeEach(async () => {
  localStorage.clear();
  await i18n.changeLanguage("pt-BR");
});
afterEach(() => vi.unstubAllGlobals());
function wrapper(element: React.ReactNode, route = "/login") {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <QueryClientProvider
        client={
          new QueryClient({
            defaultOptions: {
              queries: { retry: false },
              mutations: { retry: false },
            },
          })
        }
      >
        <PreferencesProvider>
          <SessionProvider>{element}</SessionProvider>
        </PreferencesProvider>
      </QueryClientProvider>
    </MemoryRouter>,
  );
}
describe("user-facing behavior", () => {
  it("shows translated empty and loading states", () => {
    render(
      <>
        <Empty title="emptyAccounts" body="emptyAccountsBody" />
        <Loading />
      </>,
    );
    expect(screen.getByText("Seu próximo passo começa aqui")).toBeVisible();
    expect(screen.getByRole("status")).toHaveTextContent("Carregando");
  });
  it("persists theme and language preferences without credentials", async () => {
    const user = userEvent.setup();
    render(
      <PreferencesProvider>
        <PreferenceControls />
      </PreferencesProvider>,
    );
    await user.selectOptions(screen.getByTestId("theme-select"), "dark");
    expect(document.documentElement.dataset.theme).toBe("dark");
    await user.selectOptions(screen.getByTestId("language-select"), "en");
    expect(localStorage.getItem("ledgerflow-language")).toBe("en");
    expect(screen.getByLabelText("Theme")).toBeVisible();
    expect(Object.keys(localStorage).sort()).toEqual([
      "ledgerflow-language",
      "ledgerflow-theme",
    ]);
  });
  it("rejects invalid registration before any mutation request", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response("{}", { status: 401 })),
    );
    const user = userEvent.setup();
    wrapper(<AuthPage />, "/register");
    await screen.findByLabelText("Seu nome");
    await user.type(screen.getByLabelText("E-mail"), "bad");
    await user.type(screen.getByLabelText("Senha"), "short");
    await user.click(screen.getByTestId("auth-submit"));
    expect(await screen.findByText("Informe um e-mail válido.")).toBeVisible();
    expect(screen.getAllByRole("alert").length).toBeGreaterThan(1);
    expect(vi.mocked(fetch)).toHaveBeenCalledTimes(1);
  });
  it("protects authenticated routes when the server rejects the session", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response("{}", { status: 401 })),
    );
    wrapper(<App />, "/accounts");
    expect(await screen.findByTestId("auth-submit")).toBeVisible();
  });
  it("applies a real bounded period and rejects reversed dates", async () => {
    const user = userEvent.setup();
    const changed = vi.fn();
    render(<PeriodPicker value={calendarPeriod(30)} onChange={changed} />);
    await user.selectOptions(screen.getByLabelText("Período"), "7");
    expect(changed).toHaveBeenCalledOnce();
    const period = changed.mock.calls[0][0] as { from: string; to: string };
    expect(Date.parse(period.to) - Date.parse(period.from)).toBe(7 * 86400000);
    await user.selectOptions(screen.getByLabelText("Período"), "custom");
    await user.clear(screen.getByLabelText("De"));
    await user.type(screen.getByLabelText("De"), "2030-12-31");
    await user.click(screen.getByRole("button", { name: "Aplicar" }));
    expect(screen.getByRole("alert")).toBeVisible();
    expect(changed).toHaveBeenCalledTimes(1);
  });
});
