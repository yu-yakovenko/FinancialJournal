#!/usr/bin/env bash
# One-time bootstrap for HTTPS via Let's Encrypt.
#
# nginx refuses to start with a 443 server block that points at cert files
# which don't exist yet, and certbot needs nginx already serving the ACME
# webroot challenge before it can issue those files — so the first run needs
# a throwaway self-signed cert just to get nginx up, which gets replaced by
# the real certbot-issued one before nginx is reloaded. Re-running this script
# after certs already exist is safe (it skips straight to the real request).
set -euo pipefail

if [ -z "${1:-}" ]; then
  echo "Usage: ./init-letsencrypt.sh <domain> [email]"
  echo "  <domain> must match the one already baked into frontend/nginx.conf's"
  echo "  server_name / ssl_certificate paths, e.g. tonique-finance.duckdns.org"
  exit 1
fi

DOMAIN="$1"
EMAIL="${2:-}"
EMAIL_ARG="--register-unsafely-without-email"
if [ -n "$EMAIL" ]; then
  EMAIL_ARG="--email $EMAIL --no-eff-email"
fi

echo "### Creating a dummy self-signed cert so nginx can start ###"
# --entrypoint with a shell-operator string ("&&") silently truncates at the
# operator instead of running it as a shell would — must route through
# `sh -c` explicitly to get real shell semantics.
docker compose run --rm --entrypoint sh certbot -c "\
  mkdir -p /etc/letsencrypt/live/$DOMAIN && \
  openssl req -x509 -nodes -newkey rsa:2048 -days 1 \
    -keyout /etc/letsencrypt/live/$DOMAIN/privkey.pem \
    -out /etc/letsencrypt/live/$DOMAIN/fullchain.pem \
    -subj '/CN=localhost'"

echo "### Starting all services (frontend will start clean using the dummy cert) ###"
docker compose up -d --build

echo "### Deleting dummy cert, requesting the real one from Let's Encrypt ###"
docker compose run --rm --entrypoint sh certbot -c "\
  rm -rf /etc/letsencrypt/live/$DOMAIN && \
  rm -rf /etc/letsencrypt/archive/$DOMAIN && \
  rm -rf /etc/letsencrypt/renewal/$DOMAIN.conf"

docker compose run --rm --entrypoint certbot certbot certonly --webroot -w /var/www/certbot \
    -d "$DOMAIN" \
    $EMAIL_ARG \
    --agree-tos --non-interactive

echo "### Reloading nginx with the real certificate ###"
docker compose exec frontend nginx -s reload

echo "Done. https://$DOMAIN should now serve a trusted certificate."
echo "The 'certbot' service in docker-compose.yml will keep renewing it automatically."
