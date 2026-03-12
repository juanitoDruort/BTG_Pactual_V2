# Arquitectura BTG Pactual V2

## Stack Tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 24 |
| Framework | Spring Boot 4.0.3 |
| Build | Gradle 9.3.1 |
| Documentación API | SpringDoc OpenAPI 3.0.2 |
| Seguridad | Spring Security + JWT (stateless) |
| Persistencia (desarrollo) | En memoria (ConcurrentHashMap) |
| Persistencia (runtime) | Azure Cosmos DB — API NoSQL (emulador Docker en desarrollo) |
| Notificaciones | Puerto de salida fire-and-forget (email/SMS) |
| Tests | JUnit 5 + MockMvc |

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
├── application/                                ← Orquestación CQRS + Mediador
│   ├── mediador/
│   │   ├── Manejador.java                      ← Interfaz genérica T→R
│   │   └── Mediador.java                       ← Resuelve handler por tipo
│   └── fondo/                                  ← Agrupado por entidad
│       ├── command/
│       │   ├── FondoComando.java               ← Input del command
│       │   ├── FondoResultado.java             ← Output del command
│       │   └── FondoManejador.java             ← Command handler
│       └── query/
│           ├── FondoConsulta.java              ← Input del query
│           ├── FondoResultado.java             ← Output del query
│           └── FondoManejador.java             ← Query handler
│
├── infrastructure/                             ← Toda implementación de interfaces
│   ├── config/
│   │   └── SecurityConfig.java                 ← Configuración Spring Security + JWT
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
│           │   ├── AdaptadorFondoEnMemoria.java
│           │   ├── AdaptadorClienteEnMemoria.java
│           │   ├── AdaptadorSuscripcionEnMemoria.java
│           │   ├── AdaptadorFondoCosmosDb.java      ← Nuevo: Cosmos DB (HU 3)
│           │   ├── AdaptadorClienteCosmosDb.java     ← Nuevo: Cosmos DB (HU 3)
│           │   └── AdaptadorSuscripcionCosmosDb.java  ← Nuevo: Cosmos DB (HU 3)
│           └── notification/
│               └── AdaptadorNotificacion.java  ← Nuevo: implements PuertoNotificacion (HU 2.4)
│
└── api/                                        ← Adaptador HTTP de entrada
    ├── controller/
    │   ├── ControladorFondo.java               ← Suscribir, cancelar, listar suscripciones, consultar fondo
    │   └── ControladorAutenticacion.java       ← Nuevo: registro + login (endpoints públicos)
    ├── dto/
    │   ├── SolicitudSuscripcion.java
    │   ├── SolicitudCancelacion.java           ← Nuevo (HU 2.2)
    │   ├── SolicitudRegistro.java              ← Nuevo (HU 1.1)
    │   ├── SolicitudLogin.java                 ← Nuevo (HU 1.2)
    │   ├── RespuestaSuscripcion.java
    │   ├── RespuestaFondo.java
    │   ├── RespuestaRegistro.java              ← Nuevo (HU 1.1)
    │   ├── RespuestaLogin.java                 ← Nuevo (HU 1.2)
    │   └── RespuestaSuscripcionVigente.java    ← Nuevo (HU 2.3)
    └── handler/
        └── ManejadorExcepcionesGlobal.java     ← Extendido: +401, +403, +409
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
                        └─► ControladorFondo mapea a RespuestaSuscripcion
HTTP Response 201
```

### POST `/api/suscripciones/{id}/cancelar` (Command — Protegido, HU 2.2)

```
HTTP Request + Authorization: Bearer {token}
    └─► SecurityFilterChain → JwtFiltroAutenticacion → valida token
        └─► ControladorFondo
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
        └─► ControladorFondo
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
                        └─► ControladorFondo mapea a RespuestaFondo
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
- Comandos: `FondoComando`, `RegistroComando`, `LoginComando`, `CancelacionComando`
- Consultas: `FondoConsulta`, `SuscripcionesVigentesConsulta`
- Resultados: `FondoResultado`, `RegistroResultado`, `LoginResultado`, `CancelacionResultado`
- **No conoce** el modelo de dominio ni infraestructura

### Infraestructura
- **Única capa** que implementa interfaces y manipula el modelo de dominio
- `ServicioX` — contiene la lógica de negocio llamando repositorios
- `AdaptadorXEnMemoria` — implementación en memoria (dev/test)
- `AdaptadorXCosmosDb` — implementación Cosmos DB (producción, HU 3)
- `AdaptadorNotificacionEmail` / `AdaptadorNotificacionSms` — implementación del puerto de notificación (HU 2.4)
- **Seguridad**: `JwtFiltroAutenticacion`, `JwtProveedor`, `DetallesUsuarioServicio`, `SecurityConfig` (HU 1.1-1.3)
- **Conoce** dominio y aplicación

### API
- Controladores REST que reciben DTOs, invocan el `Mediador` y retornan respuesta HTTP
- `ControladorFondo` — suscripción, cancelación, consulta de fondos y suscripciones vigentes
- `ControladorAutenticacion` — registro y login (endpoints públicos)
- DTOs de entrada: `SolicitudSuscripcion`, `SolicitudCancelacion`, `SolicitudRegistro`, `SolicitudLogin`
- DTOs de salida: `RespuestaSuscripcion`, `RespuestaFondo`, `RespuestaRegistro`, `RespuestaLogin`, `RespuestaSuscripcionVigente`
- `ManejadorExcepcionesGlobal` — traduce `ExcepcionDominio` a HTTP 422, validaciones a 400, auth a 401/403, duplicados a 409
- **Solo conoce** el Mediador y los objetos de comando/consulta de la capa de aplicación

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
| cliente-1 | Juan Rodriguez | juan@correo.com | $500.000 | CLIENTE |
| cliente-2 | Maria Lopez | maria@correo.com | $1.000.000 | CLIENTE |

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

## Cobertura de Tests E2E

```
src/test/.../ControladorFondoE2ETest.java  — 13 tests
│
├── POST /api/fondos/suscribir
│   ├── ✓ 201 - suscripción exitosa
│   ├── ✓ 400 - clienteId vacío
│   ├── ✓ 400 - fondoId vacío
│   ├── ✓ 400 - monto negativo
│   ├── ✓ 422 - fondo no existe
│   ├── ✓ 422 - cliente no existe
│   ├── ✓ 422 - monto menor al mínimo
│   ├── ✓ 422 - saldo insuficiente
│   └── ✓ 422 - cliente ya vinculado al fondo
│
└── GET /api/fondos/{fondoId}
    ├── ✓ 200 - fondo existe
    ├── ✓ 200 - todos los fondos precargados consultables
    └── ✓ 422 - fondo no existe
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
│  AdaptadorXEnMemoria  implements PuertoX (dev)  │
│  AdaptadorXCosmosDb   implements PuertoX (prod) │
│  AdaptadorNotificacionX  implements PuertoNot.  │
│  JwtProveedor, DetallesUsuarioServicio          │
└─────────────────────────────────────────────────┘

Regla: las flechas van hacia abajo. Ninguna capa
       inferior conoce a las capas superiores.
       Security filtra ANTES de que la petición
       llegue a la capa API.
```

---

## Deuda Técnica y Estado de Implementación

> **Fecha del análisis:** 12 de marzo de 2026
> **Código base:** 28 archivos Java (26 main + 2 test) · 2 endpoints · 13 tests E2E

### Resumen ejecutivo

El código actual implementa únicamente la **suscripción a fondos (HU 2.1)** y la consulta de fondos.
Las 8 historias restantes (seguridad, cancelación, listado, notificaciones, Cosmos DB) están documentadas
en esta arquitectura pero **no tienen implementación**. Adicionalmente existen **2 violaciones arquitectónicas
críticas** en el código existente que deben corregirse antes de avanzar.

### Cobertura por Historia de Usuario

| HU | Descripción | Estado | % |
|---|---|---|---|
| 1.1 | Registro de usuarios y modelo de credenciales | No implementado | 0% |
| 1.2 | Autenticación JWT y política de bloqueo | No implementado | 0% |
| 1.3 | Autorización por roles y aislamiento de datos | No implementado | 0% |
| 1.4 | Encriptación de datos sensibles en reposo | No implementado | 0% |
| 2.1 | Suscripción a fondo de inversión | **Implementado** (parcial — sin JWT ni notificación) | 70% |
| 2.2 | Cancelación de suscripción | No implementado | 0% |
| 2.3 | Consulta de suscripciones vigentes | No implementado | 0% |
| 2.4 | Notificación email/SMS | No implementado | 0% |
| 3 | Modelo de datos Cosmos DB | No implementado | 0% |

### Violaciones arquitectónicas activas

#### CRÍTICA — Dominio depende de la capa de Aplicación

Las interfaces de servicio en el dominio importan directamente clases de la capa `application`:

| Archivo | Importación indebida | Regla violada |
|---|---|---|
| `domain/service/IServicioSuscripcion.java` | `application.fondo.command.FondoComando` y `FondoResultado` | Dominio NO conoce aplicación |
| `domain/service/IServicioFondo.java` | `application.fondo.query.FondoConsulta` y `FondoResultado` | Dominio NO conoce aplicación |

**Corrección requerida:** Las interfaces de dominio deben definir sus propios tipos de entrada/salida (o usar modelos de dominio). Los handlers de aplicación realizan el mapeo entre objetos de aplicación y objetos de dominio.

```
ANTES (violación):
  domain/IServicioSuscripcion → importa application/FondoComando
  
DESPUÉS (correcto):
  domain/IServicioSuscripcion → usa domain/model/* como parámetros
  application/FondoManejador  → mapea FondoComando → parámetros de dominio
```

#### IMPORTANTE — Mensaje de saldo insuficiente no incluye nombre del fondo

`Cliente.descontarSaldo()` lanza el mensaje `"No tiene saldo disponible para vincularse al fondo. Saldo actual: X"` sin incluir el nombre del fondo. La regla de negocio documentada requiere que el mensaje identifique a qué fondo se intentó vincular.

#### IMPORTANTE — clienteId explícito en SolicitudSuscripcion

`SolicitudSuscripcion` recibe `clienteId` como campo del body. Con la implementación de JWT (HU 1.3), el `clienteId` debe extraerse del token para garantizar aislamiento de datos. El DTO no debería exponer este campo.

#### MENOR — ManejadorExcepcionesGlobal incompleto

Solo maneja `ExcepcionDominio → 422` y `MethodArgumentNotValidException → 400`. Cuando se implemente seguridad, debe extenderse con:
- `401 Unauthorized` — token ausente, inválido o expirado
- `403 Forbidden` — cuenta bloqueada o sin permisos
- `409 Conflict` — email duplicado en registro

#### MENOR — Tests E2E sin configuración de seguridad

`ControladorFondoE2ETest` usa `MockMvc` sin Spring Security. Al agregar `spring-boot-starter-security`, los tests fallarán porque todos los endpoints quedarán protegidos por defecto. Se requiere configurar `@WithMockUser` o inyectar tokens de prueba.

### Componentes faltantes por capa

#### Dominio (4 componentes nuevos)

| Componente | Tipo | HU |
|---|---|---|
| Campos extendidos en `Cliente` (email, teléfono, documento, hash, rol, bloqueo, intentos, fecha) | Modelo | 1.1 |
| `Rol` (CLIENTE \| ADMINISTRADOR) | Enum | 1.1, 1.3 |
| `PuertoNotificacion` | Interfaz puerto de salida | 2.4 |
| `PuertoEncriptacion` | Interfaz puerto de salida | 1.4 |

#### Puertos — métodos faltantes

| Puerto existente | Método faltante | HU |
|---|---|---|
| `PuertoRepositorioCliente` | `existePorEmail(String email)` | 1.1 |
| `PuertoRepositorioCliente` | `buscarPorEmail(String email)` | 1.2 |
| `PuertoRepositorioSuscripcion` | `buscarPorId(String id)` | 2.2 |
| `PuertoRepositorioSuscripcion` | `buscarPorClienteIdYEstado(String clienteId, Estado estado)` | 2.3 |
| `IServicioSuscripcion` | `cancelar(...)` | 2.2 |
| `Cliente` | `abonarSaldo(BigDecimal monto)` | 2.2 |

#### Aplicación (4 handlers nuevos)

| Comando/Consulta | Manejador | HU |
|---|---|---|
| `RegistroComando` | `RegistroManejador` | 1.1 |
| `LoginComando` | `LoginManejador` | 1.2 |
| `CancelacionComando` | `CancelacionManejador` | 2.2 |
| `SuscripcionesVigentesConsulta` | `SuscripcionesVigentesManejador` | 2.3 |

#### Infraestructura (9+ componentes nuevos)

| Componente | Paquete | HU |
|---|---|---|
| `JwtFiltroAutenticacion` | `security/` | 1.2 |
| `JwtProveedor` | `security/` | 1.2 |
| `DetallesUsuarioServicio` | `security/` | 1.2 |
| `SecurityConfig` | `config/` | 1.2, 1.3 |
| `AdaptadorClienteCosmosDb` | `adapter/out/persistence/` | 3 |
| `AdaptadorFondoCosmosDb` | `adapter/out/persistence/` | 3 |
| `AdaptadorSuscripcionCosmosDb` | `adapter/out/persistence/` | 3 |
| `AdaptadorNotificacionEmail` | `adapter/out/notification/` | 2.4 |
| `AdaptadorNotificacionSms` | `adapter/out/notification/` | 2.4 |

#### API (4 DTOs nuevos + 1 controlador)

| Componente | HU |
|---|---|
| `ControladorAutenticacion` | 1.1, 1.2 |
| `SolicitudRegistro`, `SolicitudLogin`, `SolicitudCancelacion` | 1.1, 1.2, 2.2 |
| `RespuestaRegistro`, `RespuestaLogin`, `RespuestaSuscripcionVigente` | 1.1, 1.2, 2.3 |

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
