import { test, expect } from "@playwright/test";
import { readFile, mkdir } from "node:fs/promises";
import { resolve } from "node:path";
import { post } from "./helpers";

test("capture real seeded demo for portfolio", async ({ page }) => {
  test.skip(
    process.env.CAPTURE_PORTFOLIO !== "1",
    "Explicit local fixture capture; excluded from regular CI.",
  );
  const demo = JSON.parse(
    await readFile(resolve("../.local/demo/latest.json"), "utf8"),
  ) as {
    email: string;
    password: string;
    accounts: { id: string; name: string }[];
  };
  await post(page.request, "/session/login", {
    email: demo.email,
    password: demo.password,
  });
  await page.addInitScript(() => {
    localStorage.setItem("ledgerflow-theme", "dark");
    localStorage.setItem("ledgerflow-language", "pt-BR");
  });
  const folder = resolve("../docs/assets/screenshots");
  await mkdir(folder, { recursive: true });
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("console", (message) => {
    if (message.type() === "error") errors.push(message.text());
  });
  page.on("response", (response) => {
    if (response.status() >= 400)
      errors.push(`${response.status()} ${new URL(response.url()).pathname}`);
  });
  page.on("requestfailed", (request) => {
    if (request.failure()?.errorText !== "net::ERR_ABORTED")
      errors.push(`Request failed: ${new URL(request.url()).pathname}`);
  });
  async function shot(name: string) {
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({
      path: resolve(folder, name + ".png"),
      fullPage: !["dashboard-mobile", "transfer"].includes(name),
      animations: "disabled",
    });
  }
  await page.setViewportSize({ width: 1600, height: 1000 });
  await page.goto("/dashboard");
  await expect(page.getByTestId("total-balance")).toContainText("12.480,50");
  await expect(page.locator(".recharts-surface").first()).toBeVisible();
  await shot("dashboard-dark-desktop");
  await page.getByTestId("theme-select").selectOption("light");
  await expect(page.locator("html")).toHaveAttribute("data-theme", "light");
  await shot("dashboard-light-desktop");
  await page.getByTestId("theme-select").selectOption("dark");
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.locator(".mobile-nav")).toBeVisible();
  await expect
    .poll(() =>
      page.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
    )
    .toBe(true);
  await shot("dashboard-mobile");
  for (const width of [320, 768]) {
    await page.setViewportSize({ width, height: 1000 });
    await expect
      .poll(() =>
        page.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
      )
      .toBe(true);
  }
  await page.setViewportSize({ width: 1600, height: 1000 });
  await page.goto("/accounts");
  await expect(
    page.getByText("Conta Principal", { exact: true }),
  ).toBeVisible();
  await shot("accounts");
  await page.goto("/transactions");
  await expect(page.getByRole("table")).toBeVisible();
  await shot("transactions");
  await page.goto("/dashboard");
  await page
    .getByRole("button", { name: "Nova transferência", exact: true })
    .first()
    .click();
  await page.getByTestId("operation-source").selectOption(demo.accounts[0].id);
  await page.getByTestId("operation-target").selectOption(demo.accounts[1].id);
  await page.getByTestId("operation-amount").fill("500.00");
  await page
    .getByTestId("operation-description")
    .fill("Planejamento da reserva");
  await shot("transfer");
  await page.getByRole("button", { name: "Cancelar", exact: true }).click();
  await page.goto("/goals");
  await expect(
    page.getByRole("heading", { name: "Reserva de emergência", exact: true }),
  ).toBeVisible();
  await shot("goals");
  expect(errors).toEqual([]);
});
