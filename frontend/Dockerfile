FROM node:24-alpine AS builder

WORKDIR /app

COPY package*.json ./

RUN --mount=type=cache,target=/root/.npm,id=stentio-npm-frontend \
    npm ci --prefer-offline

COPY . .

RUN --mount=type=cache,target=/tmp/metro-cache,id=stentio-metro-frontend \
    npx expo export --platform web

FROM nginx:alpine AS runner

COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
