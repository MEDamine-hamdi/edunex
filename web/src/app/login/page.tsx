import { LoginForm } from "@/components/LoginForm";
import { Logo } from "@/components/Logo";
import {
  BookIcon,
  BotIcon,
  CalendarIcon,
  ChartIcon,
  ChevronDownIcon,
  GlobeIcon,
} from "@/components/icons";

const features = [
  { icon: BookIcon, title: "Cours & documents", text: "Accédez à tous vos supports organisés par matière.", tint: "bg-violet-100 text-violet-600" },
  { icon: CalendarIcon, title: "Emploi du temps", text: "Consultez vos cours où que vous soyez.", tint: "bg-emerald-100 text-emerald-600" },
  { icon: ChartIcon, title: "Notes & absences", text: "Suivez votre progression en temps réel.", tint: "bg-orange-100 text-orange-500" },
  { icon: BotIcon, title: "Assistant IA", text: "Obtenez des réponses fiables à partir des documents de votre université.", tint: "bg-blue-100 text-blue-600" },
];

export default function LoginPage() {
  return (
    <main className="grid min-h-screen lg:grid-cols-[1.15fr_1fr]">
      {/* Left: presentation */}
      <section className="relative hidden flex-col justify-between overflow-hidden bg-linear-to-br from-sky-100 via-blue-50 to-white px-12 py-10 lg:flex xl:px-20">
        <div className="pointer-events-none absolute -left-24 top-1/3 h-96 w-96 rounded-full bg-sky-200/50 blur-3xl" />
        <div className="pointer-events-none absolute bottom-0 right-0 h-80 w-80 rounded-full bg-violet-200/40 blur-3xl" />

        <div className="relative">
          <div className="flex items-center gap-4">
            <Logo />
            <span className="mt-1 text-sm text-slate-600">Plateforme universitaire intelligente</span>
          </div>

          <h1 className="mt-14 text-5xl font-extrabold leading-[1.05] tracking-tight text-slate-950 xl:text-6xl">
            Votre université,
            <br />
            dans un{" "}
            <span className="bg-linear-to-r from-violet-600 to-blue-600 bg-clip-text text-transparent">
              seul espace
            </span>
          </h1>
          <p className="mt-6 max-w-xl text-lg leading-relaxed text-slate-600">
            EduNex centralise la gestion académique et administrative, l&apos;e-learning et un
            assistant IA personnalisé, accessibles sur Web et Mobile.
          </p>

          <div className="mt-10 grid max-w-3xl grid-cols-2 gap-6 xl:grid-cols-4">
            {features.map(({ icon: Icon, title, text, tint }) => (
              <div key={title}>
                <div className={`flex h-14 w-14 items-center justify-center rounded-2xl ${tint}`}>
                  <Icon className="h-7 w-7" />
                </div>
                <h3 className="mt-3 text-sm font-semibold text-slate-900">{title}</h3>
                <p className="mt-1 text-sm leading-snug text-slate-500">{text}</p>
              </div>
            ))}
          </div>
        </div>

        <div className="relative mt-12 w-fit rounded-2xl border border-white/60 bg-white/50 px-6 py-5 shadow-sm backdrop-blur">
          <p className="text-lg italic text-slate-700">
            {"“Apprendre aujourd'hui,"}
            <br />
            {"construire demain.”"}
          </p>
          <span className="mt-2 block h-0.5 w-16 rounded bg-indigo-400" />
        </div>
      </section>

      {/* Right: form */}
      <section className="relative flex items-center justify-center bg-linear-to-br from-white via-slate-50 to-indigo-50 px-6 py-10">
        <button
          type="button"
          className="absolute right-6 top-6 flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-800 shadow-sm"
        >
          <GlobeIcon className="h-5 w-5" /> FR <ChevronDownIcon className="h-4 w-4" />
        </button>

        <div className="w-full max-w-[34rem] rounded-3xl border border-slate-100 bg-white p-8 shadow-2xl shadow-indigo-900/10 sm:p-10">
          <div className="flex flex-col items-center text-center">
            <Logo size="lg" />
            <p className="mt-2 text-xl text-slate-600">Connexion à votre compte</p>
          </div>
          <LoginForm />
        </div>
      </section>
    </main>
  );
}