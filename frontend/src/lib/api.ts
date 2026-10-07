import { z } from "zod";

export const userSchema = z.object({
  id: z.string(),
  email: z.string(),
  name: z.string(),
});

export type User = z.infer<typeof userSchema>;

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

/** Reads one cookie from a `document.cookie`-style string. */
export function readCookie(cookies: string, name: string): string | undefined {
  for (const part of cookies.split(";")) {
    const [key, ...value] = part.trim().split("=");
    if (key === name) return decodeURIComponent(value.join("="));
  }
  return undefined;
}

const problemSchema = z.object({ detail: z.string().min(1) });

/** The backend explains errors in the "detail" field of a problem-details body. */
export async function errorMessage(response: Response): Promise<string> {
  const problem = problemSchema.safeParse(await response.json().catch(() => null));
  if (problem.success) return problem.data.detail;
  if (response.status === 403) return "Your session expired. Reload the page and try again.";
  return "Something went wrong. Try again in a moment.";
}

/**
 * The backend wants its XSRF-TOKEN cookie copied into an X-XSRF-TOKEN header on every POST. The
 * cookie is missing on a first visit and right after signing in, so ask for a new one then.
 */
async function csrfToken(): Promise<string> {
  const existing = readCookie(document.cookie, "XSRF-TOKEN");
  if (existing) return existing;
  await fetch("/api/auth/csrf");
  return readCookie(document.cookie, "XSRF-TOKEN") ?? "";
}

async function post(path: string, body?: object): Promise<void> {
  const headers: Record<string, string> = { "X-XSRF-TOKEN": await csrfToken() };
  if (body) headers["Content-Type"] = "application/json";
  const response = await fetch(path, {
    method: "POST",
    headers,
    body: body && JSON.stringify(body),
  });
  if (!response.ok) throw new ApiError(response.status, await errorMessage(response));
}

export function signIn(email: string, password: string): Promise<void> {
  return post("/api/auth/login", { email, password });
}

export function createAccount(name: string, email: string, password: string): Promise<void> {
  return post("/api/auth/register", { name, email, password });
}

export function signOut(): Promise<void> {
  return post("/api/auth/logout");
}
