"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { disconnectBank, messageFor } from "@/lib/api";

/** Disconnects one bank, after asking once to make sure. */
export function DisconnectBankButton({ bankId, bankName }: { bankId: string; bankName: string }) {
  const router = useRouter();
  const [confirming, setConfirming] = useState(false);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, startRefresh] = useTransition();
  const busy = working || refreshing;

  async function disconnect() {
    setWorking(true);
    setError(null);
    try {
      await disconnectBank(bankId);
      startRefresh(() => router.refresh());
    } catch (e) {
      setError(messageFor(e));
    } finally {
      setWorking(false);
    }
  }

  if (!confirming) {
    return (
      <button
        type="button"
        onClick={() => setConfirming(true)}
        className="h-9 rounded-lg px-3 text-sm font-medium text-ink-2 hover:bg-muted hover:text-ink"
      >
        Disconnect
      </button>
    );
  }

  return (
    <div className="flex flex-col items-end gap-2">
      <p className="text-sm text-ink-2">
        Disconnect {bankName}? Its accounts are removed from LedgerSync.
      </p>
      <div className="flex gap-2">
        <button
          type="button"
          onClick={() => setConfirming(false)}
          disabled={busy}
          className="h-9 rounded-lg border border-field-line bg-surface px-3 text-sm font-medium hover:bg-muted"
        >
          Cancel
        </button>
        <button
          type="button"
          onClick={disconnect}
          disabled={busy}
          className="h-9 rounded-lg border border-danger px-3 text-sm font-semibold text-danger disabled:opacity-70"
        >
          {busy ? "Disconnecting…" : "Disconnect"}
        </button>
      </div>
      {error && (
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
      )}
    </div>
  );
}
