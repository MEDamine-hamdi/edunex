import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { ThemeSwitcher } from "@/components/ThemeSwitcher";
import { LoginForm } from "@/components/LoginForm";
import { Logo } from "@/components/Logo";
import { RedirectIfLoggedIn } from "@/components/RedirectIfLoggedIn";

const points = [
  "Vos cours et documents, toujours à portée de main",
  "Un emploi du temps à jour, avec les séances annulées",
  "Un assistant qui répond à vos questions",
  "Vos demandes de documents, sans faire la queue",
];

export default function LoginPage() {
  return (
    <main className="relative grid min-h-screen lg:grid-cols-[1.1fr_1fr]">
      <RedirectIfLoggedIn />
      <header className="absolute inset-x-0 top-0 z-10 flex justify-end px-6 py-5">
        <div className="flex items-center gap-3">
          <ThemeSwitcher />
          <LanguageSwitcher />
        </div>
      </header>

      <section className="relative hidden flex-col justify-between overflow-hidden bg-linear-to-br from-amber-50 via-rose-50 to-indigo-100 px-12 py-10 lg:flex xl:px-20">
        <div className="pointer-events-none absolute -start-24 top-1/4 h-96 w-96 rounded-full bg-amber-200/40 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-20 end-0 h-96 w-96 rounded-full bg-indigo-200/50 blur-3xl" />
        <div className="relative">
          <Logo />
        </div>
        <div className="relative max-w-xl">
          <h1 className="text-5xl font-extrabold leading-tight tracking-tight text-stone-900">
            Votre université, plus simple au quotidien.
          </h1>
          <p className="mt-5 text-lg leading-relaxed text-stone-600">
            Cours, emploi du temps, notes, demandes administratives et un assistant pour vous aider : tout est au même endroit, sur ordinateur comme sur téléphone.
          </p>
          <ul className="mt-8 space-y-3">
            {points.map((p) => (
              <li key={p} className="flex w-fit items-center gap-3 rounded-2xl bg-white/70 px-5 py-3 text-[15px] text-stone-700 shadow-sm backdrop-blur">
                <span className="h-2 w-2 shrink-0 rounded-full bg-indigo-500" />
                {p}
              </li>
            ))}
          </ul>
        </div>
        <p className="relative text-lg italic text-stone-500">Apprendre aujourd&apos;hui, construire demain.</p>
      </section>

      <section className="flex items-center justify-center bg-stone-50 px-6 py-24">
        <div className="w-full max-w-md">
          <div className="mb-6 lg:hidden">
            <Logo />
          </div>
          <div className="rounded-3xl bg-white p-8 shadow-xl shadow-stone-900/5 ring-1 ring-stone-100 sm:p-10">
            <h2 className="text-3xl font-extrabold tracking-tight text-stone-900">Heureux de vous revoir</h2>
            <p className="mt-2 text-stone-600">
              Connectez-vous pour retrouver vos cours, votre emploi du temps et vos notes.
            </p>
            <LoginForm />
          </div>
        </div>
      </section>
    </main>
  );
}