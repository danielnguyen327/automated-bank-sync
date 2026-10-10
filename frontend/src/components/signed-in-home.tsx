import type { Bank, User } from "@/lib/api";
import type { Theme } from "@/lib/theme";
import { Brand } from "./brand";
import { ConnectBankButton } from "./connect-bank-button";
import { DeleteAccount } from "./delete-account";
import { AfterFirstSync, FirstVisit } from "./first-visit";
import { SignOutButton } from "./sign-out-button";
import { ThemeToggle } from "./theme-toggle";
import { YourBanks } from "./your-banks";

/** The signed-in app: screen A2 until a bank is connected, then the connected banks. */
export function SignedInHome({
  user,
  theme,
  banks,
}: {
  user: User;
  theme: Theme;
  banks: Bank[] | null;
}) {
  const firstName = user.name.split(" ")[0] ?? user.name;
  const hasBanks = banks !== null && banks.length > 0;

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
      <main className="mx-auto flex w-full max-w-4xl flex-col gap-8 px-4 py-10 sm:px-6">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <h1 className="text-[28px] leading-tight font-semibold tracking-tight">
            Welcome, {firstName}
          </h1>
          {hasBanks && <ConnectBankButton label="Connect another bank" look="outline" />}
        </div>
        {banks === null ? (
          <p
            role="status"
            className="rounded-lg border border-line bg-surface px-4 py-3 text-sm text-ink-2"
          >
            Couldn’t load your banks right now. Reload the page to try again.
          </p>
        ) : hasBanks ? (
          <YourBanks banks={banks} />
        ) : (
          <FirstVisit />
        )}
        <AfterFirstSync />
        <DeleteAccount />
      </main>
    </div>
  );
}
