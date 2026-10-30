import { randomUUID } from "node:crypto";
import { expect, type APIRequestContext } from "@playwright/test";
export async function csrf(request: APIRequestContext) {
  const response = await request.get("/api/v1/session/csrf");
  expect(response.ok()).toBeTruthy();
  return response.json() as Promise<{ token: string; headerName: string }>;
}
export async function post(
  request: APIRequestContext,
  path: string,
  data?: unknown,
  headers: Record<string, string> = {},
) {
  const protection = await csrf(request);
  const response = await request.post("/api/v1" + path, {
    data,
    headers: { [protection.headerName]: protection.token, ...headers },
  });
  expect(response.ok(), `POST ${path}: ${response.status()}`).toBeTruthy();
  return response.status() === 204 ? undefined : response.json();
}
export async function register(request: APIRequestContext) {
  const credentials = {
    email: `e2e-${randomUUID()}@example.test`,
    password: `Safe-${randomUUID()}!`,
    displayName: "Pessoa E2E",
  };
  const user = await post(request, "/session/register", credentials);
  return { ...credentials, user };
}
export async function fundedAccount(
  request: APIRequestContext,
  name = "Conta E2E",
  amount = "1000.00",
) {
  const account = await post(request, "/accounts", { name });
  await post(
    request,
    "/transactions/deposits",
    {
      accountId: account.id,
      amount,
      category: "SALARY",
      description: "Dados de teste descartáveis",
    },
    { "Idempotency-Key": randomUUID() },
  );
  return account as { id: string; name: string };
}
