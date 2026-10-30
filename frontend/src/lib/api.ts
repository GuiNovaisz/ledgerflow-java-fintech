import { parse } from "lossless-json";
export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
  ) {
    super(code);
    this.name = "ApiError";
  }
}
let csrf: { token: string; headerName: string } | undefined;
let expired = () => {};
export function onSessionExpired(handler: () => void) {
  expired = handler;
  return () => {
    expired = () => {};
  };
}
export function clearCsrf() {
  csrf = undefined;
}
export async function refreshCsrf() {
  csrf = await api<{ token: string; headerName: string }>("/session/csrf");
  return csrf;
}
export function parseResponse(text: string) {
  return parse(text, undefined, (value) =>
    /[.eE]/.test(value) ? value : Number(value),
  );
}
export async function api<T>(
  path: string,
  options: RequestInit = {},
  timeout = 20000,
): Promise<T> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeout);
  const abort = () => controller.abort();
  options.signal?.addEventListener("abort", abort, { once: true });
  if (options.signal?.aborted) controller.abort();
  try {
    const method = (options.method || "GET").toUpperCase();
    const headers = new Headers(options.headers);
    if (options.body) headers.set("Content-Type", "application/json");
    if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
      const token = csrf ?? (await refreshCsrf());
      headers.set(token.headerName, token.token);
    }
    const response = await fetch("/api/v1" + path, {
      ...options,
      headers,
      credentials: "same-origin",
      signal: controller.signal,
    });
    if (!response.ok) {
      let code = "HTTP_ERROR";
      try {
        const problem = (await response.json()) as { code?: string };
        code = problem.code || code;
      } catch {
        /* Untrusted HTML/network errors are never shown. */
      }
      if (
        response.status === 401 &&
        !path.startsWith("/session/login") &&
        !path.startsWith("/session/register") &&
        path != "/session/me"
      ) {
        clearCsrf();
        expired();
      }
      if (response.status === 403) {
        clearCsrf();
        // An expired session can be rejected by CSRF before authorization runs.
        // Check identity once; never replay a financial mutation automatically.
        if (!path.startsWith("/session/")) {
          const identity = await fetch("/api/v1/session/me", {
            credentials: "same-origin",
            signal: controller.signal,
          });
          if (identity.status === 401) expired();
        }
      }
      throw new ApiError(response.status, code);
    }
    if (response.status === 204) return undefined as T;
    return parseResponse(await response.text()) as T;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    if (controller.signal.aborted) throw new ApiError(0, "TIMEOUT");
    throw new ApiError(0, "NETWORK");
  } finally {
    clearTimeout(timer);
    options.signal?.removeEventListener("abort", abort);
  }
}
export function queryString(
  values: Record<string, string | number | undefined>,
) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== "") params.set(key, String(value));
  });
  return params.toString();
}
export async function downloadCsv(query: string) {
  const response = await fetch("/api/v1/entries/export.csv?" + query, {
    credentials: "same-origin",
    signal: AbortSignal.timeout(20000),
  });
  if (!response.ok) {
    if (response.status === 401) expired();
    throw new ApiError(response.status, "EXPORT_ERROR");
  }
  const url = URL.createObjectURL(await response.blob());
  const a = document.createElement("a");
  a.href = url;
  a.download = "ledgerflow-statement.csv";
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
