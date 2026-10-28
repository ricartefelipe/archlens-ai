# Base fixada por digest (manifest list, cobre amd64 e arm64) para build
# reproduzível. Atualizar com:
#   docker pull node:22-alpine && \
#     docker inspect node:22-alpine --format '{{index .RepoDigests 0}}'
# Fixar digest não corrige CVE: reconstrua a imagem após atualizar o digest.
FROM node:22-alpine@sha256:fe4f6d74e20370acb04ad889077d14d0f64a9c8ea6a42bc60cddf1e844699914 AS build
WORKDIR /app

ARG NEXT_PUBLIC_API_URL=http://localhost:8080
ARG NEXT_PUBLIC_KEYCLOAK_URL=http://localhost:8180
ARG NEXT_PUBLIC_KEYCLOAK_REALM=archlens
ARG NEXT_PUBLIC_KEYCLOAK_CLIENT=archlens-frontend
ARG NEXT_PUBLIC_OIDC_ENABLED=false
ARG NEXT_PUBLIC_APP_NAME=ArchLens AI
ARG NEXT_PUBLIC_APP_LOGO_URL=
ARG NEXT_PUBLIC_PRIMARY_COLOR=#2563eb
ARG NEXT_PUBLIC_LANDING_URL=
ARG NEXT_PUBLIC_SUPPORT_URL=
# Destino dos rewrites de /v1, /public e /q (resolvidos no build do Next).
# Em deploy com nginx à frente, essas rotas nem chegam ao servidor Next.
ARG BACKEND_ORIGIN=http://localhost:8080

ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_KEYCLOAK_URL=$NEXT_PUBLIC_KEYCLOAK_URL
ENV NEXT_PUBLIC_KEYCLOAK_REALM=$NEXT_PUBLIC_KEYCLOAK_REALM
ENV NEXT_PUBLIC_KEYCLOAK_CLIENT=$NEXT_PUBLIC_KEYCLOAK_CLIENT
ENV NEXT_PUBLIC_OIDC_ENABLED=$NEXT_PUBLIC_OIDC_ENABLED
ENV NEXT_PUBLIC_APP_NAME=$NEXT_PUBLIC_APP_NAME
ENV NEXT_PUBLIC_APP_LOGO_URL=$NEXT_PUBLIC_APP_LOGO_URL
ENV NEXT_PUBLIC_PRIMARY_COLOR=$NEXT_PUBLIC_PRIMARY_COLOR
ENV NEXT_PUBLIC_LANDING_URL=$NEXT_PUBLIC_LANDING_URL
ENV NEXT_PUBLIC_SUPPORT_URL=$NEXT_PUBLIC_SUPPORT_URL
ENV BACKEND_ORIGIN=$BACKEND_ORIGIN
ENV NEXT_TELEMETRY_DISABLED=1

COPY package.json package-lock.json ./
RUN npm ci

RUN mkdir -p public
COPY . .
RUN npm run build

FROM node:22-alpine@sha256:fe4f6d74e20370acb04ad889077d14d0f64a9c8ea6a42bc60cddf1e844699914
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

# O runtime executa apenas `node server.js`. O npm/npx que acompanham a imagem
# base não são usados e carregavam as únicas vulnerabilidades HIGH da imagem
# final (em pacote, sigstore, brace-expansion e outros do próprio CLI).
RUN rm -rf /usr/local/lib/node_modules/npm /usr/local/bin/npm /usr/local/bin/npx

COPY --from=build /app/public ./public
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static

EXPOSE 3000
CMD ["node", "server.js"]
