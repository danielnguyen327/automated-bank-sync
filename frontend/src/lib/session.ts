import { cookies } from "next/headers";
import { cache } from "react";
import { userSchema, type User } from "./api";

const apiURL = process.env.API_URL ?? "http://localhost:8080";

export type Session =
  { status: "signed-in"; user: User } | { status: "signed-out" } | { status: "unavailable" };

/** Asks the backend whose session this browser's SESSION cookie belongs to. */
export const getSession = cache(async (): Promise<Session> => {
  const sessionCookie = (await cookies()).get("SESSION");
  if (!sessionCookie) return { status: "signed-out" };
  try {
    const response = await fetch(`${apiURL}/api/me`, {
      headers: { Cookie: `SESSION=${sessionCookie.value}` },
      cache: "no-store",
    });
    if (response.status === 401) return { status: "signed-out" };
    if (!response.ok) return { status: "unavailable" };
    return { status: "signed-in", user: userSchema.parse(await response.json()) };
  } catch {
    return { status: "unavailable" };
  }
});
