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
| Persistencia (desarrollo) | En memoria (ConcurrentHashMap) |
| Persistencia (producción) | Azure Cosmos DB — API NoSQL |
| Tokens JWT | JJWT 0.12.6 |
| Tests | JUnit 5 + MockMvc + spring-security-test |

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
├── application/        ← Orquestación CQRS + Mediador
│   ├── mediador/       ← Mediador genérico (tipo → handler)
│   ├── registro/       ← Command: registro de usuarios
│   ├── login/          ← Command: autenticación
│   ├── desbloqueo/     ← Command: desbloqueo de cuentas
│   └── fondo/          ← Command + Query: operaciones de fondos
├── infrastructure/     ← Implementaciones de interfaces
│   ├── config/         ← SecurityConfig
│   ├── security/       ← JWT (proveedor, filtro, UserDetailsService)
│   ├── service/        ← ServicioSuscripcion, ServicioFondo
│   └── adapter/out/    ← Adaptadores de persistencia y notificaciones
└── api/                ← Adaptador HTTP de entrada
    ├── controller/     ← ControladorAutenticacion, ControladorFondo, ControladorAdmin
    ├── dto/            ← Records de solicitud/respuesta HTTP
    └── handler/        ← ManejadorExcepcionesGlobal
```

## Prerrequisitos

- **Java 24** (configurado en `build.gradle` via toolchain)
- **Gradle 9.3.1** (incluido via wrapper `gradlew`)

## Compilar y ejecutar

```bash
# Compilar el proyecto
./gradlew build

# Ejecutar la aplicación
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

## Ejecutar tests

```bash
# Ejecutar todos los tests
./gradlew test

# Ver reporte HTML
# build/reports/tests/test/index.html
```

**54 tests** distribuidos en:

- **Tests unitarios**: modelo de dominio (`ClienteTest`), manejadores de aplicación (`RegistroManejadorTest`, `LoginManejadorTest`, `DesbloqueoManejadorTest`), seguridad (`JwtProveedorTest`)
- **Tests E2E (integración)**: `ControladorAutenticacionE2ETest`, `ControladorFondoE2ETest`, `ControladorAdminE2ETest` — levantan contexto Spring completo con MockMvc y seguridad real

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

## Documentación

- [ARQUITECTURA.md](ARQUITECTURA.md) — Arquitectura detallada, estructura de carpetas, flujo de solicitudes
- [docs/architecture/](docs/architecture/) — Estándares de código, flujo de autenticación, seguridad JWT
- [docs/stories/](docs/stories/) — Historias de usuario del proyecto

## Estructura del proyecto

```
BTG_Pactual_V2/
├── build.gradle              ← Configuración de build y dependencias
├── settings.gradle           ← Nombre del proyecto
├── gradlew / gradlew.bat    ← Gradle wrapper
├── ARQUITECTURA.md           ← Documentación de arquitectura
├── Respuesta_SQL.txt         ← Script SQL complementario
├── docs/
│   ├── architecture/         ← Documentación técnica
│   └── stories/              ← Historias de usuario
└── src/
    ├── main/
    │   ├── java/             ← Código fuente
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/             ← Tests unitarios y de integración
```
