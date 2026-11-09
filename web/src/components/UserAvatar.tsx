"use client";

import { useEffect, useState } from "react";
import { fetchBlobUrl } from "@/lib/blob";

const cache = new Map<string, Promise<string | null>>();

function load(path: string): Promise<string | null> {
  let p = cache.get(path);
  if (!p) {
    p = fetchBlobUrl(path).catch(() => null);
    cache.set(path, p);
  }
  return p;
}

export function forgetAvatar(path: string) {
  cache.delete(path);
}

/** A profile picture, or the initials when there is none. */
export function UserAvatar({ path, name, size = 40 }: { path: string | null; name: string; size?: number }) {
  const [src, setSrc] = useState<string | null>(null);

  useEffect(() => {
    let off = false;
    setSrc(null);
    if (path) load(path).then((u) => !off && setSrc(u));
    return () => {
      off = true;
    };
  }, [path]);

  const initials = name.split(" ").filter(Boolean).slice(0, 2).map((p) => p[0]?.toUpperCase()).join("");
  const style = { width: size, height: size, minWidth: size };
  return src ? (
    // eslint-disable-next-line @next/next/no-img-element
    <img src={src} alt={name} style={style} className="rounded-full object-cover" />
  ) : (
    <span style={style} className="flex items-center justify-center rounded-full bg-blue-100 text-xs font-bold text-blue-700">{initials || "?"}</span>
  );
}