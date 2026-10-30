import { test, expect } from "@playwright/test";
import { randomUUID } from "node:crypto";
import { register, post, fundedAccount, csrf } from "./helpers";

test("metas persistem sem movimentar saldo e conta inativa bloqueia ações", async ({
  page,
}) => {
  await register(page.request);
  const account = await fundedAccount(page.request);
  await page.goto("/goals");
  await page.getByRole("button", { name: "Nova meta", exact: true }).click();
  await page.getByLabel("Nome da meta", { exact: true }).fill("Reserva E2E");
  await page.getByLabel("Valor alvo (BRL)", { exact: true }).fill("500.00");
  await page.getByLabel("Valor atual (BRL)", { exact: true }).fill("100.00");
  await page.getByRole("button", { name: "Salvar", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Reserva E2E" }),
  ).toBeVisible();
  await page.reload();
  await page.getByRole("button", { name: "Editar", exact: true }).click();
  await page.getByLabel("Valor atual (BRL)", { exact: true }).fill("500.00");
  await page.getByRole("button", { name: "Salvar", exact: true }).click();
  await expect(page.locator(".goal-card .status-chip")).toContainText(
    "Concluída",
  );
  expect(
    (await (await page.request.get(`/api/v1/accounts/${account.id}`)).json())
      .balance,
  ).toBe(1000);
  await page.getByRole("button", { name: "Excluir", exact: true }).click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Excluir", exact: true })
    .click();
  await expect(page.getByRole("heading", { name: "Reserva E2E" })).toHaveCount(
    0,
  );
  await page.goto(`/accounts/${account.id}`);
  await page
    .getByRole("button", { name: "Desativar conta", exact: true })
    .click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Desativar conta", exact: true })
    .click();
  await expect(
    page.getByRole("button", { name: "Novo depósito", exact: true }),
  ).toBeDisabled();
  expect(
    (await (await page.request.get(`/api/v1/accounts/${account.id}`)).json())
      .status,
  ).toBe("INACTIVE");
});

test("cadastro, sessão HttpOnly, preferências e logout real", async ({
  page,
  context,
}) => {
  await page.goto("/register");
  await page.getByLabel("Seu nome", { exact: true }).fill("Pessoa de teste");
  await page
    .getByLabel("E-mail", { exact: true })
    .fill(`ui-${randomUUID()}@example.test`);
  await page.getByLabel("Senha", { exact: true }).fill(`Safe-${randomUUID()}!`);
  await page.getByTestId("auth-submit").click();
  await expect(page).toHaveURL(/dashboard/);
  await expect(page.getByText("Seu próximo passo começa aqui")).toBeVisible();
  const session = (await context.cookies()).find(
    (cookie) => cookie.name === "LEDGERFLOW_SESSION",
  );
  expect(session?.httpOnly).toBe(true);
  expect(session?.sameSite).toBe("Strict");
  const stored = await page.evaluate(() => Object.keys(localStorage));
  expect(
    stored.every((key) =>
      ["ledgerflow-language", "ledgerflow-theme"].includes(key),
    ),
  ).toBe(true);
  await page.goto("/preferences");
  await page.getByTestId("theme-select").last().selectOption("light");
  await expect(page.locator("html")).toHaveAttribute("data-theme", "light");
  await page.getByTestId("theme-select").last().selectOption("dark");
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
  await page.getByTestId("language-select").last().selectOption("en");
  await expect(
    page.getByRole("heading", { name: /Preferences|Profile/ }),
  ).toBeVisible();
  await page.reload();
  await expect(page.getByTestId("language-select").last()).toHaveValue("en");
  await page
    .getByRole("button", { name: "Sign out", exact: true })
    .first()
    .click();
  await expect(page).toHaveURL(/login/);
  await page.goto("/dashboard");
  await expect(page).toHaveURL(/login/);
});

test("contas, depósito, saque, transferência confirmada e filtros reais", async ({
  page,
}) => {
  await register(page.request);
  const source = await fundedAccount(page.request, "Origem E2E");
  const destination = await post(page.request, "/accounts", {
    name: "Destino E2E",
  });
  await page.goto("/accounts");
  await page.getByRole("button", { name: "Nova conta", exact: true }).click();
  await page.getByTestId("account-name").fill("Criada pela interface");
  await page.getByTestId("create-account-submit").click();
  await expect(
    page.getByText("Criada pela interface", { exact: true }),
  ).toBeVisible();
  for (const [label, amount, description] of [
    ["Novo depósito", "100.25", "Depósito UI"],
    ["Novo saque", "10.25", "Mercado UI"],
  ]) {
    await page.goto("/dashboard");
    await page
      .getByRole("button", { name: label, exact: true })
      .first()
      .click();
    await page.getByTestId("operation-source").selectOption(source.id);
    await page.getByTestId("operation-amount").fill(amount);
    await page.getByTestId("operation-description").fill(description);
    await page.getByTestId("operation-continue").click();
    await page.getByTestId("operation-confirm").click();
    await expect(page.getByTestId("operation-done")).toBeVisible();
    await page.getByTestId("operation-done").click();
  }
  await page
    .getByRole("button", { name: "Nova transferência", exact: true })
    .first()
    .click();
  await page.getByTestId("operation-source").selectOption(source.id);
  await page.getByTestId("operation-target").selectOption(destination.id);
  await page.getByTestId("operation-amount").fill("250.00");
  await page.getByTestId("operation-description").fill("Transferência E2E");
  await page.getByTestId("operation-continue").click();
  await page.getByTestId("operation-confirm").click();
  await expect(page.getByTestId("operation-done")).toBeVisible();
  await page.getByTestId("operation-done").click();
  const account = await page.request.get(`/api/v1/accounts/${source.id}`);
  expect((await account.json()).balance).toBe(840);
  await page.goto("/transactions");
  await page.getByRole("searchbox").fill("Mercado UI");
  await expect(
    page.getByText("Mercado UI", { exact: true }).first(),
  ).toBeVisible();
  await expect(
    page.getByText("Transferência E2E", { exact: true }),
  ).toHaveCount(0);
  const download = page.waitForEvent("download");
  await page
    .getByRole("button", { name: "Exportar extrato", exact: true })
    .click();
  expect((await download).suggestedFilename()).toMatch(/\.csv$/);
});

test("timeout após commit mantém a mesma operação e consulta resultado", async ({
  page,
}) => {
  await register(page.request);
  const source = await fundedAccount(page.request);
  const target = await post(page.request, "/accounts", {
    name: "Destino retry",
  });
  let requests = 0;
  await page.route("**/api/v1/transactions/transfers", async (route) => {
    requests++;
    const response = await route.fetch();
    expect(response.status()).toBe(201);
    await route.abort("timedout");
  });
  await page.goto("/dashboard");
  await page
    .getByRole("button", { name: "Nova transferência", exact: true })
    .first()
    .click();
  await page.getByTestId("operation-source").selectOption(source.id);
  await page.getByTestId("operation-target").selectOption(target.id);
  await page.getByTestId("operation-amount").fill("25.00");
  await page.getByTestId("operation-continue").click();
  await page.getByTestId("operation-confirm").click();
  await expect(page.getByTestId("operation-check")).toBeVisible();
  await page.getByTestId("operation-check").click();
  await expect(page.getByTestId("operation-done")).toBeVisible();
  expect(requests).toBe(1);
  expect(
    (await (await page.request.get(`/api/v1/accounts/${source.id}`)).json())
      .balance,
  ).toBe(975);
});

test("CSRF e isolamento são impostos pelo servidor; sessão invalidada exige login", async ({
  page,
  playwright,
}) => {
  await register(page.request);
  const account = await fundedAccount(page.request);
  const other = await playwright.request.newContext({
    baseURL: process.env.E2E_BASE_URL || "http://127.0.0.1:8081",
  });
  await register(other);
  expect((await other.get(`/api/v1/accounts/${account.id}`)).status()).toBe(
    404,
  );
  expect(
    (
      await page.request.post("/api/v1/accounts", {
        data: { name: "Sem CSRF" },
      })
    ).status(),
  ).toBe(403);
  const protection = await csrf(page.request);
  expect(
    (
      await page.request.post("/api/v1/session/logout", {
        headers: { [protection.headerName]: protection.token },
      })
    ).status(),
  ).toBe(204);
  await page.goto("/dashboard");
  await expect(page).toHaveURL(/login/);
  await other.dispose();
});

test("mobile sem overflow, navegação e estado vazio", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await register(page.request);
  await page.goto("/dashboard");
  await expect(page.getByTestId("total-balance")).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.goto("/transactions");
  await expect(
    page.getByText("Nenhuma movimentação neste período"),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
});
