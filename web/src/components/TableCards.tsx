"use client";

import { useEffect } from "react";

/** Gives each table cell the label of its column, so the phone layout can show a table as a list of cards. */
function enhance() {
  document.querySelectorAll<HTMLTableElement>("table:not([data-keep-table])").forEach((table) => {
    const heads = Array.from(table.querySelectorAll("thead th")).map((th) => (th.textContent ?? "").trim());
    table.querySelectorAll<HTMLTableRowElement>("tbody tr").forEach((tr) => {
      const cells = Array.from(tr.children) as HTMLElement[];
      cells.forEach((td, i) => {
        if (td.hasAttribute("colspan")) return;
        const label = heads[i] ?? "";
        const actions = /^actions?$/i.test(label) || (label === "" && i === cells.length - 1 && i > 0);
        if (actions) {
          if (!td.hasAttribute("data-actions")) td.setAttribute("data-actions", "1");
        } else if (label && td.getAttribute("data-label") !== label) {
          td.setAttribute("data-label", label);
        }
      });
    });
  });
}

export function TableCards() {
  useEffect(() => {
    let frame = 0;
    const run = () => {
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(enhance);
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