import { SignInScreen } from "@/components/sign-in-screen";
import { SignedInHome } from "@/components/signed-in-home";
import { getSession } from "@/lib/session";
import { getTheme } from "@/lib/theme";

/** One page: the sign-in screen when signed out, the app when signed in. */
export default async function Home() {
  const [session, theme] = await Promise.all([getSession(), getTheme()]);
  if (session.status === "signed-in") return <SignedInHome user={session.user} theme={theme} />;
  return <SignInScreen theme={theme} unavailable={session.status === "unavailable"} />;
}
