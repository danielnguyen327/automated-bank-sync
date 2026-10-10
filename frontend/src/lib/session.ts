import { cookies } from "next/headers";
import { cache } from "react";
import { z } from "zod";
import { bankSchema, userSchema, type Bank, type User } from "./api";

const apiURL = process.env.API_URL ?? "http://localhost:8080";

export type Session =
  { status: "signed-in"; user: User } | { status: "signed-out" } | { status: "unavailable" };

/** Calls the backend from the Next.js server, passing on only this browser's SESSION cookie. */
async function fetchFromBackend(path: string): Promise<Response | null> {
  const sessionCookie = (await cookies()).get("SESSION");
  if (!sessionCookie) return null;
  return fetch(`${apiURL}${path}`, {
    headers: { Cookie: `SESSION=${sessionCookie.value}` },
    cache: "no-store",
  });
}

/** Asks the backend whose session this browser's SESSION cookie belongs to. */
export const getSession = cache(async (): Promise<Session> => {
  try {
    const response = await fetchFromBackend("/api/me");
    if (!response || response.status === 401) return { status: "signed-out" };
    if (!response.ok) return { status: "unavailable" };
    return { status: "signed-in", user: userSchema.parse(await response.json()) };
  } catch {
    return { status: "unavailable" };
  }
});

/** The signed-in user's connected banks, or null if they couldn't be loaded. */
export async function getBanks(): Promise<Bank[] | null> {
  try {
    const response = await fetchFromBackend("/api/banks");
    if (!response?.ok) return null;
    return z.array(bankSchema).parse(await response.json());
  } catch {
    return null;
  }
}
