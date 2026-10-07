"use client";

import { useRouter } from "next/navigation";
import { useId, useState, useTransition, type FormEvent } from "react";
import { ApiError, createAccount, signIn } from "@/lib/api";

type Mode = "sign-in" | "create-account";

const copy = {
  "sign-in": {
    title: "Sign in",
    intro: "Welcome back. Your accounts are right where you left them.",
    submit: "Sign in",
    busy: "Signing in…",
    switchPrompt: "New to LedgerSync?",
    switchLabel: "Create an account",
  },
  "create-account": {
    title: "Create an account",
    intro: "Sign up with your email to try LedgerSync with Plaid’s test banks.",
    submit: "Create account",
    busy: "Creating your account…",
    switchPrompt: "Already have an account?",
    switchLabel: "Sign in",
  },
} as const;

const labelClass = "flex flex-col gap-1.5 text-[13px] font-medium";
const inputClass =
  "h-11 rounded-lg border border-field-line bg-field px-3 text-sm font-normal text-ink placeholder:text-ink-3 focus:border-accent";

export function AuthForm() {
  const router = useRouter();
  const passwordHintId = useId();
  const [mode, setMode] = useState<Mode>("sign-in");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [refreshing, startRefresh] = useTransition();
  const busy = submitting || refreshing;
  const creating = mode === "create-account";
  const text = copy[mode];

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const email = String(form.get("email"));
    const password = String(form.get("password"));
    setSubmitting(true);
    setError(null);
    try {
      if (creating) await createAccount(String(form.get("name")), email, password);
      else await signIn(email, password);
      // The page asks the backend who is signed in again, and now shows the app.
      startRefresh(() => router.refresh());
    } catch (e) {
      setError(
        e instanceof ApiError
          ? e.message
          : "Can’t reach LedgerSync right now. Try again in a moment.",
      );
    } finally {
      setSubmitting(false);
    }
  }

  function switchMode() {
    setMode(creating ? "sign-in" : "create-account");
    setError(null);
  }

  return (
    <section
      aria-labelledby="auth-title"
      className="flex w-full max-w-[420px] flex-col gap-5 rounded-2xl border border-line bg-surface p-6 sm:p-8"
    >
      <div className="flex flex-col gap-1">
        <h2 id="auth-title" className="text-2xl font-semibold">
          {text.title}
        </h2>
        <p className="text-sm text-ink-2">{text.intro}</p>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {creating && (
          <label className={labelClass}>
            Name
            <input
              name="name"
              required
              maxLength={100}
              autoComplete="name"
              className={inputClass}
            />
          </label>
        )}
        <label className={labelClass}>
          Email
          <input
            name="email"
            type="email"
            required
            autoComplete="email"
            placeholder="you@example.com"
            className={inputClass}
          />
        </label>
        <label className={labelClass}>
          Password
          <input
            name="password"
            type="password"
            required
            minLength={creating ? 12 : undefined}
            maxLength={creating ? 72 : undefined}
            autoComplete={creating ? "new-password" : "current-password"}
            aria-describedby={creating ? passwordHintId : undefined}
            className={inputClass}
          />
          {creating && (
            <span id={passwordHintId} className="text-xs font-normal text-ink-3">
              At least 12 characters.
            </span>
          )}
        </label>

        {error && (
          <p role="alert" className="text-sm text-danger">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={busy}
          className="h-11 rounded-lg bg-accent text-[15px] font-semibold text-on-accent disabled:opacity-70"
        >
          {busy ? text.busy : text.submit}
        </button>
      </form>

      <p className="text-center text-sm text-ink-2">
        {text.switchPrompt}{" "}
        <button
          type="button"
          onClick={switchMode}
          className="font-medium text-accent hover:underline"
        >
          {text.switchLabel}
        </button>
      </p>
    </section>
  );
}
