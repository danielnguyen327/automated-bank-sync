"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { deleteAccount, messageFor } from "@/lib/api";

/** Deletes the account and everything in it, after asking once to make sure. */
export function DeleteAccount() {
  const router = useRouter();
  const [confirming, setConfirming] = useState(false);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, startRefresh] = useTransition();
  const busy = working || refreshing;

  async function handleDelete() {
    setWorking(true);
    setError(null);
    try {
      await deleteAccount();
      startRefresh(() => router.refresh()); // there's no session anymore, so the sign-in screen appears
    } catch (e) {
      setError(messageFor(e));
      setWorking(false);
    }
  }

  return (
    <section
      aria-labelledby="delete-title"
      className="flex flex-col gap-3 border-t border-line pt-6"
    >
      <h2 id="delete-title" className="text-base font-semibold">
        Delete my account
      </h2>
      <p className="max-w-[60ch] text-sm text-ink-2">
        Disconnects every bank from Plaid and permanently deletes your account and all of its data.
        You’ll be signed out on every device.
      </p>
      {confirming ? (
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-sm font-medium">This can’t be undone. Delete everything?</span>
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
            onClick={handleDelete}
            disabled={busy}
            className="h-9 rounded-lg border border-danger px-3 text-sm font-semibold text-danger disabled:opacity-70"
          >
            {busy ? "Deleting…" : "Delete everything"}
          </button>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => setConfirming(true)}
          className="h-9 w-fit rounded-lg border border-field-line bg-surface px-3 text-sm font-medium text-danger hover:bg-muted"
        >
          Delete my account
        </button>
      )}
      {error && (
        <p role="alert" className="text-sm text-danger">
          {error}
        </p>
      )}
    </section>
  );
}
