#!/bin/sh
# Runs once via nginx's docker-entrypoint.d hook mechanism at container
# start. certbot's renew loop (see the `certbot` service in
# docker-compose.yml) only rewrites the cert files on disk — nginx keeps
# the old certificate in memory until it reloads, so without this it would
# keep serving an expired cert after the ~90-day renewal. A periodic
# reload is cheap and doesn't drop active connections.
( while true; do sleep 12h; nginx -s reload; done ) &
