#!/usr/bin/env bash
# Executado na EC2 por aws-setup-tls-ec2.sh.
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/opt/archlens/archlens-ai}"
cd "$DEPLOY_DIR"

ARCHLENS_DOMAIN="${ARCHLENS_DOMAIN:?ARCHLENS_DOMAIN obrigatório}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:-}"

COMPOSE="sudo docker-compose"
CF="--env-file .env.aws-pilot -f docker-compose.prod.yml -f docker-compose.prod.tls.yml"

cp -f deploy/nginx/conf.d/default-pilot.conf deploy/nginx/conf.d/default-pilot-active.conf

$COMPOSE $CF up -d nginx
sleep 3

EMAIL_ARG=""
if [[ -n "$CERTBOT_EMAIL" ]]; then
  EMAIL_ARG="--email $CERTBOT_EMAIL"
else
  EMAIL_ARG="--register-unsafely-without-email"
fi

$COMPOSE $CF run --rm --entrypoint certbot certbot certonly --webroot -w /var/www/certbot \
  $EMAIL_ARG --agree-tos --no-eff-email \
  -d "$ARCHLENS_DOMAIN"

$COMPOSE $CF run --rm --entrypoint sh certbot -c "chmod 644 /etc/letsencrypt/archive/${ARCHLENS_DOMAIN}/privkey1.pem"

sed "s/__ARCHLENS_DOMAIN__/${ARCHLENS_DOMAIN}/g" \
  deploy/nginx/conf.d/default-pilot-ssl.conf > deploy/nginx/conf.d/default-pilot-active.conf

$COMPOSE $CF up -d nginx certbot-renew
sleep 5

$COMPOSE $CF up -d --force-recreate backend frontend keycloak
sleep 15

sudo $COMPOSE $CF build frontend 2>&1 | tail -15
sudo $COMPOSE $CF up -d --force-recreate frontend
sleep 10

curl -sf "https://${ARCHLENS_DOMAIN}/health" && echo " TLS OK"
