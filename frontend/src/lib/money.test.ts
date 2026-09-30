import { describe, expect, it } from "vitest";
import { formatCents } from "./money";

describe("formatCents", () => {
  it("formats whole cents as dollars", () => {
    expect(formatCents(114620)).toBe("$1,146.20");
  });
  it("keeps the sign for money coming in", () => {
    expect(formatCents(-585000)).toBe("-$5,850.00");
  });
  it("handles zero", () => {
    expect(formatCents(0)).toBe("$0.00");
  });
});