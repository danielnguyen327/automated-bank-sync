import { ApiStatus } from "@/components/api-status";

export default function Home() {
  return (
    <main className="mx-auto flex min-h-screen max-w-xl flex-col justify-center gap-4 px-6">
      <h1 className="text-3xl font-semibold tracking-tight">LedgerSync</h1>
      <p className="text-ink-2">
        All your bank accounts and credit cards in one place, synced automatically.
      </p>
      <ApiStatus />
    </main>
  );
}
