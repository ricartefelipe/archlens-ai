#!/usr/bin/env bash
# Provisiona EC2 dedicada para ArchLens AI (Docker Compose prod + Keycloak OIDC).
# Padrão Fluxe / ComercialCloud — instância separada.
#
# Uso:
#   ./scripts/aws-provision-archlens-ec2.sh
#   INSTANCE_TYPE=t3.large ./scripts/aws-provision-archlens-ec2.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

AWS_REGION="${AWS_REGION:-sa-east-1}"
INSTANCE_TYPE="${INSTANCE_TYPE:-t3.large}"
KEY_NAME="${KEY_NAME:-archlens-deploy}"
SG_NAME="${SG_NAME:-archlens-prod-sg}"
INSTANCE_NAME="${INSTANCE_NAME:-archlens-prod}"
KEY_FILE="${KEY_FILE:-$HOME/.ssh/${KEY_NAME}.pem}"

MY_IP="$(curl -sf --max-time 5 https://checkip.amazonaws.com 2>/dev/null | tr -d '\n' || true)"
if [[ -z "$MY_IP" ]]; then
  echo "Não foi possível detectar IP público para regra SSH." >&2
  exit 1
fi

echo "▸ Região: $AWS_REGION | Tipo: $INSTANCE_TYPE | SSH de: $MY_IP/32"

VPC_ID="$(aws ec2 describe-vpcs --region "$AWS_REGION" \
  --filters Name=isDefault,Values=true \
  --query 'Vpcs[0].VpcId' --output text)"

if [[ -z "$VPC_ID" || "$VPC_ID" == "None" ]]; then
  echo "VPC default não encontrada em $AWS_REGION" >&2
  exit 1
fi

if ! aws ec2 describe-key-pairs --region "$AWS_REGION" --key-names "$KEY_NAME" &>/dev/null; then
  echo "▸ Criando key pair $KEY_NAME → $KEY_FILE"
  aws ec2 create-key-pair --region "$AWS_REGION" --key-name "$KEY_NAME" \
    --query 'KeyMaterial' --output text > "$KEY_FILE"
  chmod 600 "$KEY_FILE"
else
  echo "▸ Key pair $KEY_NAME já existe"
fi

SG_ID="$(aws ec2 describe-security-groups --region "$AWS_REGION" \
  --filters "Name=group-name,Values=$SG_NAME" "Name=vpc-id,Values=$VPC_ID" \
  --query 'SecurityGroups[0].GroupId' --output text 2>/dev/null || true)"

if [[ -z "$SG_ID" || "$SG_ID" == "None" ]]; then
  echo "▸ Criando security group $SG_NAME"
  SG_ID="$(aws ec2 create-security-group --region "$AWS_REGION" \
    --group-name "$SG_NAME" \
    --description "ArchLens AI prod - HTTP/HTTPS/SSH" \
    --vpc-id "$VPC_ID" \
    --query 'GroupId' --output text)"
  aws ec2 authorize-security-group-ingress --region "$AWS_REGION" --group-id "$SG_ID" \
    --ip-permissions \
    "IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges=[{CidrIp=${MY_IP}/32,Description=SSH}]" \
    "IpProtocol=tcp,FromPort=80,ToPort=80,IpRanges=[{CidrIp=0.0.0.0/0,Description=HTTP}]" \
    "IpProtocol=tcp,FromPort=443,ToPort=443,IpRanges=[{CidrIp=0.0.0.0/0,Description=HTTPS}]"
else
  echo "▸ Security group existente: $SG_ID"
fi

AMI_ID="$(aws ec2 describe-images --region "$AWS_REGION" \
  --owners amazon \
  --filters "Name=name,Values=al2023-ami-2023*" "Name=architecture,Values=x86_64" "Name=state,Values=available" \
  --query 'sort_by(Images, &CreationDate)[-1].ImageId' --output text)"

echo "▸ AMI: $AMI_ID"

USER_DATA="#!/bin/bash
set -euxo pipefail
dnf update -y
dnf install -y docker git
systemctl enable docker
systemctl start docker
usermod -aG docker ec2-user
curl -SL https://github.com/docker/compose/releases/download/v2.27.0/docker-compose-linux-x86_64 -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose
mkdir -p /opt/archlens
chown ec2-user:ec2-user /opt/archlens
echo 'Docker ready' > /opt/archlens/bootstrap.ok
"

echo "▸ Lançando instância..."
INSTANCE_ID="$(aws ec2 run-instances --region "$AWS_REGION" \
  --image-id "$AMI_ID" \
  --instance-type "$INSTANCE_TYPE" \
  --key-name "$KEY_NAME" \
  --security-group-ids "$SG_ID" \
  --user-data "$USER_DATA" \
  --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$INSTANCE_NAME}]" \
  --block-device-mappings '[{"DeviceName":"/dev/xvda","Ebs":{"VolumeSize":80,"VolumeType":"gp3","DeleteOnTermination":true}}]' \
  --query 'Instances[0].InstanceId' --output text)"

echo "▸ Aguardando instância $INSTANCE_ID..."
aws ec2 wait instance-running --region "$AWS_REGION" --instance-ids "$INSTANCE_ID"

PUBLIC_IP="$(aws ec2 describe-instances --region "$AWS_REGION" \
  --instance-ids "$INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)"

mkdir -p "$ROOT/.aws-deploy"
PILOT_DOMAIN="${PUBLIC_IP//./-}.sslip.io"
cat > "$ROOT/.aws-deploy/last-ec2.env" <<EOF
AWS_REGION=$AWS_REGION
INSTANCE_ID=$INSTANCE_ID
PUBLIC_IP=$PUBLIC_IP
PILOT_DOMAIN=$PILOT_DOMAIN
KEY_FILE=$KEY_FILE
EOF

echo ""
echo "✔ EC2 ArchLens pronta"
echo "  InstanceId: $INSTANCE_ID"
echo "  IP público: $PUBLIC_IP"
echo "  Piloto DNS: $PILOT_DOMAIN"
echo "  SSH: ssh -i $KEY_FILE ec2-user@$PUBLIC_IP"
echo ""
echo "Próximos passos:"
echo "  source .aws-deploy/last-ec2.env"
echo "  ./scripts/aws-setup-dns-auto.sh"
echo "  ./scripts/aws-deploy-archlens-ec2.sh"
echo "  CERTBOT_EMAIL=seu@email.com ./scripts/aws-setup-tls-ec2.sh"
