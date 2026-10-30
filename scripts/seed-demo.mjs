// Explicit, local-only synthetic fixtures. Never runs automatically at application startup.
import { randomBytes, randomUUID, createHash } from "node:crypto";
import { mkdir, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { resolve, dirname } from "node:path";
import { spawnSync } from "node:child_process";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const args = process.argv.slice(2);
const arg = (name, fallback) => {
  const i = args.indexOf(name);
  return i < 0 ? fallback : args[i + 1];
};
const base = new URL(arg("--base-url", "http://127.0.0.1:8081"));
if (!["localhost", "127.0.0.1", "[::1]"].includes(base.hostname))
  throw new Error("Demo fixtures are restricted to loopback hosts");
const historical = args.includes("--historical");
const project = arg("--project", "");
if (historical && !/^ledgerflow-demo(?:-[a-z0-9-]+)?$/.test(project))
  throw new Error(
    "Historical fixtures require an isolated --project ledgerflow-demo[-name]",
  );
const email = `demo-${randomUUID()}@example.test`;
const password = randomBytes(24).toString("base64url");
let token;
async function api(path, body, method = "POST") {
  const response = await fetch(new URL("/api/v1" + path, base), {
    method,
    signal: AbortSignal.timeout(30000),
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      "Idempotency-Key": randomUUID(),
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  });
  if (!response.ok)
    throw new Error(`Demo request ${path} returned ${response.status}`);
  return response.status === 204 ? undefined : response.json();
}
const user = await api("/auth/register", {
  email,
  password,
  displayName: "Mariana Costa · Demo",
});
token = (await api("/auth/login", { email, password })).accessToken;
const accounts = [];
for (const name of ["Conta Principal", "Conta Poupança", "Projetos"])
  accounts.push(await api("/accounts", { name }));
// All amounts are integer cents. The same fixture can use only HTTP (today) or
// explicit privileged SQL in a fresh demo namespace (historical chart data).
const fixtures = [
  [-29, "DEPOSIT", 700000, "SALARY", "Saldo inicial · demonstração"],
  [-27, "WITHDRAWAL", 15690, "FOOD", "Supermercado"],
  [-25, "TRANSFER", 325000, "OTHER", "Reserva para objetivos", 1],
  [-22, "WITHDRAWAL", 120000, "HOUSING", "Moradia"],
  [-20, "WITHDRAWAL", 2750, "TRANSPORT", "Transporte urbano"],
  [-18, "DEPOSIT", 425000, "SALARY", "Salário · simulado"],
  [-15, "WITHDRAWAL", 3990, "ENTERTAINMENT", "Cinema"],
  [-13, "WITHDRAWAL", 18000, "HEALTH", "Saúde e bem-estar"],
  [-10, "WITHDRAWAL", 24900, "EDUCATION", "Curso de programação"],
  [-8, "DEPOSIT", 330000, "SALARY", "Projeto freelance · simulado"],
  [-6, "TRANSFER", 81000, "OTHER", "Planejamento de projetos", 2],
  [-4, "WITHDRAWAL", 8520, "OTHER", "Serviços"],
  [-1, "WITHDRAWAL", 13100, "FOOD", "Café e mercado"],
];
const money = (cents) =>
  `${Math.trunc(cents / 100)}.${String(cents % 100).padStart(2, "0")}`;
if (historical) {
  const quote = (value) =>
    value === null ? "NULL" : "'" + String(value).replaceAll("'", "''") + "'";
  const balances = [0, 0, 0];
  const sql = [
    "BEGIN;",
    `DO $$ BEGIN IF (SELECT count(*) FROM accounts WHERE owner_id=${quote(user.id)} AND balance=0) <> 3 OR EXISTS(SELECT 1 FROM operations WHERE owner_id=${quote(user.id)}) THEN RAISE EXCEPTION 'Demo accounts must be fresh and empty'; END IF; END $$;`,
  ];
  const origin = new Date();
  origin.setUTCHours(12, 0, 0, 0);
  for (const [
    day,
    type,
    cents,
    category,
    description,
    destination,
  ] of fixtures) {
    const id = randomUUID();
    const key = `demo-${randomUUID()}`;
    const amount = money(cents);
    const date = new Date(origin.getTime() + day * 86400000).toISOString();
    const source = type === "DEPOSIT" ? null : accounts[0].id;
    const target = type === "WITHDRAWAL" ? null : accounts[destination || 0].id;
    const fingerprint = createHash("sha256")
      .update(
        JSON.stringify([type, source, target, amount, category, description]),
      )
      .digest("hex");
    sql.push(
      `INSERT INTO operations(id,owner_id,idempotency_key,fingerprint,type,source_account_id,target_account_id,amount,category,description,created_at) VALUES(${[id, user.id, key, fingerprint, type, source, target, amount, category, description, date].map(quote).join(",")});`,
    );
    if (source) {
      balances[0] -= cents;
      sql.push(
        `INSERT INTO ledger_entries(id,operation_id,account_id,direction,amount,created_at) VALUES(${[randomUUID(), id, source, "DEBIT", amount, date].map(quote).join(",")});`,
      );
    }
    if (target) {
      balances[destination || 0] += cents;
      sql.push(
        `INSERT INTO ledger_entries(id,operation_id,account_id,direction,amount,created_at) VALUES(${[randomUUID(), id, target, "CREDIT", amount, date].map(quote).join(",")});`,
      );
    }
    if (balances.some((balance) => balance < 0))
      throw new Error("Fixture would overdraw");
  }
  const created = new Date(origin.getTime() - 30 * 86400000).toISOString();
  accounts.forEach((account, index) =>
    sql.push(
      `UPDATE accounts SET balance=${money(balances[index])},created_at=${quote(created)} WHERE id=${quote(account.id)} AND owner_id=${quote(user.id)};`,
    ),
  );
  sql.push("COMMIT;");
  const environmentFile = arg("--env-file", "");
  const result = spawnSync(
    "docker",
    [
      "compose",
      ...(environmentFile ? ["--env-file", environmentFile] : []),
      "-p",
      project,
      "exec",
      "-T",
      "postgres",
      "psql",
      "-X",
      "-U",
      "ledgerflow",
      "-d",
      "ledgerflow",
      "-v",
      "ON_ERROR_STOP=1",
    ],
    { cwd: root, input: sql.join("\n"), encoding: "utf8" },
  );
  if (result.status !== 0)
    throw new Error(
      "Historical demo transaction failed; inspect the isolated database. No credentials were logged.",
    );
} else {
  for (const [, type, cents, category, description, destination] of fixtures) {
    const fields =
      type === "TRANSFER"
        ? {
            sourceAccountId: accounts[0].id,
            targetAccountId: accounts[destination].id,
          }
        : { accountId: accounts[0].id };
    await api(
      `/transactions/${type === "DEPOSIT" ? "deposits" : type === "WITHDRAWAL" ? "withdrawals" : "transfers"}`,
      { ...fields, amount: money(cents), category, description },
    );
  }
}
for (const goal of [
  {
    name: "Reserva de emergência",
    targetAmount: "20000.00",
    currentAmount: "8000.00",
  },
  {
    name: "Viagem dos sonhos",
    targetAmount: "15000.00",
    currentAmount: "6500.00",
  },
  {
    name: "Próximo projeto",
    targetAmount: "10000.00",
    currentAmount: "3250.00",
  },
])
  await api("/goals", { ...goal, dueDate: null });
const folder = resolve(root, ".local", "demo");
await mkdir(folder, { recursive: true });
const record = {
  baseUrl: base.origin,
  email,
  password,
  userId: user.id,
  accounts: accounts.map(({ id, name }) => ({ id, name })),
  historical,
  createdAt: new Date().toISOString(),
};
const file = resolve(folder, `credentials-${user.id}.json`);
await writeFile(file, JSON.stringify(record, null, 2), {
  flag: "wx",
  mode: 0o600,
});
await writeFile(
  resolve(folder, "latest.json"),
  JSON.stringify(record, null, 2),
  { mode: 0o600 },
);
console.log(`Synthetic demo created. Private login details: ${file}`);
