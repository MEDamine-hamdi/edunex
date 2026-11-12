"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { StudentShell } from "@/components/StudentShell";
import { ErrorBox, PageHeader, primaryBtn, smallBtn } from "@/components/ui";
import { ApiError, apiFetch } from "@/lib/api";

type Info = { courseId: number; title: string; subjectName: string; completed: boolean; attempts: number };
type Question = { index: number; question: string; options: string[] };
type Quiz = { id: number; courseId: number; courseTitle: string; questions: Question[] };
type Item = { index: number; question: string; options: string[]; chosen: number | null; correct: number; right: boolean; explanation: string };
type Result = { id: number; courseTitle: string; score: number; total: number; items: Item[] };
type Attempt = { id: number; createdAt: string; score: number | null; total: number; submitted: boolean };
type State = { quiz: Quiz | null; result: Result | null };

function explain(e: unknown): string {
  if (!(e instanceof ApiError)) return "Une erreur est survenue.";
  if (e.status === 503) return "Le service d'IA est momentanément indisponible. Réessayez dans un instant.";
  if (e.status === 429) return "Trop de quiz générés : patientez quelques minutes.";
  if (e.status === 400 && e.message.includes("too little readable text")) return "Les documents de ce cours contiennent trop peu de texte lisible (les documents scannés ne peuvent pas être lus).";
  if (e.status === 400 && e.message.includes("no document")) return "Ce cours ne contient aucun document : le quiz ne peut pas être généré.";
  if (e.status === 403) return "Vous n'avez pas accès à ce cours.";
  return e.message;
}

export default function CourseQuizPage() {
  const params = useParams<{ id: string }>();
  const courseId = Number(params.id);
  const [info, setInfo] = useState<Info | null>(null);
  const [history, setHistory] = useState<Attempt[]>([]);
  const [quiz, setQuiz] = useState<Quiz | null>(null);
  const [answers, setAnswers] = useState<(number | null)[]>([]);
  const [result, setResult] = useState<Result | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [i, h] = await Promise.all([
        apiFetch<Info>(`/api/student/courses/${courseId}/quiz-info`),
        apiFetch<Attempt[]>(`/api/student/courses/${courseId}/quizzes`),
      ]);
      setInfo(i);
      setHistory(h);
    } catch (e) {
      if (!(e instanceof ApiError && e.status === 401)) setError(explain(e));
    }
  }, [courseId]);

  useEffect(() => {
    load();
  }, [load]);

  async function start() {
    setBusy(true);
    setError(null);
    try {
      await apiFetch(`/api/student/courses/${courseId}/complete`, { method: "POST" });
      const q = await apiFetch<Quiz>(`/api/student/courses/${courseId}/quizzes`, { method: "POST" });
      setQuiz(q);
      setAnswers(q.questions.map(() => null));
      setResult(null);
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }

  async function open(id: number) {
    setError(null);
    try {
      const s = await apiFetch<State>(`/api/student/quizzes/${id}`);
      if (s.quiz) {
        setQuiz(s.quiz);
        setAnswers(s.quiz.questions.map(() => null));
        setResult(null);
      } else {
        setQuiz(null);
        setResult(s.result);
      }
    } catch (e) {
      setError(explain(e));
    }
  }

  async function submit() {
    if (!quiz) return;
    const skipped = answers.filter((a) => a === null).length;
    if (skipped > 0 && !window.confirm(`${skipped} question(s) sans réponse. Valider quand même ?`)) return;
    setBusy(true);
    setError(null);
    try {
      const r = await apiFetch<Result>(`/api/student/quizzes/${quiz.id}/submit`, { method: "POST", body: JSON.stringify({ answers }) });
      setResult(r);
      setQuiz(null);
      load();
    } catch (e) {
      setError(explain(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <StudentShell>
      <PageHeader title={`Quiz : ${info?.title ?? ""}`} subtitle="Des questions générées par l'IA à partir des documents du cours, pour vérifier que vous avez bien compris." />
      <ErrorBox message={error} />

      {busy && !quiz && !result && (
        <div className="mt-8 flex items-center gap-3 rounded-2xl border border-slate-200 bg-white p-6 text-slate-600">
          <span className="inline-block h-6 w-6 animate-spin rounded-full border-2 border-slate-200 border-t-blue-600" />
          L&apos;IA prépare votre quiz à partir du cours, cela prend quelques secondes...
        </div>
      )}

      {!quiz && !result && !busy && (
        <>
          <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="text-lg font-semibold">Vous avez terminé ce cours ?</h2>
            <p className="mt-2 text-sm text-slate-600">Le quiz est généré à partir du texte des documents du cours. Chaque question a 4 réponses possibles ; vous voyez la correction et une explication à la fin.</p>
            <div className="mt-5 flex flex-wrap gap-3">
              <button onClick={start} className={primaryBtn}>Cours terminé : générer mon quiz</button>
              <Link href="/student/courses" className={smallBtn}>Retour aux cours</Link>
            </div>
          </section>
          {history.length > 0 && (
            <section className="mt-6">
              <h2 className="text-lg font-semibold">Mes quiz</h2>
              <ul className="mt-3 space-y-2">
                {history.map((h) => (
                  <li key={h.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white px-5 py-3 shadow-sm">
                    <span className="text-sm text-slate-700">{new Date(h.createdAt).toLocaleString(undefined, { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" })}</span>
                    <span className={`rounded-full px-3 py-1 text-xs font-semibold ${h.submitted ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-800"}`}>{h.submitted ? `${h.score}/${h.total}` : "Non terminé"}</span>
                    <button onClick={() => open(h.id)} className={smallBtn}>{h.submitted ? "Voir la correction" : "Reprendre"}</button>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </>
      )}

      {quiz && (
        <div className="mt-6 space-y-5">
          <p className="text-sm text-slate-500">{answers.filter((a) => a !== null).length} / {quiz.questions.length} réponses</p>
          {quiz.questions.map((q) => (
            <fieldset key={q.index} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <legend className="px-1 text-sm font-semibold text-slate-900">{q.index + 1}. {q.question}</legend>
              <div className="mt-3 space-y-2">
                {q.options.map((o, i) => (
                  <label key={i} className={`flex cursor-pointer items-start gap-3 rounded-xl border px-4 py-2.5 text-sm ${answers[q.index] === i ? "border-blue-500 bg-blue-50" : "border-slate-200 hover:bg-slate-50"}`}>
                    <input type="radio" name={`q${q.index}`} checked={answers[q.index] === i} onChange={() => setAnswers((a) => a.map((v, k) => (k === q.index ? i : v)))} className="mt-1 h-4 w-4 accent-blue-600" />
                    <span className="text-slate-800">{o}</span>
                  </label>
                ))}
              </div>
            </fieldset>
          ))}
          <button onClick={submit} disabled={busy} className={primaryBtn}>{busy ? "Correction..." : "Valider mes réponses"}</button>
        </div>
      )}

      {result && (
        <div className="mt-6 space-y-5">
          <section className={`rounded-2xl border p-6 text-center shadow-sm ${result.score >= result.total / 2 ? "border-emerald-200 bg-emerald-50" : "border-orange-200 bg-orange-50"}`}>
            <p className="text-sm font-semibold text-slate-600">Votre score</p>
            <p className="mt-1 text-4xl font-extrabold text-slate-900">{result.score} <span className="text-xl font-medium text-slate-500">/ {result.total}</span></p>
            <p className="mt-2 text-sm text-slate-600">{result.score === result.total ? "Parfait : vous maîtrisez ce cours." : result.score >= result.total / 2 ? "Bien : relisez les points manqués ci-dessous." : "Relisez le cours, puis refaites un quiz."}</p>
          </section>
          {result.items.map((it) => (
            <article key={it.index} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <p className="text-sm font-semibold text-slate-900">{it.index + 1}. {it.question}</p>
              <ul className="mt-3 space-y-1.5">
                {it.options.map((o, i) => (
                  <li key={i} className={`rounded-xl border px-4 py-2 text-sm ${i === it.correct ? "border-emerald-400 bg-emerald-50 text-emerald-900" : i === it.chosen ? "border-red-300 bg-red-50 text-red-800" : "border-slate-200 text-slate-700"}`}>
                    {o}{i === it.correct ? "  ✓" : i === it.chosen ? "  ✗ votre réponse" : ""}
                  </li>
                ))}
              </ul>
              {it.chosen === null && <p className="mt-2 text-xs text-slate-500">Sans réponse</p>}
              <p className="mt-3 rounded-xl bg-slate-50 px-4 py-2 text-sm text-slate-600">{it.explanation}</p>
            </article>
          ))}
          <div className="flex flex-wrap gap-3">
            <button onClick={start} disabled={busy} className={primaryBtn}>Refaire un nouveau quiz</button>
            <button onClick={() => { setResult(null); load(); }} className={smallBtn}>Mes quiz</button>
            <Link href="/student/courses" className={smallBtn}>Retour aux cours</Link>
          </div>
        </div>
      )}
    </StudentShell>
  );
}