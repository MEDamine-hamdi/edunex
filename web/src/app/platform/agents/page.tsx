"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

export default function PlatformAgentsRedirect() {
  const router = useRouter();
  useEffect(() => {
    router.replace("/platform/universities");
  }, [router]);
  return null;
}