# Base Alpine fixada por digest (manifest list, cobre amd64 e arm64) para build
# reproduzível. A variante slim (Debian) carregava 44 vulnerabilidades HIGH de
# pacotes do sistema sem correção publicada pelo upstream; Alpine entrega a
# mesma stack Python com zero achados.
#
# Atualizar com:
#   docker pull python:3.12-alpine && \
#     docker inspect python:3.12-alpine --format '{{index .RepoDigests 0}}'
# Fixar digest não corrige CVE: reconstrua a imagem após atualizar o digest.
FROM python:3.12-alpine@sha256:5789aa194f757888f41fa3d48e14b6feaa1b57c0af0b56fcb1372d6474d3bff0

WORKDIR /app

# psycopg[binary] embute a libpq; nenhum pacote de sistema adicional é necessário.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
