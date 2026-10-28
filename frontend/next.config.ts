import type { NextConfig } from "next";

import { API_PREFIXES } from "./src/lib/api-routes";

// A UI chama a API na mesma origem (ver src/lib/api-base.ts). Em produção o
// nginx roteia /v1, /public e /q para o backend; sem proxy externo (Compose
// local, dev), o próprio servidor Next encaminha essas rotas, senão o POST de
// login cai no proxy de rotas protegidas.
//
// Os rewrites são resolvidos no build: BACKEND_ORIGIN precisa estar definido
// como build arg na imagem (ver Dockerfile).
const backendOrigin = process.env.BACKEND_ORIGIN ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  output: "standalone",
  rewrites() {
    return {
      beforeFiles: API_PREFIXES.map((prefix) => ({
        source: `${prefix}/:path*`,
        destination: `${backendOrigin}${prefix}/:path*`,
      })),
      afterFiles: [],
      fallback: [],
    };
  },
};

export default nextConfig;
