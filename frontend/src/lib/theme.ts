import { cookies } from "next/headers";

export type Theme = "light" | "dark";

/**
 * Light unless the visitor switched to dark. The choice is kept in a cookie, so the server draws the
 * page in the right colors and there's no flash of the wrong theme.
 */
export async function getTheme(): Promise<Theme> {
  const cookieStore = await cookies();
  return cookieStore.get("theme")?.value === "dark" ? "dark" : "light";
}
