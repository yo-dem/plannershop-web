# ---- Build: compile Angular ----
FROM node:26-alpine AS build
WORKDIR /src

# Manifests first: node_modules stays cached until they change.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Runtime: nginx serves the static files and forwards /api to the backend ----
FROM nginx:1-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
