"use client";

import { useEffect, useState } from "react";

const labels = {
  checking: "Checking the API...",
  up: "API and database are up",
  down: "API and database are down",
};

export function ApiStatus() {
  const [status, setStatus] = useState<keyof typeof labels>("checking");

  useEffect(() => {
    fetch("/api/health")
      .then((res) => setStatus(res.ok ? "up" : "down"))
      .catch(() => setStatus("down"));
  }, []);

  return (
    <p role="status" className="text-sm text-ink-3">
      {labels[status]}
    </p>
  );
}
