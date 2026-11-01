FROM node:22-alpine AS ui
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build
FROM node:22-alpine
WORKDIR /app
COPY --from=ui --chown=node:node /build/frontend/dist/cage/browser/ ./public/
COPY --chown=node:node demo-server.mjs ./
ENV STATIC_DIR=/app/public
ENV PORT=8080
USER node
EXPOSE 8080
CMD ["node", "demo-server.mjs"]
