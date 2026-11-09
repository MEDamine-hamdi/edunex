"use client";

import { useEffect } from "react";
import { DICT } from "@/i18n/dictionary";

// translated labels point back to the French label, so icons also work in English and Arabic
const REVERSE = new Map<string, string>();
for (const [fr, v] of Object.entries(DICT)) {
  REVERSE.set(v.en.trim().toLowerCase(), fr.toLowerCase());
  REVERSE.set(v.ar.trim(), fr.toLowerCase());
}

const RULES: [RegExp, string][] = [
  [/^(supprimer|retirer)/, "trash"],
  [/^(modifier|renommer|éditer)/, "pencil"],
  [/^(annuler|fermer|refuser|rejeter|désactiver)/, "x"],
  [/^tout marquer/, "check-all"],
  [/^(enregistrer|confirmer|valider|approuver|accepter|publier|créer|activer)/, "check"],
  [/^(ajouter|nouveau|nouvelle|planifier)/, "plus"],
  [/^télécharger/, "download"],
  [/^(envoyer le document|joindre)/, "upload"],
  [/^envoyer/, "send"],
  [/^(réanalyser|réessayer|actualiser|remplacer)/, "refresh"],
  [/^se déconnecter/, "log-out"],
  [/^se connecter/, "log-in"],
  [/^rechercher/, "search"],
  [/^ouvrir/, "external"],
  [/^voir/, "eye"],
];

function iconFor(text: string): string | null {
  let t = text.trim().toLowerCase();
  if (t.length < 2 || t.length > 45) return null;
  const plus = t.startsWith("+");
  t = t.replace(/^[+←→]\s*/, "");
  const french = REVERSE.get(t) ?? t;
  for (const [re, icon] of RULES) if (re.test(french)) return icon;
  return plus ? "plus" : null;
}

function process(root: ParentNode) {
  root.querySelectorAll<HTMLElement>("button, a").forEach((el) => {
    if (el.hasAttribute("data-no-icon") || el.querySelector("svg, img") || el.children.length > 0) return;
    const icon = iconFor(el.textContent ?? "");
    const auto = el.getAttribute("data-icon-auto") === "1";
    if (icon) {
      if (el.getAttribute("data-icon") !== icon) el.setAttribute("data-icon", icon);
      el.setAttribute("data-icon-auto", "1");
    } else if (auto) {
      el.removeAttribute("data-icon");
      el.removeAttribute("data-icon-auto");
    }
  });
}

export function IconEnhancer() {
  useEffect(() => {
    let frame = 0;
    const run = () => {
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(() => process(document));
    };
    run();
    const observer = new MutationObserver(run);
    observer.observe(document.body, { childList: true, subtree: true, characterData: true });
    return () => {
      cancelAnimationFrame(frame);
      observer.disconnect();
    };
  }, []);
  return null;
}