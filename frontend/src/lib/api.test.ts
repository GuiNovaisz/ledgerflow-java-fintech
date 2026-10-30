import { beforeEach, afterEach, describe, it, expect, vi } from "vitest";
import {
  api,
  clearCsrf,
  onSessionExpired,
  parseResponse,
  ApiError,
} from "./api";
beforeEach(() => clearCsrf());
afterEach(() => vi.unstubAllGlobals());
describe("HTTP boundary", () => {
  it("retains exact decimal JSON while parsing counters as numbers", () => {
    expect(parseResponse('{"balance":999999999999999.99,"page":2}')).toEqual({
      balance: "999999999999999.99",
      page: 2,
    });
  });
  it("obtains CSRF in memory and sends same-origin cookies on mutations", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            token: "test-only-csrf",
            headerName: "X-CSRF-TOKEN",
          }),
        ),
      )
      .mockResolvedValueOnce(new Response('{"id":"created"}'));
    vi.stubGlobal("fetch", fetcher);
    expect(
      await api("/accounts", { method: "POST", body: '{"name":"Test"}' }),
    ).toEqual({ id: "created" });
    const options = fetcher.mock.calls[1][1] as RequestInit;
    expect(options.credentials).toBe("same-origin");
    expect(new Headers(options.headers).get("X-CSRF-TOKEN")).toBe(
      "test-only-csrf",
    );
  });
  it("does not retry a financial mutation automatically", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({ token: "fixture", headerName: "X-CSRF-TOKEN" }),
        ),
      )
      .mockRejectedValueOnce(new TypeError("offline"));
    vi.stubGlobal("fetch", fetcher);
    await expect(
      api("/transactions/transfers", { method: "POST", body: "{}" }),
    ).rejects.toMatchObject({ code: "NETWORK" });
    expect(fetcher).toHaveBeenCalledTimes(2);
  });
  it("clears session state on unauthorized protected requests without exposing response details", async () => {
    const expired = vi.fn();
    const dispose = onSessionExpired(expired);
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          new Response("<private stack trace>", { status: 401 }),
        ),
    );
    await expect(api("/accounts")).rejects.toBeInstanceOf(ApiError);
    expect(expired).toHaveBeenCalledOnce();
    dispose();
  });
});
