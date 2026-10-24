"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { hasSession } from "@/lib/auth";

/** A signed-in user who lands on the login page (back button) goes straight to the dashboard. */
export function RedirectIfLoggedIn() {
  const router = useRouter();
  useEffect(() => {
    let off = false;
    const check = async () => {
      if ((await hasSession()) && !off) router.replace("/dashboard");
    };
    check();
    window.addEventListener("pageshow", check);
    return () => {
      off = true;
      window.removeEventListener("pageshow", check);
    };
  }, [router]);
  return null;
}