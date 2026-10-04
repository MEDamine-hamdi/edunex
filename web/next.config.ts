import type { NextConfig } from "next";

const backend = process.env.BACKEND_URL ?? "http://localhost:8099";

const nextConfig: NextConfig = {
  // The browser calls /api/... on Next.js, which forwards it to Spring Boot (no CORS needed)
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${backend}/api/:path*` }];
  },
};

export default nextConfig;