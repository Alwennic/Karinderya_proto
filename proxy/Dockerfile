FROM nginx:1.27-alpine
# Config is baked into the image (no bind mount) so it works when Jenkins deploys
COPY nginx.conf /etc/nginx/conf.d/default.conf