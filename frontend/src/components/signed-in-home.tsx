import type { User } from "@/lib/api";
import type { Theme } from "@/lib/theme";
import { Brand } from "./brand";
import { SignOutButton } from "./sign-out-button";
import { ThemeToggle } from "./theme-toggle";

/** What a signed-in user sees until Day 3 adds connecting a bank. */
export function SignedInHome({ user, theme }: { user: User; theme: Theme }) {
  const firstName = user.name.split(" ")[0] ?? user.name;

  return (
    <div className="flex min-h-screen flex-col">
      <header className="flex items-center gap-3 border-b border-line bg-surface px-4 py-2.5 sm:px-6">
        <Brand />
        <div className="ml-auto flex items-center gap-2">
          <span className="hidden text-sm text-ink-2 sm:inline">{user.email}</span>
          <ThemeToggle initialTheme={theme} />
          <SignOutButton />
        </div>
      </header>
      <main className="mx-auto flex w-full max-w-3xl flex-col gap-6 px-4 py-10 sm:px-6">
        <h1 className="text-2xl font-semibold tracking-tight">Welcome, {firstName}</h1>
        <section className="flex flex-col gap-1 rounded-2xl border border-line bg-surface p-6">
          <h2 className="text-base font-semibold">No banks connected yet</h2>
          <p className="text-sm text-ink-2">
            Your accounts, balances and spending will show up here once you connect a bank.
          </p>
        </section>
      </main>
    </div>
  );
}
