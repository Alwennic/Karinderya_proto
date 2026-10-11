FROM nginx:1.27-alpine

# Build number shown in the page footer (Jenkins passes TAG through docker compose)
ARG BUILD_NUMBER=dev

COPY src/ /usr/share/nginx/html/
RUN sed -i "s/__BUILD_NUMBER__/${BUILD_NUMBER}/g" /usr/share/nginx/html/*.html

EXPOSE 80
