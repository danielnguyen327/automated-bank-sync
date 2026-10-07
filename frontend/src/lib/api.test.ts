import { afterEach, describe, expect, it, vi } from "vitest";
import { errorMessage, readCookie, signIn } from "./api";

describe("readCookie", () => {
  it("finds one cookie among several", () => {
    expect(readCookie("theme=dark; XSRF-TOKEN=abc-123; other=x", "XSRF-TOKEN")).toBe("abc-123");
  });
  it("returns undefined when the cookie is missing", () => {
    expect(readCookie("theme=dark", "XSRF-TOKEN")).toBeUndefined();
  });
});

describe("errorMessage", () => {
  it("uses the backend's problem detail", async () => {
    const response = Response.json({ detail: "Email or password is incorrect." }, { status: 401 });
    expect(await errorMessage(response)).toBe("Email or password is incorrect.");
  });
  it("falls back to a general message when there is no detail", async () => {
    const response = new Response("Internal Server Error", { status: 500 });
    expect(await errorMessage(response)).toBe("Something went wrong. Try again in a moment.");
  });
});

describe("signIn", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("gets a CSRF cookie first, then sends it back in a header", async () => {
    const browser = { cookie: "" };
    const fetchMock = vi.fn(async (path: string) => {
      if (path === "/api/auth/csrf") browser.cookie = "XSRF-TOKEN=abc-123";
      return new Response(null, { status: 204 });
    });
    vi.stubGlobal("document", browser);
    vi.stubGlobal("fetch", fetchMock);

    await signIn("sam@example.com", "correct horse battery");

    expect(fetchMock).toHaveBeenNthCalledWith(1, "/api/auth/csrf");
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "/api/auth/login",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({ "X-XSRF-TOKEN": "abc-123" }),
      }),
    );
  });

  it("throws the backend's message when sign-in fails", async () => {
    vi.stubGlobal("document", { cookie: "XSRF-TOKEN=abc-123" });
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        Response.json({ detail: "Email or password is incorrect." }, { status: 401 }),
      ),
    );

    await expect(signIn("sam@example.com", "wrong")).rejects.toMatchObject({
      status: 401,
      message: "Email or password is incorrect.",
    });
  });
});
