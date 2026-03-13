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

## Consulta SQL complementaria

El archivo [Respuesta_SQL.txt](Respuesta_SQL.txt) contiene un script SQL Server con:
- Creación de tablas (Cliente, Sucursal, Producto, Inscripcion, Disponibilidad, Visitan)
- Datos de prueba
- Consulta con CTEs para encontrar clientes cuyos productos inscritos están disponibles en **todas** las sucursales que visitan

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
