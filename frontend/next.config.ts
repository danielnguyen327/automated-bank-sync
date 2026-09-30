import type { NextConfig } from "next";

const apiURL = process.env.API_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  async rewrites() {
    return [{ source: "/api/:path*",
              destination: `${apiURL}/api/:path*`
    }];
  },
};

export default nextConfig;
