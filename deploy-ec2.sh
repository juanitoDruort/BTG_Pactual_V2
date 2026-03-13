#!/bin/bash
# ============================================================
# Script de Despliegue - BTG Pactual V2 en AWS EC2
# ============================================================
# Uso: chmod +x deploy-ec2.sh && ./deploy-ec2.sh
# Prerequisitos: Instancia EC2 con Amazon Linux 2 o Ubuntu
# ============================================================

set -euo pipefail

REPO_URL="https://github.com/juanitoDruort/BTG_Pactual_V2.git"
APP_DIR="$HOME/BTG_Pactual_V2"
COMPOSE_FILE="docker-compose.ec2.yml"
HEALTH_URL="http://localhost:8080/swagger-ui.html"
MAX_RETRIES=15
RETRY_INTERVAL=10
DOCKER_JUST_INSTALLED=false

echo "=========================================="
echo " BTG Pactual V2 - Despliegue EC2"
echo "=========================================="

# --- 1. Instalar Docker ---
if ! command -v docker &> /dev/null; then
    echo "[1/5] Instalando Docker..."
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        case "$ID" in
            amzn)
                sudo yum update -y
                sudo yum install -y docker
                sudo systemctl start docker
                sudo systemctl enable docker
                sudo usermod -aG docker "$USER"
                DOCKER_JUST_INSTALLED=true
                ;;
            ubuntu|debian)
                sudo apt-get update -y
                sudo apt-get install -y docker.io
                sudo systemctl start docker
                sudo systemctl enable docker
                sudo usermod -aG docker "$USER"
                DOCKER_JUST_INSTALLED=true
                ;;
            *)
                echo "ERROR: OS no soportado: $ID"
                exit 1
                ;;
        esac
    fi
    echo "Docker instalado."
else
    echo "[1/5] Docker ya instalado."
fi

# --- 2. Instalar Docker Compose ---
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "[2/5] Instalando Docker Compose..."
    COMPOSE_VERSION="v2.29.1"
    sudo curl -L "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    echo "Docker Compose instalado."
else
    echo "[2/5] Docker Compose ya instalado."
fi

# --- 3. Clonar o actualizar repositorio ---
echo "[3/5] Preparando repositorio..."
if [ -d "$APP_DIR" ]; then
    cd "$APP_DIR"
    git pull origin main
else
    git clone "$REPO_URL" "$APP_DIR"
    cd "$APP_DIR"
fi

# --- 4. Verificar .env.ec2 ---
if [ ! -f ".env.ec2" ]; then
    echo ""
    echo "ERROR: Archivo .env.ec2 no encontrado."
    echo "Crea el archivo a partir del template:"
    echo "  cp .env.ec2.template .env.ec2"
    echo "  nano .env.ec2  # completar con valores reales"
    echo ""
    exit 1
fi

# Verificar que no sea el template sin modificar
if grep -q "REEMPLAZAR_CON" .env.ec2; then
    echo ""
    echo "ERROR: .env.ec2 aún contiene valores placeholder."
    echo "Edita el archivo y reemplaza todos los valores REEMPLAZAR_CON_*"
    echo ""
    exit 1
fi

# --- 5. Build y Deploy ---
echo "[4/5] Construyendo imagen y levantando contenedores..."
if [ "$DOCKER_JUST_INSTALLED" = true ]; then
    # Docker recién instalado: requiere sudo hasta re-login
    if sudo docker compose version &> /dev/null; then
        sudo docker compose -f "$COMPOSE_FILE" up -d --build
    else
        sudo docker-compose -f "$COMPOSE_FILE" up -d --build
    fi
else
    if docker compose version &> /dev/null; then
        docker compose -f "$COMPOSE_FILE" up -d --build
    else
        docker-compose -f "$COMPOSE_FILE" up -d --build
    fi
fi

# --- 6. Health Check ---
echo "[5/5] Verificando que la app esté corriendo..."
for i in $(seq 1 $MAX_RETRIES); do
    if curl -sf "$HEALTH_URL" > /dev/null 2>&1; then
        echo ""
        echo "=========================================="
        echo " DESPLIEGUE EXITOSO"
        echo "=========================================="
        PUBLIC_IP=$(curl -sf http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo "<IP_NO_DISPONIBLE>")
        echo " API:     http://${PUBLIC_IP}:8080"
        echo " Swagger: http://${PUBLIC_IP}:8080/swagger-ui.html"
        echo "=========================================="
        exit 0
    fi
    echo "  Intento $i/$MAX_RETRIES - Esperando ${RETRY_INTERVAL}s..."
    sleep "$RETRY_INTERVAL"
done

echo ""
echo "ERROR: La app no respondió después de $((MAX_RETRIES * RETRY_INTERVAL)) segundos."
echo "Revisar logs: docker logs btg-pactual-v2"
exit 1
