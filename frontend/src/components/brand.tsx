import { SyncIcon } from "./icons";

export function Brand() {
  return (
    <div className="flex items-center gap-2.5">
      <span className="flex size-8 items-center justify-center rounded-lg bg-accent text-on-accent">
        <SyncIcon className="size-[18px]" />
      </span>
      <span className="text-[17px] font-semibold tracking-tight">LedgerSync</span>
      <span className="rounded-full bg-sandbox px-2 py-0.5 text-[11px] font-medium tracking-wider text-sandbox-ink uppercase">
        Sandbox
      </span>
    </div>
  );
}
