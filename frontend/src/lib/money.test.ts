import { describe, it, expect } from "vitest";
import { cents, money, inputMoney, percentage } from "./money";
describe("exact financial presentation", () => {
  it("preserves cents beyond the safe JavaScript integer range", () => {
    expect(cents("999999999999999.99")).toBe(99999999999999999n);
    expect(money("999999999999999.99", "en")).toContain(
      "999,999,999,999,999.99",
    );
  });
  it("formats both locales without converting money to float", () => {
    expect(money("12480.50", "pt-BR")).toContain("12.480,50");
    expect(money("12480.50", "en")).toContain("12,480.50");
    expect(cents("-0.01")).toBe(-1n);
  });
  it("rejects fractional cents, exponents, negative input and overflowing precision", () => {
    for (const value of [
      "1.001",
      "1e3",
      "-1",
      "1000000000000000.00",
      "1,000.00",
    ])
      expect(inputMoney(value)).toBeNull();
    expect(inputMoney(" 12,50 ")).toBe("12.50");
  });
  it("clamps planning progress and handles zero safely", () => {
    expect(percentage("8000.00", "20000.00")).toBe(40);
    expect(percentage("200.00", "100.00")).toBe(100);
    expect(percentage("0.00", "0.00")).toBe(0);
  });
});
