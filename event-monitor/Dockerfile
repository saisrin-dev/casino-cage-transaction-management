FROM node:22-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev
COPY src ./src
RUN mkdir -p /app/data && chown -R node:node /app
USER node
EXPOSE 3001
CMD ["node","src/server.js"]
