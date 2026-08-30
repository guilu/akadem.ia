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
# Commit desplegado, para la banda de preproducción. Va como argumento porque
# dentro de la imagen no hay forma de averiguarlo: `.git` está en el
# .dockerignore y node:alpine no trae git. Vacío = la banda avisa sin versión.
ARG VITE_BUILD_SHA
ENV VITE_BUILD_SHA=$VITE_BUILD_SHA
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]
