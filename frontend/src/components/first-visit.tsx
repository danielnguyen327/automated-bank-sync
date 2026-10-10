import { ConnectBankButton } from "./connect-bank-button";

const steps = [
  { title: "Choose your bank", text: "Search for it in Plaid’s window." },
  {
    title: "Sign in to your bank",
    text: "Plaid checks your login and asks which accounts to share.",
  },
  {
    title: "Watch your dashboard fill in",
    text: "Balances and transactions arrive after the first sync.",
  },
];

/** Screen A2: shown until the first bank is connected. */
export function FirstVisit() {
  return (
    <section
      aria-labelledby="connect-title"
      className="flex flex-col gap-5 rounded-2xl border border-line bg-surface p-6 sm:p-10"
    >
      <div className="flex flex-col gap-2">
        <h2 id="connect-title" className="text-[26px] leading-tight font-semibold">
          Connect your first bank
        </h2>
        <p className="max-w-[56ch] text-[15px] leading-relaxed text-ink-2">
          LedgerSync uses Plaid to connect securely. You sign in to your bank inside Plaid’s window,
          so LedgerSync never sees your password.
        </p>
      </div>
      <ol className="flex flex-col gap-3.5">
        {steps.map((step, index) => (
          <li key={step.title} className="flex items-start gap-3">
            <span className="flex size-[26px] shrink-0 items-center justify-center rounded-full bg-muted text-[13px] font-semibold text-accent">
              {index + 1}
            </span>
            <div className="flex flex-col">
              <span className="text-[15px] font-medium">{step.title}</span>
              <span className="text-[13px] text-ink-3">{step.text}</span>
            </div>
          </li>
        ))}
      </ol>
      <ConnectBankButton label="Connect a bank" />
      <p className="rounded-lg bg-muted px-3.5 py-3 text-[13px] text-ink-2">
        Demo mode: pick any bank in Plaid’s window and sign in with{" "}
        <span className="font-mono text-ink">user_good</span> /{" "}
        <span className="font-mono text-ink">pass_good</span>.
      </p>
    </section>
  );
}

const coming = [
  {
    title: "Net worth",
    text: "Everything in your bank accounts, minus what you owe on cards.",
  },
  {
    title: "What changed this month",
    text: "A short summary from Claude, with every number calculated by the app.",
  },
  {
    title: "Spending by category",
    text: "Where your money went, for all accounts or just your cards.",
  },
];

/** Outlined placeholders for what the dashboard shows once transactions are synced. */
export function AfterFirstSync() {
  return (
    <section aria-labelledby="after-title" className="flex flex-col gap-3">
      <h2 id="after-title" className="text-xs font-semibold tracking-wider text-ink-3 uppercase">
        After your first sync you’ll see
      </h2>
      <div className="grid gap-4 sm:grid-cols-3">
        {coming.map((card) => (
          <div
            key={card.title}
            className="flex flex-col gap-1.5 rounded-xl border border-dashed border-field-line px-5 py-4"
          >
            <span className="text-[15px] font-semibold">{card.title}</span>
            <span className="text-[13px] text-ink-3">{card.text}</span>
          </div>
        ))}
      </div>
    </section>
  );
}
