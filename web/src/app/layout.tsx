import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "EduNex | Plateforme universitaire intelligente",
  description: "Votre université, dans un seul espace.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="fr">
      <body className="bg-white text-slate-900">{children}</body>
    </html>
  );
}