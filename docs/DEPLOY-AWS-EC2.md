# Deploy na AWS (EC2 dedicada) — ArchLens AI

Piloto e produção inicial usando **EC2 + Docker Compose + Nginx + Keycloak OIDC**, no mesmo padrão do [Fluxe B2B Suite](../../fluxe-b2b-suite/docs/DEPLOY-AWS-EC2.md) e [ComercialCloud](../../comercial-cloud/docs/DEPLOY-AWS-EC2.md).

**Instância separada** — key pair e security group próprios (`archlens-deploy` / `archlens-prod-sg`).

---

## Arquitetura (um host)

| Rota | Serviço |
|------|---------|
| `/` | Frontend Next.js |
| `/v1/*` | Backend Quarkus |
| `/q/*` | Health, metrics, Swagger |
| `/auth/*` | Keycloak 26 (OIDC + PKCE) |
| `/health` | Alias → `/q/health` |

Serviços internos (não expostos): Postgres+pgvector, RabbitMQ, worker-ai.

---

## Pré-requisitos

| Item | Detalhe |
|------|---------|
| AWS CLI | Configurado (`aws configure`) |
| SSH | Região `sa-east-1` (ou `AWS_REGION`) |
| OpenAI (opcional) | `OPENAI_API_KEY` em `.env.aws-pilot` para LLM/embeddings reais |

---

## Passo a passo (piloto)

### 1. Provisionar EC2

```bash
cd archlens-ai
chmod +x scripts/*.sh
./scripts/aws-provision-archlens-ec2.sh
```

Cria EC2 `t3.large`, 80 GB, Amazon Linux 2023. Metadata em `.aws-deploy/last-ec2.env`.

### 2. Validar DNS piloto (sslip.io)

```bash
source .aws-deploy/last-ec2.env
./scripts/aws-setup-dns-auto.sh
```

### 3. Deploy da stack

```bash
./scripts/aws-deploy-archlens-ec2.sh
```

Gera `.env.aws-pilot`, realm Keycloak, build Docker na EC2 (~20–35 min).

### 4. HTTPS (Let's Encrypt)

```bash
CERTBOT_EMAIL=seu@email.com ./scripts/aws-setup-tls-ec2.sh
```

Atualiza URLs HTTPS, regenera realm, rebuild frontend e recria serviços.

### 5. Validar

```bash
./scripts/aws-pilot-smoke.sh
```

Browser:

- App: `https://<PILOT_DOMAIN>/login` (OIDC PKCE)
- Keycloak admin: `https://<PILOT_DOMAIN>/auth/admin`

**Login demo (realm importado):**

| E-mail | Senha | Perfil |
|--------|-------|--------|
| `admin@archlens.dev` | `admin123` | platform admin + tenant-1 |
| `architect@archlens.dev` | `arch123` | architect, tenant-1 |
| `viewer@archlens.dev` | `view123` | viewer, tenant-2 |

---

## Keycloak

- Imagem `quay.io/keycloak/keycloak:26.0`
- Path `/auth` via Nginx
- Realm `archlens` — clients `archlens-frontend` (PKCE) e `archlens-api` (bearer)
- Claims JWT: `tenant_id`, `realm_roles`

Regenerar realm para outro domínio:

```bash
./scripts/generate-keycloak-realm-prod.sh https://app.seudominio.com.br
```

Template: `infra/keycloak/realm-prod-template.json`.

---

## Variáveis (`.env.aws-pilot`)

| Variável | Exemplo |
|----------|---------|
| `DOMAIN` | `54-94-52-89.sslip.io` |
| `APP_URL` | `https://54-94-52-89.sslip.io` |
| `KEYCLOAK_PUBLIC_URL` | `https://.../auth` |
| `KEYCLOAK_ISSUER_URL` | `https://.../auth/realms/archlens` |
| `POSTGRES_PASSWORD` | gerado no deploy |
| `OPENAI_API_KEY` | opcional — LLM/embeddings reais |

Ver `.env.aws-pilot.example`.

---

## Domínio próprio

1. Route 53 ou Registro.br → A record para Elastic IP da EC2
2. Atualizar `.env.aws-pilot` com domínio real
3. `./scripts/generate-keycloak-realm-prod.sh https://app.seudominio.com.br`
4. `CERTBOT_EMAIL=... ./scripts/aws-setup-tls-ec2.sh`
5. `./scripts/aws-deploy-archlens-ec2.sh` (se mudou branding/build args)

---

## Comandos úteis na EC2

```bash
ssh -i ~/.ssh/archlens-deploy.pem ec2-user@<IP>
cd /opt/archlens/archlens-ai
sudo docker-compose --env-file .env.aws-pilot -f docker-compose.prod.yml ps
sudo docker-compose --env-file .env.aws-pilot -f docker-compose.prod.yml logs -f backend
./scripts/backup-postgres.sh
```

---

## Referências

- [RUNBOOK-PRODUCAO.md](./RUNBOOK-PRODUCAO.md)
- [DEMO-SCRIPT.md](./DEMO-SCRIPT.md)
- Fluxe AWS: `fluxe-b2b-suite/docs/DEPLOY-AWS-EC2.md`
