"use client";

import { useState } from "react";
import type { Theme } from "@/lib/theme";
import { MoonIcon, SunIcon } from "./icons";

export function ThemeToggle({ initialTheme }: { initialTheme: Theme }) {
  const [theme, setTheme] = useState(initialTheme);
  const other = theme === "dark" ? "light" : "dark";
  const label = `Switch to ${other} theme`;

  function toggle() {
    document.documentElement.dataset.theme = other;
    document.cookie = `theme=${other}; path=/; max-age=31536000; samesite=lax`;
    setTheme(other);
  }

  return (
    <button
      type="button"
      onClick={toggle}
      aria-label={label}
      title={label}
      className="flex size-11 items-center justify-center rounded-lg border border-field-line bg-surface text-ink hover:bg-muted"
    >
      {theme === "dark" ? (
        <SunIcon className="size-[18px]" />
      ) : (
        <MoonIcon className="size-[18px]" />
      )}
    </button>
  );
}
