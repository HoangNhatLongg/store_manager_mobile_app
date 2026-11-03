import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Permit the development browser on the local network to request dev assets.
  allowedDevOrigins: ["172.20.10.3"],
};

export default nextConfig;
