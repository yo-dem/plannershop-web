# ---- Build: compila Angular ----
FROM node:26-alpine AS build
WORKDIR /src

# Prima solo i manifest: node_modules resta in cache finché non cambiano.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Runtime: nginx serve i file statici e inoltra /api al backend ----
FROM nginx:1-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
