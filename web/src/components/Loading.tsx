import { Logo } from "./Logo";

function Spinner({ className }: { className: string }) {
  return (
    <span
      aria-hidden="true"
      className={`inline-block animate-spin rounded-full border-slate-200 border-t-blue-600 motion-reduce:animate-none ${className}`}
    />
  );
}

/** Whole-page loading screen: the logo and a spinner. */
export function LoadingScreen() {
  return (
    <div role="status" aria-live="polite" className="flex h-dvh flex-col items-center justify-center gap-8 bg-slate-50 dark:bg-black">
      <div className="animate-pulse motion-reduce:animate-none">
        <Logo />
      </div>
      <Spinner className="h-10 w-10 border-[3px]" />
      <span className="sr-only">Chargement...</span>
    </div>
  );
}

/** Covers the page while an action is running (sign in, for example). */
export function LoadingOverlay() {
  return (
    <div role="status" aria-live="polite" className="fixed inset-0 z-[60] flex flex-col items-center justify-center gap-8 bg-white/90 backdrop-blur-sm">
      <div className="animate-pulse motion-reduce:animate-none">
        <Logo />
      </div>
      <Spinner className="h-10 w-10 border-[3px]" />
      <span className="sr-only">Chargement...</span>
    </div>
  );
}

/** Centered spinner for the content of a page or a card. */
export function SectionLoader() {
  return (
    <div role="status" aria-live="polite" className="flex justify-center py-14">
      <Spinner className="h-9 w-9 border-[3px]" />
      <span className="sr-only">Chargement...</span>
    </div>
  );
}

/** Small spinner for a table row, a list item or a line of text. */
export function InlineLoader() {
  return (
    <span role="status" className="inline-flex items-center justify-center py-2 align-middle">
      <Spinner className="h-6 w-6 border-2" />
      <span className="sr-only">Chargement...</span>
    </span>
  );
}