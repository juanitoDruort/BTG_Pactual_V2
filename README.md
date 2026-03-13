# BTG Pactual V2

API REST para gestión de fondos de inversión BTG Pactual. Permite registro y autenticación de usuarios, suscripción/cancelación de fondos, y consulta de suscripciones vigentes, con seguridad basada en JWT y control de acceso por roles.

## Stack Tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 24 |
| Framework | Spring Boot 4.0.3 |
| Build | Gradle 9.3.1 (Groovy DSL) |
| Documentación API | SpringDoc OpenAPI 3.0.2 |
| Seguridad | Spring Security + JWT (HMAC-SHA256, stateless) |
| Persistencia | AWS DynamoDB Local (Docker) — SDK v2 Enhanced Client |
| Tokens JWT | JJWT 0.12.6 |
| Tests | JUnit 5 + MockMvc + Testcontainers 1.20.6 + spring-security-test |
| Contenedores | Docker (DynamoDB Local) |

## Arquitectura

**Arquitectura Hexagonal (Ports & Adapters)** combinada con **CQRS** y patrón **Mediador**.

- El **dominio** no depende de ninguna capa externa
- La **capa de aplicación** orquesta mediante CQRS (Commands/Queries) sin conocer infraestructura
- La **API** solo conoce el Mediador y los objetos de comando/consulta
- La **seguridad** se resuelve como filtro transversal antes de llegar al controlador
- Los **puertos de salida** abstraen repositorios y servicios externos (notificaciones)

```
src/main/java/btg_pactual_v1/btg_pactual_v2/
├── domain/             ← Núcleo puro, sin dependencias externas
│   ├── exception/      ← Excepciones de dominio
│   ├── model/          ← Entidades: Cliente, Fondo, Suscripcion, Rol
│   ├── port/out/       ← Puertos de salida (interfaces)
│   └── service/        ← Contratos de servicios de dominio
├── application/        ← Orquestación CQRS + Mediador + DTOs
│   ├── mediador/       ← Mediador genérico (tipo → handler)
│   ├── registro/       ← Command: registro de usuarios
│   ├── login/          ← Command: autenticación
│   ├── desbloqueo/     ← Command: desbloqueo de cuentas
│   ├── cancelacion/    ← Command: cancelación de suscripciones
│   ├── fondo/          ← Command + Query: operaciones de fondos
│   └── suscripcion/    ← Query: suscripciones vigentes
├── infrastructure/     ← Implementaciones de interfaces
│   ├── config/         ← SecurityConfig, ConfiguracionDynamoDb, InicializadorTablasDynamoDb
│   ├── security/       ← JWT (proveedor, filtro, UserDetailsService)
│   ├── service/        ← ServicioSuscripcion, ServicioFondo
│   └── adapter/out/    ← Adaptadores DynamoDB y notificaciones
└── api/                ← Adaptador HTTP de entrada (sin DTOs propios)
    ├── controller/     ← ControladorAutenticacion, ControladorFondo, ControladorSuscripcion, ControladorAdmin
    └── handler/        ← ManejadorExcepcionesGlobal
```

## Prerrequisitos

- **Java 24** (configurado en `build.gradle` via toolchain)
- **Gradle 9.3.1** (incluido via wrapper `gradlew`)
- **Docker** (para DynamoDB Local)

## Levantar DynamoDB Local con Docker

La aplicación requiere una instancia de DynamoDB Local corriendo en el puerto 8000.

### Paso a paso

**1. Verificar que Docker esté corriendo:**

```bash
docker info
```

**2. Levantar DynamoDB Local con docker-compose:**

```bash
docker-compose up -d
```

Esto usa el archivo `docker-compose.yml` del proyecto que ejecuta:
- Imagen: `amazon/dynamodb-local:latest`
- Puerto: `8000` (mapeado a `localhost:8000`)
- Modo: `-sharedDb` (base de datos compartida)

**3. Verificar que DynamoDB Local esté corriendo:**

```bash
# Linux/Mac
curl -s http://localhost:8000 -X POST \
  -H "X-Amz-Target: DynamoDB_20120810.ListTables" \
  -H "Content-Type: application/x-amz-json-1.0" \
  -d '{"Limit": 10}'

# Windows PowerShell
Invoke-RestMethod -Uri "http://localhost:8000" -Method POST `
  -Headers @{"X-Amz-Target"="DynamoDB_20120810.ListTables"; "Content-Type"="application/x-amz-json-1.0"} `
  -Body '{"Limit": 10}'
```

Debe retornar `{"TableNames":[]}` si es primera vez, o las tablas existentes.

**4. (Opcional) Detener DynamoDB Local:**

```bash
docker-compose down
```

> **Nota:** Al iniciar la aplicación Spring Boot, las tablas (`clientes`, `fondos`, `suscripciones`) se crean automáticamente junto con datos semilla (2 clientes de prueba y 5 fondos). No se requiere configuración adicional.

### Configuración de conexión

Ya configurada en `application.properties`:

```properties
dynamodb.endpoint=http://localhost:8000
dynamodb.region=us-east-1
dynamodb.access-key=fakeMyKeyId
dynamodb.secret-key=fakeSecretAccessKey
```

Estas credenciales son ficticias — DynamoDB Local no valida autenticación real.

## Compilar y ejecutar

```bash
# 1. Levantar DynamoDB Local (si no está corriendo)
docker-compose up -d

# 2. Compilar el proyecto
./gradlew build

# 3. Ejecutar la aplicación
./gradlew bootRun

# Solo compilar sin ejecutar tests
./gradlew build -x test
```

En Windows usar `gradlew.bat` en lugar de `./gradlew`.

La aplicación se levanta en `http://localhost:8080`.

## API Endpoints

### Públicos (sin autenticación)

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/auth/registro` | Registro de nuevo cliente |
| `POST` | `/api/auth/login` | Autenticación, retorna JWT |

### Protegidos (requieren `Authorization: Bearer {token}`)

| Método | Endpoint | Descripción | Rol requerido |
|---|---|---|---|
| `POST` | `/api/fondos/suscribir` | Suscribir cliente a un fondo | CLIENTE / ADMINISTRADOR |
| `GET` | `/api/fondos/{fondoId}` | Consultar fondo por ID | Cualquier autenticado |
| `POST` | `/api/suscripciones/{id}/cancelar` | Cancelar suscripción | CLIENTE / ADMINISTRADOR |
| `GET` | `/api/suscripciones/vigentes` | Listar suscripciones activas del cliente | CLIENTE / ADMINISTRADOR |
| `PUT` | `/api/admin/usuarios/{userId}/desbloquear` | Desbloquear cuenta bloqueada | ADMINISTRADOR |

### Documentación interactiva (Swagger UI)

Disponible en: `http://localhost:8080/swagger-ui.html`

API Docs JSON: `http://localhost:8080/api-docs`

## Seguridad

### Autenticación JWT

- **Algoritmo**: HMAC-SHA256
- **Duración del token**: 5 minutos (300.000 ms)
- **Sesiones**: Stateless (sin estado en servidor)
- **Filtro**: `JwtFiltroAutenticacion` valida el token en cada request protegido

### Roles y permisos (RBAC)

| Rol | Permisos |
|---|---|
| **CLIENTE** | Suscribir, cancelar, listar sus suscripciones, consultar fondos. Solo accede a sus propios datos (prevención BOLA). |
| **ADMINISTRADOR** | Todas las operaciones + desbloqueo de cuentas + acceso a datos de cualquier cliente. |

### Política de bloqueo

- **3 intentos fallidos** consecutivos de login → cuenta bloqueada automáticamente
- Solo un **ADMINISTRADOR** puede desbloquear la cuenta
- Intentos posteriores al bloqueo retornan HTTP 403

## Fondos disponibles (datos iniciales)

| ID | Nombre | Monto mínimo | Categoría |
|---|---|---|---|
| fondo-1 | FPV_BTG_PACTUAL_RECAUDADORA | $75.000 | FPV |
| fondo-2 | FPV_BTG_PACTUAL_ECOPETROL | $125.000 | FPV |
| fondo-3 | DEUDAPRIVADA | $50.000 | FIC |
| fondo-4 | FDO-ACCIONES | $250.000 | FIC |
| fondo-5 | FPV_BTG_PACTUAL_DINAMICA | $100.000 | FPV |

## Clientes semilla (datos iniciales)

| ID | Nombre | Email | Saldo |
|---|---|---|---|
| cliente-1 | Juan Rodriguez | juan.rodriguez@email.com | $500.000 (encriptado AES-256) |
| cliente-2 | Maria Lopez | maria.lopez@email.com | $1.000.000 (encriptado AES-256) |

> Los datos semilla se cargan automáticamente al iniciar la aplicación via `InicializadorTablasDynamoDb`. Los saldos se persisten encriptados en DynamoDB.

## Ejecutar tests

```bash
# Ejecutar todos los tests
./gradlew test

# Ver reporte HTML
# build/reports/tests/test/index.html
```

**107 tests** distribuidos en:

- **Tests unitarios de dominio**: `ClienteTest` (7), `SuscripcionTest` (2)
- **Tests unitarios de aplicación**: `RegistroManejadorTest` (4), `LoginManejadorTest` (5), `DesbloqueoManejadorTest` (2), `CancelacionManejadorTest` (2), `SuscripcionesVigentesManejadorTest` (4)
- **Tests unitarios de infraestructura**: `JwtProveedorTest` (5), `AdaptadorEncriptacionAesTest` (7), `ServicioSuscripcionTest` (6), `AdaptadorNotificacionConsolaTest` (2)
- **Tests de integración DynamoDB** (Testcontainers): `AdaptadorClienteDynamoDbTest` (6), `AdaptadorFondoDynamoDbTest` (2), `AdaptadorSuscripcionDynamoDbTest` (6)
- **Tests E2E** (MockMvc + Testcontainers): `ControladorAutenticacionE2ETest` (12), `ControladorFondoE2ETest` (16), `ControladorAdminE2ETest` (3), `ControladorSuscripcionE2ETest` (15)
- **Context load**: `BtgPactualV2ApplicationTests` (1)

> Los tests E2E y de integración usan **Testcontainers** con DynamoDB Local. Docker debe estar corriendo para ejecutar los tests.

## Ejemplo de uso

### 1. Registrar un cliente

```bash
curl -X POST http://localhost:8080/api/auth/registro \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Juan Rodriguez",
    "email": "juan@email.com",
    "telefono": "3001234567",
    "documentoIdentidad": "CC123456789",
    "contrasena": "MiPass1!",
    "saldoInicial": 500000
  }'
```

### 2. Iniciar sesión

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@email.com",
    "contrasena": "MiPass1!"
  }'
# Respuesta: { "token": "eyJ..." }
```

### 3. Suscribirse a un fondo

```bash
curl -X POST http://localhost:8080/api/fondos/suscribir \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJ..." \
  -d '{
    "clienteId": "<clienteId>",
    "fondoId": "fondo-1",
    "monto": 75000
  }'
```

### 4. Cancelar una suscripción

```bash
curl -X POST http://localhost:8080/api/suscripciones/<suscripcionId>/cancelar \
  -H "Authorization: Bearer eyJ..."
# Respuesta: { "suscripcionId": "...", "estado": "CANCELADO", "monto": 75000, ... }
# El monto se devuelve automáticamente al saldo del cliente
```

### 5. Consultar fondos disponibles

```bash
curl http://localhost:8080/api/fondos/fondo-1 \
  -H "Authorization: Bearer eyJ..."
```

## Reglas de negocio principales

| Regla | Detalle |
|---|---|
| Saldo mínimo inicial | $500.000 al registrarse |
| Monto mínimo por fondo | Cada fondo define su monto mínimo de suscripción |
| Sin duplicados | Un cliente no puede tener dos suscripciones ACTIVAS al mismo fondo |
| Re-suscripción | Permitida tras cancelar la suscripción anterior |
| Devolución al cancelar | El monto se abona automáticamente al saldo del cliente |
| Aislamiento de datos | Un cliente solo puede operar sobre sus propios datos (BOLA prevention) |
| Contraseña segura | Mínimo 5 caracteres, debe incluir números, letras y caracteres especiales |
| Bloqueo automático | 3 intentos fallidos de login → cuenta bloqueada |
| Encriptación en reposo | Saldos encriptados con AES-256-GCM; contraseñas hasheadas con BCrypt |

## Documentación

- [ARQUITECTURA.md](ARQUITECTURA.md) — Arquitectura detallada, estructura de carpetas, flujo de solicitudes, reglas de negocio completas
- [docs/architecture/](docs/architecture/) — Estándares de código, flujo de autenticación, seguridad JWT
- [docs/stories/](docs/stories/) — Historias de usuario del proyecto (9 HUs definidas)

## Historias de usuario

| Épica | HU | Descripción | Estado |
|---|---|---|---|
| **Seguridad** | 1.1 | Registro de usuarios con modelo de credenciales | ✅ Implementada |
| | 1.2 | Autenticación JWT con política de bloqueo | ✅ Implementada |
| | 1.3 | Autorización por roles y aislamiento de datos | ✅ Implementada |
| | 1.4 | Encriptación de datos sensibles en reposo (AES-256) | ✅ Implementada |
| **Fondos** | 2.1 | Suscripción a fondo de inversión | ✅ Implementada |
| | 2.2 | Cancelación de suscripción a fondo | ✅ Implementada |
| | 2.3 | Consulta de suscripciones vigentes | ✅ Implementada |
| | 2.4 | Notificación email/SMS al suscribir | ✅ Implementada |
| **Datos** | 3 | Modelo de datos DynamoDB (tablas + adaptadores + semilla) | ✅ Implementada |
| **Infraestructura** | 4 | Despliegue AWS EC2 con Docker | ✅ Implementada |

## Consulta SQL complementaria

El archivo [Respuesta_SQL.txt](Respuesta_SQL.txt) contiene un script SQL Server con:
- Creación de tablas (Cliente, Sucursal, Producto, Inscripcion, Disponibilidad, Visitan)
- Datos de prueba
- Consulta con CTEs para encontrar clientes cuyos productos inscritos están disponibles en **todas** las sucursales que visitan

## Despliegue en AWS EC2 con Docker

### Arquitectura de Despliegue

```
┌─────────────────────────────────────────────────────────────────┐
│                        AWS Cloud                                │
│                                                                 │
│  ┌──────────────────────────────────────┐                       │
│  │        EC2 Instance                  │                       │
│  │  ┌────────────────────────────────┐  │                       │
│  │  │   Docker Container             │  │                       │
│  │  │  ┌──────────────────────────┐  │  │                       │
│  │  │  │  Spring Boot App         │  │  │   ┌───────────────┐  │
│  │  │  │  (JRE 24 runtime)       │──┼──┼──▶│ AWS DynamoDB  │  │
│  │  │  │  Puerto 8080            │  │  │   │ (us-east-1)   │  │
│  │  │  └──────────────────────────┘  │  │   └───────────────┘  │
│  │  └────────────────────────────────┘  │                       │
│  │                                      │                       │
│  │  Security Group:                     │                       │
│  │  - TCP 8080 (HTTP) ← Internet       │                       │
│  │  - TCP 22 (SSH) ← Admin IP          │                       │
│  └──────────────────────────────────────┘                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
          ▲
          │ HTTP :8080
          │
    ┌─────┴─────┐
    │  Cliente   │
    │  externo   │
    └───────────┘
```

**Diferencia con entorno local:** En local se usa DynamoDB Local (Docker, puerto 8000) con credenciales ficticias. En EC2 la app se conecta al servicio AWS DynamoDB real (endpoint regional) con credenciales IAM, y no se levanta contenedor de base de datos.

---

### Prerrequisitos

#### 1. Instancia EC2

- **Tipo recomendado**: `t2.micro` (Free Tier) o `t3.small`
- **SO**: Amazon Linux 2 o Ubuntu 20.04+
- **Almacenamiento**: Mínimo 8 GB (para Docker + imagen)
- **Key Pair**: Creado y `.pem` descargado para acceso SSH
- **IP pública**: Asignada (Elastic IP recomendada para persistencia)

#### 2. Credenciales AWS IAM

Usuario IAM con **permisos mínimos requeridos** para DynamoDB:

```
dynamodb:CreateTable
dynamodb:DescribeTable
dynamodb:PutItem
dynamodb:GetItem
dynamodb:Query
dynamodb:Scan
dynamodb:DeleteItem
dynamodb:UpdateItem
```

> Generar Access Key / Secret Key desde la consola AWS IAM.

#### 3. Security Group

Crear un Security Group en la VPC de la instancia EC2 con estas reglas:

| Dirección | Tipo | Protocolo | Puerto | Origen | Descripción |
|-----------|------|-----------|--------|--------|-------------|
| **Inbound** | Custom TCP | TCP | 8080 | `0.0.0.0/0` | API REST — acceso público |
| **Inbound** | SSH | TCP | 22 | `Mi IP/32` | SSH — restringir a tu IP |
| **Outbound** | All traffic | All | All | `0.0.0.0/0` | Conexión a DynamoDB, Docker Hub |

> **Importante**: Restringir SSH (puerto 22) únicamente a tu IP pública para mayor seguridad.

---

### Archivos de Despliegue

| Archivo | Descripción |
|---------|-------------|
| `Dockerfile` | Build multi-stage: JDK 24 compila → JRE 24 ejecuta. Imagen final sin código fuente |
| `docker-compose.ec2.yml` | Compose para EC2: solo servicio `app` (sin DynamoDB Local) |
| `application-ec2.properties` | Perfil Spring que interpola variables de entorno `${VARIABLE}` |
| `.env.ec2.template` | Template con placeholders — commitear al repo |
| `.env.ec2` | Variables reales — **NUNCA commitear** (excluido en `.gitignore`) |
| `deploy-ec2.sh` | Script automatizado: instala Docker, clona repo, construye y levanta |

---

### Dockerfile Multi-Stage

El `Dockerfile` usa un build en dos etapas para minimizar el tamaño de la imagen final:

```
Etapa 1 (build):     eclipse-temurin:24-jdk
  └── Copia Gradle wrapper + build.gradle
  └── Descarga dependencias (capa cacheada)
  └── Copia src/ y ejecuta: gradlew bootJar

Etapa 2 (runtime):   eclipse-temurin:24-jre
  └── Instala curl (para healthcheck)
  └── Crea usuario no-root (appuser)
  └── Copia solo el JAR desde etapa 1
  └── Ejecuta: java -XX:MaxRAMPercentage=75.0 -jar app.jar
```

**Características de seguridad:**
- Se ejecuta como usuario no-root (`appuser`)
- La imagen final no contiene código fuente, Gradle ni JDK
- `MaxRAMPercentage=75.0` limita el heap al 75% de la RAM del contenedor

---

### Variables de Entorno

Todas las variables sensibles se externalizan mediante el archivo `.env.ec2`. La app las consume a través del perfil Spring `ec2` (`application-ec2.properties`) que interpola `${VARIABLE}`.

#### Referencia completa de variables

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Perfil Spring Boot activo | `ec2` (no modificar) |
| `JWT_SECRET` | Secreto para firmar tokens JWT (HMAC-SHA256) | Generar con `openssl rand -base64 48` |
| `JWT_EXPIRATION_MS` | Duración del token en milisegundos | `300000` (5 min) |
| `JWT_MAX_FAILED_ATTEMPTS` | Intentos fallidos antes de bloqueo | `3` |
| `AES_KEY` | Clave AES-256 en Base64 (32 bytes) | Generar con `openssl rand -base64 32` |
| `DYNAMODB_ENDPOINT` | Endpoint regional de DynamoDB | `https://dynamodb.us-east-1.amazonaws.com` |
| `DYNAMODB_REGION` | Región AWS | `us-east-1` |
| `DYNAMODB_ACCESS_KEY` | AWS Access Key ID (IAM) | `AKIA...` |
| `DYNAMODB_SECRET_KEY` | AWS Secret Access Key (IAM) | `wJalr...` |

#### Cómo funciona la externalización

```
.env.ec2                          application-ec2.properties          Clase Java
─────────                         ──────────────────────────          ──────────
JWT_SECRET=abc123        →        jwt.secret=${JWT_SECRET}     →     @Value("${jwt.secret}")
AES_KEY=base64key        →        encriptacion.clave-aes=${AES_KEY} → @Value("${encriptacion.clave-aes}")
DYNAMODB_ENDPOINT=https://...  →  dynamodb.endpoint=${DYNAMODB_ENDPOINT} → @Value("${dynamodb.endpoint}")
```

**Ningún archivo Java requiere cambios** — Spring Boot resuelve la cadena automáticamente.

---

### Despliegue Paso a Paso

#### Opción A: Despliegue Automático (Recomendado)

El script `deploy-ec2.sh` automatiza todo el proceso:

```bash
# 1. Conectarse a la instancia EC2
ssh -i mi-key.pem ec2-user@<IP_PUBLICA>

# 2. Instalar Git (si no está disponible)
# Amazon Linux 2:
sudo yum install -y git
# Ubuntu:
sudo apt-get install -y git

# 3. Clonar el repositorio
git clone https://github.com/juanitoDruort/BTG_Pactual_V2.git
cd BTG_Pactual_V2

# 4. Crear y configurar variables de entorno
cp .env.ec2.template .env.ec2
nano .env.ec2
# → Reemplazar TODOS los valores REEMPLAZAR_CON_* con valores reales

# 5. Ejecutar el script de despliegue
chmod +x deploy-ec2.sh
./deploy-ec2.sh
```

**El script realiza automáticamente:**

1. **Instala Docker** — Detecta el SO (Amazon Linux 2 / Ubuntu) e instala Docker + habilita servicio
2. **Instala Docker Compose** — Descarga el binario v2.29.1 si no existe
3. **Clona/actualiza el repositorio** — Si ya existe hace `git pull`
4. **Valida `.env.ec2`** — Verifica que exista y no contenga placeholders sin reemplazar
5. **Construye imagen Docker** — Ejecuta el build multi-stage
6. **Levanta contenedores** — `docker-compose -f docker-compose.ec2.yml up -d --build`
7. **Health check** — Reintenta hasta 15 veces (cada 10s) verificando que la app responda
8. **Muestra resultado** — IP pública, URL de la API y Swagger UI

#### Opción B: Despliegue Manual

```bash
# 1. Conectarse a EC2
ssh -i mi-key.pem ec2-user@<IP_PUBLICA>

# 2. Instalar Docker (Amazon Linux 2)
sudo yum update -y
sudo yum install -y docker git
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER
# Cerrar sesión y reconectar para aplicar grupo docker
exit
ssh -i mi-key.pem ec2-user@<IP_PUBLICA>

# 3. Instalar Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.29.1/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 4. Clonar repositorio y configurar
git clone https://github.com/juanitoDruort/BTG_Pactual_V2.git
cd BTG_Pactual_V2
cp .env.ec2.template .env.ec2
nano .env.ec2  # completar con valores reales

# 5. Construir y levantar
docker-compose -f docker-compose.ec2.yml up -d --build

# 6. Verificar logs
docker logs -f btg-pactual-v2

# 7. Verificar que la app responda
curl http://localhost:8080/swagger-ui.html
```

---

### Demo en Vivo (EC2)

> **⚠️ La IP de la instancia EC2 es dinámica** — puede cambiar si la instancia se detiene y reinicia. La URL actual es válida mientras la instancia esté corriendo.

**Swagger UI (pruebas directas desde el navegador):**

http://3.144.178.143:8080/swagger-ui/index.html

Desde Swagger UI se pueden probar todos los endpoints interactivamente: registro, login, suscripción, cancelación y consultas.

**Endpoints de ejemplo:**

| Endpoint | URL directa |
|----------|-------------|
| Swagger UI | http://3.144.178.143:8080/swagger-ui/index.html |
| Registro | http://3.144.178.143:8080/api/auth/registro |
| Login | http://3.144.178.143:8080/api/auth/login |
| API Docs (JSON) | http://3.144.178.143:8080/api-docs |

---

### Verificación del Despliegue

Una vez levantado, verificar desde la máquina local (reemplazar `<IP_PUBLICA_EC2>` con la IP actual):

```bash
# Health check básico
curl http://<IP_PUBLICA_EC2>:8080/swagger-ui/index.html

# Verificar API - Registro de usuario
curl -X POST http://<IP_PUBLICA_EC2>:8080/api/auth/registro \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Test EC2",
    "email": "test@ec2.com",
    "telefono": "3001234567",
    "documentoIdentidad": "CC987654321",
    "contrasena": "Test1!",
    "saldoInicial": 500000
  }'

# Verificar API - Login
curl -X POST http://<IP_PUBLICA_EC2>:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@ec2.com", "contrasena": "Test1!"}'
```

**Swagger UI** disponible en: `http://<IP_PUBLICA_EC2>:8080/swagger-ui/index.html`

---

### Comandos Útiles en EC2

```bash
# Ver estado del contenedor
docker ps

# Ver logs en tiempo real
docker logs -f btg-pactual-v2

# Reiniciar la aplicación
docker-compose -f docker-compose.ec2.yml restart

# Reconstruir y redesplegar (tras git pull)
docker-compose -f docker-compose.ec2.yml up -d --build

# Detener la aplicación
docker-compose -f docker-compose.ec2.yml down

# Ver uso de recursos
docker stats btg-pactual-v2

# Inspeccionar el healthcheck
docker inspect --format='{{json .State.Health}}' btg-pactual-v2
```

---

### Troubleshooting

| Problema | Causa probable | Solución |
|----------|----------------|----------|
| `Connection refused` al acceder :8080 | Security Group no permite TCP 8080 | Agregar regla Inbound TCP 8080 desde `0.0.0.0/0` |
| App no arranca — error DynamoDB | Credenciales AWS inválidas o sin permisos | Verificar `DYNAMODB_ACCESS_KEY` y `DYNAMODB_SECRET_KEY` en `.env.ec2`. Validar permisos IAM |
| `docker: command not found` | Docker no instalado | Ejecutar `deploy-ec2.sh` o instalar Docker manualmente |
| `.env.ec2 contiene placeholders` | No se editó el template | `nano .env.ec2` y reemplazar valores `REEMPLAZAR_CON_*` |
| `Container exits immediately` | Error en configuración | `docker logs btg-pactual-v2` para ver stack trace |
| Build falla por memoria | Instancia con poca RAM | Usar `t3.small` (2 GB) o agregar swap: `sudo fallocate -l 2G /swapfile` |
| Tablas DynamoDB no se crean | Permisos `CreateTable` faltantes | Agregar `dynamodb:CreateTable` y `dynamodb:DescribeTable` al usuario IAM |

---

### Notas de Seguridad

- **Secretos**: El archivo `.env.ec2` con valores reales **nunca** se commitea al repositorio (excluido en `.gitignore`)
- **Solo el template** `.env.ec2.template` (con placeholders) está en el repositorio
- **El contenedor** se ejecuta como usuario no-root (`appuser`)
- **No se usa HTTPS** — este despliegue es para demostración/prueba técnica. En producción se requeriría un load balancer (ALB) con certificado SSL o un reverse proxy (nginx) con Let's Encrypt
- **Credenciales IAM**: En producción se recomienda usar IAM Roles asociados a la instancia EC2 en lugar de access key/secret key

## Estructura del proyecto

```
BTG_Pactual_V2/
├── build.gradle              ← Configuración de build y dependencias
├── settings.gradle           ← Nombre del proyecto
├── docker-compose.yml        ← DynamoDB Local
├── gradlew / gradlew.bat    ← Gradle wrapper
├── ARQUITECTURA.md           ← Documentación de arquitectura
├── Respuesta_SQL.txt         ← Script SQL complementario
├── docs/
│   ├── architecture/         ← Documentación técnica
│   └── stories/              ← Historias de usuario
└── src/
    ├── main/
    │   ├── java/             ← Código fuente (hexagonal + CQRS)
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/             ← 107 tests (unitarios + integración + E2E)
```
