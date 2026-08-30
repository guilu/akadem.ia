FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json* pnpm-lock.yaml* yarn.lock* ./
RUN if [ -f yarn.lock ]; then yarn --frozen-lockfile; elif [ -f pnpm-lock.yaml ]; then npm i -g pnpm && pnpm i --frozen-lockfile; else npm i; fi;
ARG VITE_STRIPE_PUBLISHABLE_KEY
ENV VITE_STRIPE_PUBLISHABLE_KEY=$VITE_STRIPE_PUBLISHABLE_KEY
# GA4 measurement ID (public, not a secret). Empty value disables analytics
# entirely — no gtag script, no consent banner.
ARG VITE_GA_MEASUREMENT_ID
ENV VITE_GA_MEASUREMENT_ID=$VITE_GA_MEASUREMENT_ID
# Commit desplegado, para la banda de preproducción.
#
# Va como argumento porque dentro de la imagen no hay forma de averiguarlo: el
# contexto de build es `./frontend` y `.git` vive en la raíz del repositorio,
# así que ni siquiera está en el contexto. Quien construye lo pasa:
#
#   VITE_BUILD_SHA=$(git rev-parse --short HEAD) docker compose up --build -d
#
# Sin él se cae a la fecha del build, más abajo.
ARG VITE_BUILD_SHA
ENV VITE_BUILD_SHA=$VITE_BUILD_SHA
COPY . .

# Sin sha la banda se quedaba sin segunda línea, que es lo que pasaba en cada
# despliegue porque `docker compose up --build` no pasa el argumento. La fecha
# del build no es el commit, pero es un hecho cierto que la imagen conoce de sí
# misma y responde a lo que uno se pregunta mirando una preproducción.
RUN if [ -z "$VITE_BUILD_SHA" ]; then \
      export VITE_BUILD_SHA="$(date -u +%m-%d\ %H:%M)"; \
    fi; \
    npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]
