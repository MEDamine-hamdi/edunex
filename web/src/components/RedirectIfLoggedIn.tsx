"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { getToken } from "@/lib/auth";

/** A signed-in user who lands on the login page (back button) goes straight to the dashboard. */
export function RedirectIfLoggedIn() {
  const router = useRouter();
  useEffect(() => {
    const check = () => {
      if (getToken()) router.replace("/dashboard");
    };
    check();
    window.addEventListener("pageshow", check);
    return () => window.removeEventListener("pageshow", check);
  }, [router]);
  return null;
}