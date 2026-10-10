import { afterEach, describe, expect, it, vi } from "vitest";
import {
  ApiError,
  createLinkToken,
  disconnectBank,
  errorMessage,
  messageFor,
  readCookie,
  signIn,
} from "./api";

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

describe("bank requests", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("returns the link token the backend created", async () => {
    vi.stubGlobal("document", { cookie: "XSRF-TOKEN=abc-123" });
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => Response.json({ linkToken: "link-sandbox-42" })),
    );

    expect(await createLinkToken()).toBe("link-sandbox-42");
  });

  it("disconnects a bank with a DELETE that carries the CSRF token", async () => {
    vi.stubGlobal("document", { cookie: "XSRF-TOKEN=abc-123" });
    const fetchMock = vi.fn(async () => new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await disconnectBank("bank-1");

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/banks/bank-1",
      expect.objectContaining({
        method: "DELETE",
        headers: expect.objectContaining({ "X-XSRF-TOKEN": "abc-123" }),
      }),
    );
  });
});

describe("messageFor", () => {
  it("shows the backend's sentence for an ApiError", () => {
    expect(messageFor(new ApiError(404, "That bank isn't connected."))).toBe(
      "That bank isn't connected.",
    );
  });
  it("explains a network failure in plain words", () => {
    expect(messageFor(new TypeError("Failed to fetch"))).toBe(
      "Can’t reach LedgerSync right now. Try again in a moment.",
    );
  });
});
