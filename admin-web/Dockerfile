FROM node:24-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
# "/" 로 두면 api.ts 가 끝 슬래시를 떼어 빈 문자열이 되고, API 호출이 같은 출처로 나간다.
# 관리자 웹과 백엔드가 ALB 한 대를 공유하므로 ALB 주소를 빌드에 박을 필요가 없다 —
# 덕분에 이미지를 미리 만들어 둘 수 있다. 로컬 compose 에서만 localhost:8080 을 넣는다
ARG VITE_API_BASE_URL=/
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
