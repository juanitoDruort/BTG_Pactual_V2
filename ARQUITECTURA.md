# Arquitectura BTG Pactual V2

## Stack Tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 24 |
| Framework | Spring Boot 4.0.3 |
| Build | Gradle 9.3.1 |
| Documentación API | SpringDoc OpenAPI 3.0.2 |
| Seguridad | Spring Security + JWT (stateless) |
| Persistencia | Amazon DynamoDB (AWS SDK v2 + Enhanced Client, DynamoDB Local en Docker) |
| Notificaciones | Puerto de salida fire-and-forget (email/SMS) |
| Tests E2E | JUnit 5 + MockMvc + Testcontainers (DynamoDB Local) |
| Tests Integración | JUnit 5 + Testcontainers (DynamoDB Local) |
| Tests Unitarios| JUnit 5 + Mockito |

---

## Patrón Arquitectónico

**Arquitectura Hexagonal (Ports & Adapters)** combinada con **CQRS** y **Mediador**.

### Principios aplicados
- El **dominio** no depende de ninguna capa externa
- Toda **implementación de interfaces** vive en infraestructura
- La **capa de aplicación** orquesta mediante CQRS sin conocer el modelo de dominio
- La **API** solo conoce el Mediador y los objetos de comando/consulta
- La **seguridad** (autenticación/autorización) se resuelve como filtro transversal antes de llegar al controlador
- Los **puertos de salida** abstraen tanto repositorios como servicios externos (notificaciones)

---

## Estructura de Carpetas

```
src/main/java/btg_pactual_v1/btg_pactual_v2/
│
├── BtgPactualV2Application.java
│
├── domain/                                     ← Núcleo puro, sin dependencias externas
│   ├── exception/
│   │   └── ExcepcionDominio.java
│   ├── model/
│   │   ├── Cliente.java                        ← Extendido: +email, telefono, documentoIdentidad,
│   │   │                                          contraseña, rol, bloqueado, intentosFallidos,
│   │   │                                          preferenciaNotificacion (ver HU 1.1)
│   │   ├── Fondo.java
│   │   └── Suscripcion.java
│   ├── port/
│   │   └── out/
│   │       ├── PuertoRepositorioCliente.java   ← +buscarPorEmail(), existePorEmail()
│   │       ├── PuertoRepositorioFondo.java
│   │       ├── PuertoRepositorioSuscripcion.java ← +buscarPorId(), buscarPorClienteIdYEstado()
│   │       └── PuertoNotificacion.java         ← Nuevo: enviar notificación email/SMS (HU 2.4)
│   └── service/
│       ├── IServicioFondo.java
│       └── IServicioSuscripcion.java           ← +cancelar()
│
├── application/                                ← Orquestación CQRS + Mediador + DTOs
│   ├── mediador/
│   │   ├── Manejador.java                      ← Interfaz genérica T→R
│   │   └── Mediador.java                       ← Resuelve handler por tipo
│   └── fondo/                                  ← Agrupado por entidad
│       ├── command/
│       │   ├── FondoComando.java               ← Input del command (DTO de entrada)
│       │   ├── FondoResultado.java             ← Output del command (DTO de salida)
│       │   └── FondoManejador.java             ← Command handler
│       └── query/
│           ├── FondoConsulta.java              ← Input del query (DTO de entrada)
│           ├── FondoResultado.java             ← Output del query (DTO de salida)
│           └── FondoManejador.java             ← Query handler
│
├── infrastructure/                             ← Toda implementación de interfaces
│   ├── config/
│   │   ├── SecurityConfig.java                 ← Configuración Spring Security + JWT
│   │   ├── ConfiguracionDynamoDb.java          ← Beans DynamoDbClient + DynamoDbEnhancedClient
│   │   ├── EsquemaDynamoDb.java                ← Esquema de 3 tablas centralizado
│   │   └── InicializadorTablasDynamoDb.java    ← Creación de tablas + datos semilla
│   ├── security/
│   │   ├── JwtFiltroAutenticacion.java         ← Filtro que valida Bearer token en cada request
│   │   ├── JwtProveedor.java                   ← Genera y valida tokens JWT
│   │   └── DetallesUsuarioServicio.java        ← Carga usuario por email para Spring Security
│   ├── service/
│   │   ├── ServicioSuscripcion.java            ← implements IServicioSuscripcion (+cancelar)
│   │   └── ServicioFondo.java                  ← implements IServicioFondo
│   └── adapter/
│       └── out/
│           ├── persistence/
│           │   ├── AdaptadorClienteDynamoDb.java     ← DynamoDB (HU 3)
│           │   ├── AdaptadorFondoDynamoDb.java       ← DynamoDB (HU 3)
│           │   ├── AdaptadorSuscripcionDynamoDb.java ← DynamoDB (HU 3)
│           │   └── dynamodb/                         ← Modelos de mapeo DynamoDB
│           │       ├── ClienteDynamoDb.java
│           │       ├── FondoDynamoDb.java
│           │       └── SuscripcionDynamoDb.java
│           └── notification/
│               └── AdaptadorNotificacion.java  ← Nuevo: implements PuertoNotificacion (HU 2.4)
│
└── api/                                        ← Adaptador HTTP de entrada (sin DTOs propios)
    ├── controller/
    │   ├── ControladorFondo.java               ← Suscribir fondo y consultar fondo por ID
    │   ├── ControladorSuscripcion.java         ← Cancelar suscripción y listar vigentes
    │   ├── ControladorAutenticacion.java       ← Registro + login (endpoints públicos)
    │   └── ControladorAdmin.java               ← Desbloqueo de cuentas (ADMINISTRADOR)
    └── handler/
        └── ManejadorExcepcionesGlobal.java     ← Extendido: +401, +403, +409

    ⚠️ NOTA: Los DTOs de entrada y salida (Comandos, Consultas, Resultados) viven en
    la capa de aplicación, NO en api/dto/. Los controladores reciben directamente los
    objetos de comando/consulta de aplicación como @RequestBody y retornan los
    Resultados de aplicación. Esto permite que los manejadores CQRS tomen decisiones
    directamente sobre los DTOs sin depender de la capa API.
```

---

## Capa de Seguridad (HU 1.1, 1.2, 1.3, 1.4)

### Cadena de filtros

Toda petición HTTP pasa por la cadena de seguridad **antes** de llegar al controlador:

```
HTTP Request
    └─► SecurityFilterChain
            ├─► ¿Es /api/auth/registro o /api/auth/login?
            │     SÍ → Permitir sin token (endpoints públicos)
            │
            └─► NO → JwtFiltroAutenticacion
                    ├─► Extrae token de cabecera Authorization: Bearer {token}
                    ├─► Valida firma, expiración (5 min default)
                    ├─► Extrae claims: userId, rol
                    ├─► Establece SecurityContext con usuario autenticado
                    └─► Continúa al controlador (o 401 si token inválido/expirado)
```

### Endpoints públicos vs protegidos

| Tipo | Endpoints | Autenticación |
|---|---|---|
| **Público** | `POST /api/auth/registro`, `POST /api/auth/login` | Sin token |
| **Protegido** | Todos los demás (`/api/fondos/*`, `/api/suscripciones/*`) | Bearer JWT obligatorio |

### Roles y permisos

| Rol | Operaciones permitidas | Aislamiento de datos |
|---|---|---|
| **CLIENTE** | Suscribir, cancelar, listar sus suscripciones, consultar fondos | Solo accede a sus propios datos (userId del token) |
| **ADMINISTRADOR** | Todas las operaciones + gestión de usuarios + desbloqueo de cuentas | Accede a datos de cualquier cliente |

### Política de bloqueo

- Tras **3 intentos fallidos** consecutivos de login → cuenta bloqueada automáticamente
- Solo un **ADMINISTRADOR** puede desbloquear una cuenta
- Intentos posteriores al bloqueo retornan HTTP 403 sin incrementar contador

### Encriptación de datos sensibles (HU 1.4)

| Dato | Estrategia | Algoritmo |
|---|---|---|
| Contraseñas | Hashing unidireccional | BCrypt o Argon2 |
| Saldos | Encriptación reversible | AES-256 |

- Puerto de encriptación definido en **dominio** (`PuertoEncriptacion`)
- Implementación de algoritmo en **infraestructura**
- Dominio permanece libre de dependencias de librerías de encriptación

---

## Flujo de una Solicitud

### POST `/api/auth/registro` (Command — Público, HU 1.1)

```
HTTP Request (sin token)
    └─► SecurityFilterChain → permitido (endpoint público)
        └─► ControladorAutenticacion
                │  crea RegistroComando(nombre, email, telefono, documentoIdentidad, contraseña)
                └─► Mediador.enviar(RegistroComando)
                        └─► RegistroManejador.manejar(RegistroComando)
                                ├─► PuertoRepositorioCliente.existePorEmail() → valida unicidad
                                ├─► Valida política de contraseña (min 5 chars, nums+letras+especiales)
                                ├─► Hash de contraseña (BCrypt/Argon2)
                                ├─► Crea Cliente con rol CLIENTE, saldo >= $500.000
                                ├─► PuertoRepositorioCliente.guardar()
                                └─► RegistroResultado
HTTP Response 201
```

### POST `/api/auth/login` (Command — Público, HU 1.2)

```
HTTP Request (sin token)
    └─► SecurityFilterChain → permitido (endpoint público)
        └─► ControladorAutenticacion
                │  crea LoginComando(email, contraseña)
                └─► Mediador.enviar(LoginComando)
                        └─► LoginManejador.manejar(LoginComando)
                                ├─► PuertoRepositorioCliente.buscarPorEmail()
                                ├─► Verifica cuenta no bloqueada (o 403)
                                ├─► Verifica contraseña (hash comparison)
                                │     FALLO → incrementa intentosFallidos
                                │              si intentosFallidos >= 3 → bloquea cuenta
                                │              retorna 401
                                │     ÉXITO → resetea intentosFallidos a 0
                                ├─► JwtProveedor.generarToken(userId, rol)
                                └─► LoginResultado(token JWT)
HTTP Response 200 { "token": "eyJ..." }
```

### POST `/api/fondos/suscribir` (Command — Protegido, HU 2.1)

```
HTTP Request + Authorization: Bearer {token}
    └─► SecurityFilterChain → JwtFiltroAutenticacion → valida token → extrae userId, rol
        └─► ControladorFondo
                │  crea FondoComando(clienteId, fondoId, monto)
                └─► Mediador.enviar(FondoComando)
                        │  resuelve por tipo → FondoManejador (command)
                        └─► FondoManejador.manejar(FondoComando)
                                │  delega al servicio de dominio
                                └─► IServicioSuscripcion → ServicioSuscripcion
                                        ├─► PuertoRepositorioFondo → valida que el fondo exista
                                        ├─► PuertoRepositorioCliente → valida saldo suficiente
                                        │       descuenta saldo (lógica de negocio en Cliente)
                                        ├─► PuertoRepositorioSuscripcion → guarda la suscripción
                                        ├─► PuertoNotificacion.enviar() (fire-and-forget, HU 2.4)
                                        └─► FondoResultado (command)
                        └─► ControladorFondo retorna FondoResultado (command)
HTTP Response 201
```

### POST `/api/suscripciones/{id}/cancelar` (Command — Protegido, HU 2.2)

```
HTTP Request + Authorization: Bearer {token}
    └─► SecurityFilterChain → JwtFiltroAutenticacion → valida token
        └─► ControladorSuscripcion
                │  crea CancelacionComando(suscripcionId, clienteId del token)
                └─► Mediador.enviar(CancelacionComando)
                        └─► CancelacionManejador.manejar(CancelacionComando)
                                └─► IServicioSuscripcion.cancelar()
                                        ├─► PuertoRepositorioSuscripcion.buscarPorId()
                                        │       valida que exista y estado == ACTIVO
                                        │       valida que pertenezca al cliente (aislamiento)
                                        ├─► Cambia estado a CANCELADO
                                        ├─► PuertoRepositorioCliente → devuelve monto al saldo
                                        │       (lógica inversa: Cliente.abonarSaldo)
                                        ├─► PuertoRepositorioSuscripcion.guardar()
                                        └─► CancelacionResultado
HTTP Response 200
```

### GET `/api/suscripciones/vigentes` (Query — Protegido, HU 2.3)

```
HTTP Request + Authorization: Bearer {token}
    └─► SecurityFilterChain → JwtFiltroAutenticacion → valida token → extrae userId
        └─► ControladorSuscripcion
                │  crea SuscripcionesVigentesConsulta(clienteId del token)
                └─► Mediador.enviar(SuscripcionesVigentesConsulta)
                        └─► SuscripcionesVigentesManejador.manejar(...)
                                └─► PuertoRepositorioSuscripcion.buscarPorClienteIdYEstado(clienteId, ACTIVO)
                                        └─► Lista<Suscripcion> con datos del fondo (nombre)
HTTP Response 200 (lista, puede ser vacía)
```

### GET `/api/fondos/{fondoId}` (Query — Protegido)

```
HTTP Request + Authorization: Bearer {token}
    └─► SecurityFilterChain → JwtFiltroAutenticacion → valida token
        └─► ControladorFondo
                │  crea FondoConsulta(fondoId)
                └─► Mediador.enviar(FondoConsulta)
                        │  resuelve por tipo → FondoManejador (query)
                        └─► FondoManejador.manejar(FondoConsulta)
                                └─► IServicioFondo → ServicioFondo
                                        └─► PuertoRepositorioFondo
                                                └─► FondoResultado (query)
                        └─► ControladorFondo retorna FondoResultado (query)
HTTP Response 200
```

---

## Responsabilidad de cada Capa

### Dominio
- Entidades de negocio con lógica encapsulada (`Cliente.descontarSaldo`, `Cliente.abonarSaldo`)
- Interfaces de repositorios (`PuertoRepositorioX`) — define qué necesita, no cómo
- Interfaces de servicios (`IServicioX`) — contratos que infraestructura implementa
- `PuertoNotificacion` — interfaz fire-and-forget para envío de notificaciones (HU 2.4)
- `PuertoEncriptacion` — interfaz para hashing y encriptación de datos sensibles (HU 1.4)
- Excepción de dominio (`ExcepcionDominio`)
- **No conoce** Spring, infraestructura, ni capa de aplicación

### Aplicación
- Patrón **CQRS** organizado por entidad de dominio
- `Manejador<T,R>` — interfaz genérica que todo handler implementa
- `Mediador` — resuelve automáticamente el handler según el tipo de solicitud enviada
- Comandos (DTOs de entrada): `FondoComando`, `RegistroComando`, `LoginComando`, `CancelacionComando`
- Consultas (DTOs de entrada): `FondoConsulta`, `SuscripcionesVigentesConsulta`
- Resultados (DTOs de salida): `FondoResultado`, `RegistroResultado`, `LoginResultado`, `CancelacionResultado`
- **TODOS los DTOs viven aquí** — los controladores de API reciben y retornan directamente estos objetos. La capa de aplicación es autosuficiente: los manejadores CQRS toman decisiones directamente sobre los DTOs de entrada sin depender de transformaciones de la capa API
- **No conoce** el modelo de dominio ni infraestructura

### Infraestructura
- **Única capa** que implementa interfaces y manipula el modelo de dominio
- `ServicioX` — contiene la lógica de negocio llamando repositorios
- `AdaptadorXDynamoDb` — implementación DynamoDB (AWS SDK v2 Enhanced Client)
- Modelos de mapeo DynamoDB: `ClienteDynamoDb`, `FondoDynamoDb`, `SuscripcionDynamoDb` — DTOs de persistencia con `fromDomain()`/`toDomain()`
- `AdaptadorNotificacion` — implementación del puerto de notificación (HU 2.4)
- **Seguridad**: `JwtFiltroAutenticacion`, `JwtProveedor`, `DetallesUsuarioServicio`, `SecurityConfig` (HU 1.1-1.3)
- **Conoce** dominio y aplicación

### API
- Controladores REST **delgados** que reciben objetos de aplicación (Comandos/Consultas) directamente como @RequestBody, invocan el `Mediador` y retornan Resultados de aplicación como respuesta HTTP
- `ControladorFondo` — suscripción a fondos y consulta de fondo por ID
- `ControladorSuscripcion` — cancelación de suscripciones y consulta de vigentes
- `ControladorAutenticacion` — registro y login (endpoints públicos)
- `ControladorAdmin` — desbloqueo de cuentas (ADMINISTRADOR)
- **No tiene DTOs propios** — usa directamente los Comandos, Consultas y Resultados de la capa de aplicación
- `ManejadorExcepcionesGlobal` — traduce `ExcepcionDominio` a HTTP 422, validaciones a 400, auth a 401/403, duplicados a 409
- **Solo conoce** el Mediador y los objetos de comando/consulta/resultado de la capa de aplicación

---

## Patrón Mediador

**"MediatR"** Permite al controlador enviar cualquier solicitud sin conocer qué handler la procesa.

```java
// Registro automático al arrancar Spring
Mediador
  ├── FondoComando.class                →  FondoManejador (command — suscribir)
  ├── FondoConsulta.class               →  FondoManejador (query — consultar fondo)
  ├── RegistroComando.class             →  RegistroManejador (command — registro usuario, HU 1.1)
  ├── LoginComando.class                →  LoginManejador (command — login + JWT, HU 1.2)
  ├── CancelacionComando.class          →  CancelacionManejador (command — cancelar suscripción, HU 2.2)
  └── SuscripcionesVigentesConsulta.class → SuscripcionesVigentesManejador (query — listar vigentes, HU 2.3)

// En el controlador
mediador.enviar(new FondoComando(...));                   // suscribir
mediador.enviar(new FondoConsulta(...));                  // consultar fondo
mediador.enviar(new RegistroComando(...));                // registrar usuario
mediador.enviar(new LoginComando(...));                   // login
mediador.enviar(new CancelacionComando(...));             // cancelar suscripción
mediador.enviar(new SuscripcionesVigentesConsulta(...));  // listar vigentes
```

Para agregar un nuevo caso de uso basta con crear un `@Component` que implemente `Manejador<T,R>`. El Mediador lo registra automáticamente sin modificar el controlador.

---

## Reglas de Negocio

| Regla | Implementación | HU |
|---|---|---|
| Monto mínimo por fondo | `ServicioSuscripcion` valida `monto >= fondo.montoMinimo` | 2.1 |
| Saldo suficiente del cliente | `Cliente.descontarSaldo()` lanza `ExcepcionDominio` si no alcanza. Mensaje incluye nombre del fondo | 2.1 |
| Sin suscripciones duplicadas | `PuertoRepositorioSuscripcion.existePorClienteIdYFondoId()` → duplicado = 422 | 2.1 |
| Re-suscripción tras cancelación | Permitido: si la suscripción fue cancelada previamente, el cliente puede volver a suscribirse al mismo fondo | 2.1, 2.2 |
| Cancelación solo de suscripción activa | `IServicioSuscripcion.cancelar()` valida `estado == ACTIVO`; otro estado → 422 | 2.2 |
| Devolución de saldo al cancelar | `Cliente.abonarSaldo(monto)` al cancelar suscripción activa | 2.2 |
| Aislamiento de datos por cliente | Toda operación filtra por `clienteId` extraído del JWT. Un cliente no puede operar datos de otro | 1.3 |
| Política de contraseña | Mínimo 5 caracteres, debe incluir números, letras y caracteres especiales | 1.1 |
| Bloqueo de cuenta | Tras 3 intentos fallidos de login, cuenta se bloquea. Solo ADMIN puede desbloquear | 1.2 |
| Hashing de contraseñas | Contraseñas almacenadas con BCrypt o Argon2 (nunca en texto plano) | 1.4 |
| Encriptación de saldos | Saldos encriptados con AES-256 en reposo | 1.4 |
| Expiración de token JWT | Token expira en 5 minutos por defecto. Configurable vía `application.properties` | 1.2 |

---

## Fondos Precargados

| ID | Nombre | Monto Mínimo | Categoría |
|---|---|---|---|
| fondo-1 | FPV_BTG_PACTUAL_RECAUDADORA | $75.000 | FPV |
| fondo-2 | FPV_BTG_PACTUAL_ECOPETROL | $125.000 | FPV |
| fondo-3 | DEUDAPRIVADA | $50.000 | FIC |
| fondo-4 | FDO-ACCIONES | $250.000 | FIC |
| fondo-5 | FPV_BTG_PACTUAL_DINAMICA | $100.000 | FPV |

## Modelo de Cliente Extendido (HU 1.1)

```java
Cliente {
    String id;                     // UUID generado
    String nombre;                 // obligatorio
    String email;                  // obligatorio, único
    String telefono;               // obligatorio (preferencia notificación)
    String documentoIdentidad;     // obligatorio, único
    String contrasenaHash;         // BCrypt/Argon2 — nunca texto plano
    double saldo;                  // >= $500.000 al registro, encriptado en reposo (AES-256)
    Rol rol;                       // CLIENTE | ADMINISTRADOR (default = CLIENTE)
    boolean cuentaBloqueada;       // default false, true tras 3 intentos fallidos
    int intentosFallidosLogin;     // 0..3, reset a 0 al login exitoso
    LocalDateTime fechaRegistro;   // timestamp de creación
}
```

## Clientes Precargados

| ID | Nombre | Email | Saldo | Rol |
|---|---|---|---|---|
| cliente-1 | Juan Rodriguez | juan.rodriguez@email.com | $500.000 (encriptado) | CLIENTE |
| cliente-2 | Maria Lopez | maria.lopez@email.com | $1.000.000 (encriptado) | CLIENTE |

---

## Endpoints

### Públicos (sin autenticación)

| Método | URL | Descripción | HTTP |
|---|---|---|---|
| POST | `/api/auth/registro` | Registrar nuevo usuario (HU 1.1) | 201 / 400 / 409 |
| POST | `/api/auth/login` | Autenticarse y obtener JWT (HU 1.2) | 200 / 401 / 403 |

### Protegidos (requieren Bearer JWT)

| Método | URL | Descripción | HTTP |
|---|---|---|---|
| POST | `/api/fondos/suscribir` | Suscribir cliente a un fondo (HU 2.1) | 201 / 400 / 422 |
| POST | `/api/suscripciones/{id}/cancelar` | Cancelar suscripción activa (HU 2.2) | 200 / 422 |
| GET | `/api/suscripciones/vigentes` | Listar suscripciones vigentes del cliente (HU 2.3) | 200 |
| GET | `/api/fondos/{fondoId}` | Consultar datos de un fondo | 200 / 422 |

### Ejemplo POST `/api/auth/registro`

**Request:**
```json
{
  "nombre": "Juan Rodriguez",
  "email": "juan@correo.com",
  "telefono": "+573001234567",
  "documentoIdentidad": "1234567890",
  "contrasena": "P@ss1word",
  "saldoInicial": 500000
}
```

**Response 201:**
```json
{
  "clienteId": "cliente-uuid",
  "nombre": "Juan Rodriguez",
  "email": "juan@correo.com",
  "rol": "CLIENTE"
}
```

**Response 409 (email duplicado):**
```json
{ "error": "Ya existe un usuario registrado con ese email" }
```

### Ejemplo POST `/api/auth/login`

**Request:**
```json
{
  "email": "juan@correo.com",
  "contrasena": "P@ss1word"
}
```

**Response 200:**
```json
{ "token": "eyJhbGciOiJIUzI1NiIs..." }
```

**Response 401 (credenciales incorrectas):**
```json
{ "error": "Credenciales incorrectas" }
```

**Response 403 (cuenta bloqueada):**
```json
{ "error": "Cuenta bloqueada por múltiples intentos fallidos" }
```

### Ejemplo POST `/api/fondos/suscribir`

**Request:**
```json
{
  "fondoId": "fondo-1",
  "monto": 75000
}
```
> `clienteId` se extrae del token JWT (aislamiento de datos).

**Response 201:**
```json
{
  "suscripcionId": "3fa85f64-...",
  "clienteId": "cliente-1",
  "fondoId": "fondo-1",
  "monto": 75000,
  "estado": "ACTIVO",
  "fechaSuscripcion": "2026-03-11T20:00:00"
}
```

**Response 422 (regla de negocio):**
```json
{
  "error": "El monto mínimo para vincularse al fondo FPV_BTG_PACTUAL_RECAUDADORA es $75000"
}
```

**Response 400 (validación):**
```json
{
  "errores": ["fondoId: El fondoId es obligatorio"]
}
```

### Ejemplo POST `/api/suscripciones/{id}/cancelar`

**Response 200:**
```json
{
  "suscripcionId": "3fa85f64-...",
  "estado": "CANCELADO",
  "montoDevuelto": 75000
}
```

**Response 422 (no activa):**
```json
{ "error": "La suscripción no se encuentra en estado ACTIVO" }
```

### Ejemplo GET `/api/suscripciones/vigentes`

**Response 200:**
```json
[
  {
    "suscripcionId": "3fa85f64-...",
    "fondoId": "fondo-1",
    "nombreFondo": "FPV_BTG_PACTUAL_RECAUDADORA",
    "monto": 75000,
    "estado": "ACTIVO",
    "fechaSuscripcion": "2026-03-11T20:00:00"
  }
]
```

**Response 200 (sin suscripciones):**
```json
[]
```

---

## Cobertura de Tests

**Total: 107 tests, 0 failures** (25 suites)

```
Tests E2E (DynamoDB Local vía Testcontainers):
├── ControladorAutenticacionE2ETest.java — 8 tests (registro, login, bloqueo)
├── ControladorFondoE2ETest.java — 22 tests (suscripción, consulta, JWT, BOLA, notificación)
├── ControladorAdminE2ETest.java — 5 tests (desbloqueo, roles)
├── ControladorSuscripcionE2ETest.java — 11 tests (cancelación, vigentes)

Tests unitarios (Mockito):
├── RegistroManejadorTest.java — 5 tests
├── LoginManejadorTest.java — 7 tests
├── DesbloqueoManejadorTest.java — 3 tests
├── ClienteTest.java — 11 tests

Tests integración DynamoDB (Testcontainers):
├── AdaptadorClienteDynamoDbTest.java — 6 tests
├── AdaptadorFondoDynamoDbTest.java — 2 tests
└── AdaptadorSuscripcionDynamoDbTest.java — 6 tests
```

---

## Diagrama de Dependencias entre Capas

```
┌─────────────────────────────────────────────────┐
│                  SECURITY                       │
│  SecurityFilterChain → JwtFiltroAutenticacion   │
│  Endpoints públicos: /api/auth/* (sin filtro)   │
│  Endpoints protegidos: todo lo demás (JWT req.) │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│                     API                         │
│  ControladorFondo → Mediador                    │
│  ControladorAutenticacion → Mediador            │
│  DTOs de entrada/salida + Bean Validation       │
│  ManejadorExcepcionesGlobal (422/400/401/403)   │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│                APPLICATION                      │
│  Mediador → XManejador (command/query)          │
│  6 handlers: Fondo(cmd/qry), Registro, Login,   │
│              Cancelacion, SuscripcionesVigentes  │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│                  DOMAIN                         │
│  IServicioX  (interfaces)                       │
│  PuertoRepositorioX  (interfaces)               │
│  PuertoNotificacion  (fire-and-forget)          │
│  PuertoEncriptacion  (hashing + AES)            │
│  Fondo, Cliente, Suscripcion  (modelos)         │
│  ExcepcionDominio                               │
└────────────────────┬────────────────────────────┘
                     │ implementa
┌────────────────────▼────────────────────────────┐
│               INFRASTRUCTURE                    │
│  ServicioX         implements IServicioX        │
│  AdaptadorXDynamoDb   implements PuertoX        │
│  ClienteDynamoDb/FondoDynamoDb/SuscripcionDynamoDb│
│  AdaptadorNotificacion   implements PuertoNot.   │
│  JwtProveedor, DetallesUsuarioServicio          │
└─────────────────────────────────────────────────┘

Regla: las flechas van hacia abajo. Ninguna capa
       inferior conoce a las capas superiores.
       Security filtra ANTES de que la petición
       llegue a la capa API.
```

---

## Deuda Técnica y Estado de Implementación

> **Fecha del análisis:** 13 de marzo de 2026
> **Código base:** 40+ archivos Java · 7 endpoints · 107 tests (E2E + integración + unitarios)

### Resumen ejecutivo

El código implementa **todas las historias de usuario** (HU 1.1–1.4, 2.1–2.4, 3). La persistencia usa
Amazon DynamoDB Local como única implementación. Los tests E2E y de integración usan Testcontainers.

### Cobertura por Historia de Usuario

| HU | Descripción | Estado | % |
|---|---|---|---|
| 1.1 | Registro de usuarios y modelo de credenciales | **Implementado** | 100% |
| 1.2 | Autenticación JWT y política de bloqueo | **Implementado** | 100% |
| 1.3 | Autorización por roles y aislamiento de datos | **Implementado** | 100% |
| 1.4 | Encriptación de datos sensibles en reposo | **Implementado** | 100% |
| 2.1 | Suscripción a fondo de inversión | **Implementado** | 100% |
| 2.2 | Cancelación de suscripción | **Implementado** | 100% |
| 2.3 | Consulta de suscripciones vigentes | **Implementado** | 100% |
| 2.4 | Notificación email/SMS | **Implementado** | 100% |
| 3 | Modelo de datos DynamoDB | **Implementado** | 100% |

### Violaciones arquitectónicas — Resueltas

Todas las violaciones arquitectónicas detectadas en el análisis inicial han sido corregidas:

- ✅ **Dominio ya no depende de la capa de Aplicación** — Las interfaces de servicio usan modelos de dominio como parámetros
- ✅ **Mensaje de saldo incluye nombre del fondo** — `Cliente.descontarSaldo(monto, nombreFondo)`
- ✅ **clienteId se extrae del JWT** — Aislamiento de datos implementado (prevención BOLA)
- ✅ **ManejadorExcepcionesGlobal completo** — Maneja 400, 401, 403, 409, 422
- ✅ **Tests E2E con seguridad** — Todos usan `@WithMockUser` o tokens JWT reales

### Componentes implementados (HU 3 — DynamoDB)

#### Infraestructura — Persistencia DynamoDB

| Componente | Paquete | Descripción |
|---|---|---|
| `ConfiguracionDynamoDb` | `config/` | Beans `DynamoDbClient` + `DynamoDbEnhancedClient` |
| `EsquemaDynamoDb` | `config/` | Creación centralizada de 3 tablas con GSIs |
| `InicializadorTablasDynamoDb` | `config/` | `@PostConstruct` — crea tablas + datos semilla (encriptados) |
| `AdaptadorClienteDynamoDb` | `adapter/out/persistence/` | PK: id, GSI: email-index. Encripta/desencripta saldo |
| `AdaptadorFondoDynamoDb` | `adapter/out/persistence/` | PK: id. Catálogo inmutable |
| `AdaptadorSuscripcionDynamoDb` | `adapter/out/persistence/` | PK: clienteId, SK: id, GSI: id-index. Denormaliza nombreFondo |
| `ClienteDynamoDb` | `adapter/out/persistence/dynamodb/` | DTO de mapeo con `fromDomain()`/`toDomain()` |
| `FondoDynamoDb` | `adapter/out/persistence/dynamodb/` | DTO de mapeo con `fromDomain()`/`toDomain()` |
| `SuscripcionDynamoDb` | `adapter/out/persistence/dynamodb/` | DTO de mapeo con `fromDomain()`/`toDomain()` |

#### Tests — DynamoDB (Testcontainers)

| Componente | Descripción |
|---|---|
| `E2ETestBase` | Base abstracta con Testcontainers singleton + `@DynamicPropertySource` + reinicio por test |
| `DynamoDbIntegrationTestBase` | Base para tests unitarios de adaptadores DynamoDB |
| `AdaptadorClienteDynamoDbTest` | 6 tests de integración |
| `AdaptadorFondoDynamoDbTest` | 2 tests de integración |
| `AdaptadorSuscripcionDynamoDbTest` | 6 tests de integración |

#### API (1 controlador nuevo — sin DTOs propios)

| Componente | HU |
|---|---|
| `ControladorAutenticacion` | 1.1, 1.2 |

⚠️ **Nota:** Los DTOs de entrada (`RegistroComando`, `LoginComando`, `CancelacionComando`) y salida (`RegistroResultado`, `LoginResultado`) viven en la **capa de aplicación**, no en `api/dto/`. Los controladores reciben directamente los Comandos/Consultas como @RequestBody y retornan los Resultados de aplicación.

#### Dependencias faltantes en build.gradle

| Dependencia | Propósito | HU |
|---|---|---|
| `spring-boot-starter-security` | Spring Security autoconfiguration | 1.1-1.3 |
| `io.jsonwebtoken:jjwt-api` + `jjwt-impl` + `jjwt-jackson` | Generación y validación de JWT | 1.2 |
| `com.azure:azure-cosmos` + `spring-cloud-azure-starter-data-cosmos` | Azure Cosmos DB SDK | 3 |
| `spring-boot-starter-mail` | Envío de email (notificaciones) | 2.4 |

### Orden de implementación recomendado

```
 FASE 0 ─ Corregir violaciones arquitectónicas (dominio→aplicación)
    │
    ▼
 FASE 1 ─ HU 1.1-1.4: Seguridad completa
    │      (modelo Cliente extendido, registro, login, JWT, roles, encriptación)
    │      Todas las HU restantes dependen de autenticación
    │
    ▼
 FASE 2 ─ HU 2.2: Cancelación de suscripción
    │      (abonarSaldo, buscarPorId, nuevo handler, endpoint)
    │
    ▼
 FASE 3 ─ HU 2.3: Listado de suscripciones vigentes
    │      (buscarPorClienteIdYEstado, nuevo handler, endpoint)
    │
    ▼
 FASE 4 ─ HU 2.4: Notificaciones fire-and-forget
    │      (PuertoNotificacion, adaptadores, integración en suscribir/cancelar)
    │
    ▼
 FASE 5 ─ HU 3: Cosmos DB
           (adaptadores Cosmos, configuración, migración de datos)
```

### Métricas actuales vs objetivo

| Métrica | Actual | Objetivo |
|---|---|---|
| Endpoints implementados | 2 | 6 |
| Handlers del Mediador | 2 | 6 |
| Puertos de dominio | 3 | 5+ |
| Reglas de negocio activas | 3 | 12 |
| Tests E2E | 13 | ~40+ |
| Violaciones arquitectónicas | 4 | 0 |
| Dependencias en build.gradle | 3 | 7+ |
