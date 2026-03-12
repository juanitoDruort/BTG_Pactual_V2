# Flujo de Autenticación Login y Política de Bloqueo de Cuentas

> **Fecha:** 2025-07-22  
> **Dominio:** Seguridad — Autenticación, Autorización y Política de Bloqueo  
> **Stack:** Java 24 · Spring Boot 4.0.3 · Spring Security · JJWT 0.12.6  
> **Patrón base:** Arquitectura Hexagonal + CQRS + Mediador  
> **Tipo de flujo:** Síncrono (sin operaciones asíncronas)  
> **Integraciones externas:** Ninguna (adaptadores en memoria)

---

## 📋 Introducción

### Descripción

Este documento describe los flujos de negocio del módulo de autenticación del sistema BTG Pactual V2, cubriendo cuatro escenarios principales:

1. **Login exitoso end-to-end** — Autenticación por email/contraseña con emisión de JWT stateless.
2. **Login fallido con bloqueo progresivo** — Incremento de intentos fallidos y bloqueo automático tras 3 intentos consecutivos.
3. **Validación JWT transversal en requests protegidas** — Filtro de seguridad que intercepta y valida tokens Bearer en cada petición.
4. **Desbloqueo administrativo** — Restauración de cuentas bloqueadas exclusivamente por un administrador.

### Alcance

- **Proceso de negocio:** Autenticación de usuarios por email/contraseña con emisión de JWT stateless y política de bloqueo automático tras 3 intentos fallidos consecutivos. Desbloqueo solo por administrador.
- **Puntos críticos de error:** Credenciales inválidas (401), cuenta bloqueada (403), token inválido/expirado (401 vía filtro).
- **Catálogo de excepciones HTTP:** 400 (validación), 401 (credenciales/token inválido), 403 (cuenta bloqueada), 409 (conflicto), 422 (dominio).

### Componentes Involucrados

| Componente | Capa | Responsabilidad |
|---|---|---|
| `ControladorAutenticacion` | API (`api/controller/`) | Expone endpoints REST `/api/auth/*`. Recibe comandos y delega al Mediador. |
| `Mediador` | Aplicación (`application/mediador/`) | Resuelve y despacha comandos al manejador correspondiente por tipo. |
| `LoginManejador` | Aplicación (`application/login/command/`) | Orquesta el flujo de login: buscar cliente, validar credenciales, gestionar intentos fallidos, emitir token. |
| `DesbloqueoManejador` | Aplicación (`application/desbloqueo/command/`) | Orquesta el desbloqueo de cuentas: buscar cliente, resetear estado de bloqueo. |
| `Cliente` | Dominio (`domain/model/`) | Entidad raíz con estado de bloqueo (`bloqueada`, `intentosFallidosLogin`) y métodos de negocio. |
| `JwtFiltroAutenticacion` | Infraestructura (`infrastructure/security/`) | `OncePerRequestFilter` que extrae, valida y establece el `SecurityContext` desde el token Bearer. |
| `JwtProveedor` | Infraestructura (`infrastructure/security/`) | Implementa `PuertoGenerarToken`. Genera tokens JWT (HMAC-SHA256) y extrae claims. |
| `PuertoRepositorioCliente` | Dominio (`domain/port/out/`) | Puerto de salida para persistencia de clientes. Métodos: `buscarPorEmail()`, `buscarPorId()`, `guardar()`. |
| `PuertoHashContrasena` | Dominio (`domain/port/out/`) | Puerto de salida para verificación de hash de contraseña (BCrypt). |
| `PuertoGenerarToken` | Dominio (`domain/port/out/`) | Puerto de salida para generación de tokens JWT. |
| `ManejadorExcepcionesGlobal` | API (`api/handler/`) | Traduce excepciones de dominio/aplicación a respuestas HTTP con códigos de estado apropiados. |
| `SecurityConfig` | Infraestructura (`infrastructure/config/`) | Configura la cadena de filtros de Spring Security, endpoints públicos/protegidos, política de sesiones stateless. |

---

## 🔄 Diagramas de Secuencia

### 1. Login Exitoso (End-to-End)

Flujo completo cuando un usuario proporciona credenciales válidas y su cuenta no está bloqueada.

```mermaid
sequenceDiagram
    actor Usuario as Usuario (HTTP Client)
    participant Ctrl as ControladorAutenticacion
    participant Med as Mediador
    participant LH as LoginManejador
    participant Repo as PuertoRepositorioCliente
    participant Hash as PuertoHashContrasena
    participant Dom as Cliente (Domain)
    participant JWT as JwtProveedor<br/>(PuertoGenerarToken)
    participant Exc as ManejadorExcepcionesGlobal

    Usuario->>Ctrl: POST /api/auth/login<br/>{email, contraseña}
    Note over Ctrl: @Valid LoginComando<br/>Validación de campos
    Ctrl->>Med: enviar(LoginComando)
    Med->>LH: manejar(LoginComando)

    LH->>Repo: buscarPorEmail(email)
    Repo-->>LH: Optional<Cliente>
    Note over LH: Cliente encontrado ✓

    LH->>Dom: estaBloqueada()
    Dom-->>LH: false ✓

    LH->>Hash: verificar(contraseña, contrasenaHash)
    Hash-->>LH: true ✓

    LH->>Dom: resetearIntentosFallidos()
    Note over Dom: intentosFallidosLogin = 0

    LH->>Repo: guardar(cliente)
    Repo-->>LH: Cliente actualizado

    LH->>JWT: generarToken(clienteId, rol)
    Note over JWT: JWT: sub=clienteId,<br/>claim "rol", iat, exp(5min)<br/>Firma: HMAC-SHA256
    JWT-->>LH: token JWT

    LH-->>Med: LoginResultado(token)
    Med-->>Ctrl: LoginResultado(token)
    Ctrl-->>Usuario: HTTP 200 OK<br/>{ token: "eyJhbG..." }
```

### 2. Login Fallido con Bloqueo Progresivo

Flujo cuando un usuario proporciona credenciales inválidas, mostrando el incremento progresivo de intentos fallidos hasta el bloqueo automático.

```mermaid
sequenceDiagram
    actor Usuario as Usuario (HTTP Client)
    participant Ctrl as ControladorAutenticacion
    participant Med as Mediador
    participant LH as LoginManejador
    participant Repo as PuertoRepositorioCliente
    participant Hash as PuertoHashContrasena
    participant Dom as Cliente (Domain)
    participant Exc as ManejadorExcepcionesGlobal

    Note over Usuario,Exc: ── Intento 1: Contraseña incorrecta ──

    Usuario->>Ctrl: POST /api/auth/login<br/>{email, contraseña_incorrecta}
    Ctrl->>Med: enviar(LoginComando)
    Med->>LH: manejar(LoginComando)

    LH->>Repo: buscarPorEmail(email)
    Repo-->>LH: Optional<Cliente>

    LH->>Dom: estaBloqueada()
    Dom-->>LH: false

    LH->>Hash: verificar(contraseña_incorrecta, contrasenaHash)
    Hash-->>LH: false ✗

    LH->>Dom: incrementarIntentosFallidos()
    Note over Dom: intentosFallidosLogin = 1<br/>(< 3 → no bloquear)

    LH->>Repo: guardar(cliente)

    LH->>Exc: throw ExcepcionCredencialesInvalidas
    Exc-->>Usuario: HTTP 401 UNAUTHORIZED

    Note over Usuario,Exc: ── Intento 2: Contraseña incorrecta ──

    Usuario->>Ctrl: POST /api/auth/login<br/>{email, contraseña_incorrecta}
    Ctrl->>Med: enviar(LoginComando)
    Med->>LH: manejar(LoginComando)

    LH->>Repo: buscarPorEmail(email)
    Repo-->>LH: Optional<Cliente>

    LH->>Dom: estaBloqueada()
    Dom-->>LH: false

    LH->>Hash: verificar(contraseña_incorrecta, contrasenaHash)
    Hash-->>LH: false ✗

    LH->>Dom: incrementarIntentosFallidos()
    Note over Dom: intentosFallidosLogin = 2<br/>(< 3 → no bloquear)

    LH->>Repo: guardar(cliente)

    LH->>Exc: throw ExcepcionCredencialesInvalidas
    Exc-->>Usuario: HTTP 401 UNAUTHORIZED

    Note over Usuario,Exc: ── Intento 3: Contraseña incorrecta → BLOQUEO ──

    Usuario->>Ctrl: POST /api/auth/login<br/>{email, contraseña_incorrecta}
    Ctrl->>Med: enviar(LoginComando)
    Med->>LH: manejar(LoginComando)

    LH->>Repo: buscarPorEmail(email)
    Repo-->>LH: Optional<Cliente>

    LH->>Dom: estaBloqueada()
    Dom-->>LH: false

    LH->>Hash: verificar(contraseña_incorrecta, contrasenaHash)
    Hash-->>LH: false ✗

    LH->>Dom: incrementarIntentosFallidos()
    Note over Dom: intentosFallidosLogin = 3<br/>(>= maxIntentosFallidos)

    LH->>Dom: bloquearCuenta()
    Note over Dom: bloqueada = true 🔒

    LH->>Repo: guardar(cliente)

    LH->>Exc: throw ExcepcionCredencialesInvalidas
    Exc-->>Usuario: HTTP 401 UNAUTHORIZED

    Note over Usuario,Exc: ── Intento 4: Cuenta ya bloqueada ──

    Usuario->>Ctrl: POST /api/auth/login<br/>{email, cualquier_contraseña}
    Ctrl->>Med: enviar(LoginComando)
    Med->>LH: manejar(LoginComando)

    LH->>Repo: buscarPorEmail(email)
    Repo-->>LH: Optional<Cliente>

    LH->>Dom: estaBloqueada()
    Dom-->>LH: true 🔒

    LH->>Exc: throw ExcepcionCuentaBloqueada
    Note over Exc: No incrementa contador
    Exc-->>Usuario: HTTP 403 FORBIDDEN
```

### 3. Validación JWT Transversal en Requests Protegidas

Flujo del filtro de seguridad que intercepta cada petición a endpoints protegidos, valida el token JWT y establece el contexto de seguridad.

```mermaid
sequenceDiagram
    actor Usuario as Usuario (HTTP Client)
    participant SC as SecurityFilterChain
    participant Filtro as JwtFiltroAutenticacion<br/>(OncePerRequestFilter)
    participant JWT as JwtProveedor
    participant Ctx as SecurityContext
    participant Ctrl as Controlador REST<br/>(endpoint protegido)
    participant Exc as HttpStatusEntryPoint

    Usuario->>SC: GET /api/fondos<br/>Authorization: Bearer {token}

    SC->>SC: ¿Endpoint público?<br/>(/api/auth/login, /registro, /swagger-ui/**)
    Note over SC: NO → continuar filtro

    SC->>Filtro: doFilterInternal(request, response)
    Filtro->>Filtro: Extraer header<br/>"Authorization: Bearer {token}"

    alt Token presente y válido
        Filtro->>JWT: validarToken(token)
        JWT-->>Filtro: true ✓

        Filtro->>JWT: extraerUserId(token)
        JWT-->>Filtro: "cliente-uuid-123"

        Filtro->>JWT: extraerRol(token)
        JWT-->>Filtro: "CLIENTE"

        Filtro->>Ctx: setAuthentication(<br/>UsernamePasswordAuthenticationToken(<br/>userId, null, [ROLE_CLIENTE]))
        Note over Ctx: SecurityContext<br/>establecido ✓

        Filtro->>Ctrl: continuar cadena de filtros
        Ctrl-->>Usuario: HTTP 200 OK<br/>{datos del recurso}

    else Token ausente
        Note over Filtro: No hay header Authorization
        Filtro->>Filtro: Continuar cadena sin autenticación
        Note over Ctx: SecurityContext vacío
        Filtro->>Exc: Spring Security intercepta
        Exc-->>Usuario: HTTP 401 UNAUTHORIZED

    else Token inválido o expirado
        Filtro->>JWT: validarToken(token)
        JWT-->>Filtro: false / excepción
        Note over Filtro: Token rechazado<br/>(firma inválida o expirado)
        Filtro->>Filtro: Continuar cadena sin autenticación
        Filtro->>Exc: Spring Security intercepta
        Exc-->>Usuario: HTTP 401 UNAUTHORIZED
    end
```

### 4. Desbloqueo Administrativo

Flujo de desbloqueo de una cuenta bloqueada, ejecutable únicamente por un usuario con rol `ADMINISTRADOR`.

```mermaid
sequenceDiagram
    actor Admin as Administrador
    participant SC as SecurityFilterChain
    participant Filtro as JwtFiltroAutenticacion
    participant JWT as JwtProveedor
    participant Ctrl as ControladorAutenticacion
    participant Med as Mediador
    participant DH as DesbloqueoManejador
    participant Repo as PuertoRepositorioCliente
    participant Dom as Cliente (Domain)
    participant Exc as ManejadorExcepcionesGlobal

    Admin->>SC: PUT /api/auth/desbloqueo/{clienteId}<br/>Authorization: Bearer {token_admin}

    SC->>Filtro: doFilterInternal()
    Filtro->>JWT: validarToken(token_admin)
    JWT-->>Filtro: true ✓
    Filtro->>JWT: extraerRol(token_admin)
    JWT-->>Filtro: "ADMINISTRADOR"
    Note over Filtro: SecurityContext con<br/>ROLE_ADMINISTRADOR ✓

    Filtro->>Ctrl: continuar cadena

    Ctrl->>Med: enviar(DesbloqueoComando(clienteId))
    Med->>DH: manejar(DesbloqueoComando)

    DH->>Repo: buscarPorId(clienteId)

    alt Cliente encontrado
        Repo-->>DH: Optional<Cliente>
        DH->>Dom: desbloquearCuenta()
        Note over Dom: bloqueada = false<br/>intentosFallidosLogin = 0 🔓

        DH->>Repo: guardar(cliente)
        Repo-->>DH: Cliente actualizado

        DH-->>Med: DesbloqueoResultado(<br/>id, email,<br/>"Cuenta desbloqueada exitosamente")
        Med-->>Ctrl: DesbloqueoResultado
        Ctrl-->>Admin: HTTP 200 OK<br/>{id, email, mensaje}

    else Cliente no encontrado
        Repo-->>DH: Optional.empty()
        DH->>Exc: throw ExcepcionDominio
        Exc-->>Admin: HTTP 422 UNPROCESSABLE_ENTITY
    end
```

---

## 📊 Estados y Transiciones

### Diagrama de Estados de Cuenta de Usuario

```mermaid
stateDiagram-v2
    [*] --> Activa : POST /api/auth/registro<br/>Cuenta creada exitosamente

    Activa --> Activa : Login exitoso<br/>resetearIntentosFallidos()<br/>intentosFallidosLogin = 0

    Activa --> IntentoFallido1 : Login fallido (intento 1)<br/>incrementarIntentosFallidos()

    IntentoFallido1 --> Activa : Login exitoso<br/>resetearIntentosFallidos()

    IntentoFallido1 --> IntentoFallido2 : Login fallido (intento 2)<br/>incrementarIntentosFallidos()

    IntentoFallido2 --> Activa : Login exitoso<br/>resetearIntentosFallidos()

    IntentoFallido2 --> Bloqueada : Login fallido (intento 3)<br/>incrementarIntentosFallidos()<br/>bloquearCuenta()

    Bloqueada --> Activa : PUT /api/auth/desbloqueo/{id}<br/>desbloquearCuenta()<br/>(Solo ADMINISTRADOR)

    Bloqueada --> Bloqueada : Intento de login<br/>HTTP 403 FORBIDDEN<br/>(sin incrementar contador)

    state Activa {
        [*] --> Autenticable
        Autenticable : bloqueada = false
        Autenticable : intentosFallidosLogin = 0
    }

    state IntentoFallido1 {
        [*] --> Riesgo1
        Riesgo1 : bloqueada = false
        Riesgo1 : intentosFallidosLogin = 1
    }

    state IntentoFallido2 {
        [*] --> Riesgo2
        Riesgo2 : bloqueada = false
        Riesgo2 : intentosFallidosLogin = 2
    }

    state Bloqueada {
        [*] --> Inaccesible
        Inaccesible : bloqueada = true
        Inaccesible : intentosFallidosLogin = 3
    }
```

### Tabla de Transiciones

| Estado Origen | Evento | Condición | Estado Destino | Efecto |
|---|---|---|---|---|
| Activa | Login exitoso | `contraseña válida` | Activa | `resetearIntentosFallidos()` → `intentosFallidosLogin = 0` |
| Activa | Login fallido | `contraseña inválida`, `intentos < 3` | IntentoFallido (1 o 2) | `incrementarIntentosFallidos()` |
| IntentoFallido (1 o 2) | Login exitoso | `contraseña válida` | Activa | `resetearIntentosFallidos()` → `intentosFallidosLogin = 0` |
| IntentoFallido 2 | Login fallido | `intentos >= maxIntentosFallidos (3)` | Bloqueada | `incrementarIntentosFallidos()` + `bloquearCuenta()` |
| Bloqueada | Intento de login | `estaBloqueada() == true` | Bloqueada | HTTP 403, sin modificar estado |
| Bloqueada | Desbloqueo admin | `PUT /desbloqueo/{id}` por `ADMINISTRADOR` | Activa | `desbloquearCuenta()` → `bloqueada = false`, `intentosFallidosLogin = 0` |

---

## 📋 Configuración y Parámetros

### Parámetros de Seguridad JWT

| Propiedad | Valor | Descripción | Impacto |
|---|---|---|---|
| `jwt.secret` | `S3cur3K3yBTG...` (en `application.properties`) | Clave HMAC-SHA256 para firma de tokens | Compromiso de esta clave invalida toda la seguridad JWT. **Migrar a vault en producción.** |
| `jwt.expiration-ms` | `300000` (5 minutos) | Tiempo de vida del token JWT | Ventana de exposición ante token comprometido. Balance entre seguridad y UX. |
| `jwt.max-failed-attempts` | `3` | Intentos fallidos consecutivos antes de bloqueo automático | Umbral de tolerancia. Valor bajo protege contra fuerza bruta. |

### Endpoints y Permisos

| Endpoint | Método | Acceso | Descripción |
|---|---|---|---|
| `/api/auth/login` | POST | Público | Autenticación por email/contraseña |
| `/api/auth/registro` | POST | Público | Registro de nuevos usuarios |
| `/api/auth/desbloqueo/{clienteId}` | PUT | Autenticado (ADMINISTRADOR) | Desbloqueo de cuenta |
| `/swagger-ui/**` | GET | Público | Documentación OpenAPI |
| `/v3/api-docs/**` | GET | Público | Especificación OpenAPI JSON |
| Todos los demás | * | Autenticado (Bearer JWT) | Requieren token válido |

### Configuración de Spring Security

| Aspecto | Configuración | Justificación |
|---|---|---|
| CSRF | Deshabilitado | Arquitectura stateless sin cookies de sesión |
| Sesiones | `STATELESS` | Sin estado en servidor; autenticación por token en cada request |
| Entry Point | `HttpStatusEntryPoint(UNAUTHORIZED)` | Retorna 401 sin redirección, adecuado para API REST |
| Filtro JWT | Before `UsernamePasswordAuthenticationFilter` | Intercepta antes del filtro estándar de Spring Security |

---

## 🔧 Métricas y Monitoreo

### Puntos Críticos de Medición

| Métrica | Componente | Tipo | Descripción |
|---|---|---|---|
| Login exitosos / minuto | `LoginManejador` | Contador | Tasa de autenticaciones exitosas |
| Login fallidos / minuto | `LoginManejador` | Contador | Tasa de intentos fallidos — picos indican ataque de fuerza bruta |
| Cuentas bloqueadas (acumulado) | `LoginManejador` | Gauge | Número total de cuentas actualmente bloqueadas |
| Desbloqueos realizados | `DesbloqueoManejador` | Contador | Tasa de desbloqueos administrativos |
| Tokens generados / minuto | `JwtProveedor` | Contador | Correlacionar con login exitosos |
| Tokens rechazados / minuto | `JwtFiltroAutenticacion` | Contador | Tokens inválidos o expirados — picos indican tokens comprometidos |
| Latencia de login (p50, p95, p99) | `LoginManejador` | Histograma | Tiempo de respuesta del flujo completo de login |
| Errores 401 / minuto | `ManejadorExcepcionesGlobal` | Contador | Credenciales o tokens inválidos |
| Errores 403 / minuto | `ManejadorExcepcionesGlobal` | Contador | Intentos de acceso con cuenta bloqueada |

### Puntos de Logging Recomendados

| Nivel | Componente | Evento | Datos a Registrar |
|---|---|---|---|
| `INFO` | `LoginManejador` | Login exitoso | `email` (enmascarado), `clienteId`, timestamp |
| `WARN` | `LoginManejador` | Login fallido | `email` (enmascarado), `intentosFallidosLogin`, IP origen |
| `WARN` | `LoginManejador` | Cuenta bloqueada automáticamente | `clienteId`, `email` (enmascarado), timestamp del bloqueo |
| `WARN` | `LoginManejador` | Intento de login en cuenta bloqueada | `clienteId`, `email` (enmascarado), IP origen |
| `INFO` | `DesbloqueoManejador` | Cuenta desbloqueada | `clienteId`, `adminId` (del token), timestamp |
| `WARN` | `JwtFiltroAutenticacion` | Token inválido o expirado | URI solicitada, tipo de error (firma/expiración), IP origen |
| `DEBUG` | `JwtProveedor` | Token generado | `clienteId`, `rol`, `exp` |

### Alertas Recomendadas

| Alerta | Condición | Severidad | Acción |
|---|---|---|---|
| Ataque de fuerza bruta | > 50 login fallidos/minuto desde misma IP | ALTA | Bloquear IP temporalmente, notificar equipo de seguridad |
| Ola de bloqueos | > 10 cuentas bloqueadas en 5 minutos | ALTA | Investigar patrón de ataque, considerar rate-limiting |
| Tokens rechazados masivos | > 100 tokens inválidos/minuto | MEDIA | Verificar si la clave JWT fue comprometida o rotada |
| Latencia elevada login | p95 > 2 segundos | MEDIA | Revisar rendimiento de repositorio y hash BCrypt |

---

## 🧪 Escenarios de Prueba

### Escenario 1: Login Exitoso

```gherkin
Feature: Autenticación de usuarios por email y contraseña

  Scenario: Login exitoso con credenciales válidas
    Given un cliente registrado con email "juan@example.com" y contraseña "Clave123!"
    And la cuenta no está bloqueada
    And la cuenta tiene 0 intentos fallidos
    When el cliente envía POST /api/auth/login con email "juan@example.com" y contraseña "Clave123!"
    Then el sistema responde con HTTP 200 OK
    And el body contiene un campo "token" con un JWT válido
    And el JWT contiene el claim "sub" con el ID del cliente
    And el JWT contiene el claim "rol" con valor "CLIENTE"
    And el JWT tiene una expiración de 5 minutos desde la emisión
    And los intentos fallidos del cliente se reinician a 0
```

### Escenario 2: Login Fallido con Bloqueo Progresivo

```gherkin
  Scenario: Bloqueo automático tras 3 intentos fallidos consecutivos
    Given un cliente registrado con email "juan@example.com" y contraseña "Clave123!"
    And la cuenta no está bloqueada
    And la cuenta tiene 0 intentos fallidos

    When el cliente envía POST /api/auth/login con email "juan@example.com" y contraseña "incorrecta"
    Then el sistema responde con HTTP 401 UNAUTHORIZED
    And los intentos fallidos del cliente son 1

    When el cliente envía POST /api/auth/login con email "juan@example.com" y contraseña "incorrecta"
    Then el sistema responde con HTTP 401 UNAUTHORIZED
    And los intentos fallidos del cliente son 2

    When el cliente envía POST /api/auth/login con email "juan@example.com" y contraseña "incorrecta"
    Then el sistema responde con HTTP 401 UNAUTHORIZED
    And los intentos fallidos del cliente son 3
    And la cuenta queda bloqueada automáticamente

    When el cliente envía POST /api/auth/login con email "juan@example.com" y contraseña "Clave123!"
    Then el sistema responde con HTTP 403 FORBIDDEN
    And el mensaje indica que la cuenta está bloqueada
    And los intentos fallidos siguen siendo 3
```

### Escenario 3: Token Inválido en Request Protegida

```gherkin
  Scenario: Acceso a recurso protegido con token inválido
    Given un endpoint protegido GET /api/fondos
    When el usuario envía la petición con header "Authorization: Bearer token_invalido_abc123"
    Then JwtFiltroAutenticacion extrae el token del header
    And JwtProveedor.validarToken() retorna false
    And el SecurityContext no se establece
    And Spring Security intercepta la petición
    And el sistema responde con HTTP 401 UNAUTHORIZED

  Scenario: Acceso a recurso protegido sin token
    Given un endpoint protegido GET /api/fondos
    When el usuario envía la petición sin header "Authorization"
    Then JwtFiltroAutenticacion no encuentra token Bearer
    And el SecurityContext permanece vacío
    And Spring Security intercepta la petición
    And el sistema responde con HTTP 401 UNAUTHORIZED

  Scenario: Acceso a recurso protegido con token expirado
    Given un cliente autenticado con un token JWT emitido hace 6 minutos
    And el token tiene expiración de 5 minutos
    When el cliente envía GET /api/fondos con el token expirado
    Then JwtProveedor.validarToken() detecta expiración
    And el sistema responde con HTTP 401 UNAUTHORIZED
```

### Escenario 4: Desbloqueo Administrativo

```gherkin
  Scenario: Administrador desbloquea cuenta bloqueada
    Given un cliente con ID "cliente-uuid-123" con cuenta bloqueada
    And un administrador autenticado con token JWT válido con rol "ADMINISTRADOR"
    When el administrador envía PUT /api/auth/desbloqueo/cliente-uuid-123
    Then el sistema responde con HTTP 200 OK
    And el body contiene el ID del cliente, su email y mensaje "Cuenta desbloqueada exitosamente"
    And la cuenta del cliente ya no está bloqueada
    And los intentos fallidos se reinician a 0

  Scenario: Desbloqueo de cliente inexistente
    Given un administrador autenticado con token JWT válido con rol "ADMINISTRADOR"
    When el administrador envía PUT /api/auth/desbloqueo/cliente-inexistente
    Then el sistema responde con HTTP 422 UNPROCESSABLE_ENTITY
```

### Matriz de Cobertura de Pruebas

| Escenario | Tipo | HTTP Status | Excepción | Componentes Ejercitados |
|---|---|---|---|---|
| Login exitoso | Happy path | 200 | — | `LoginManejador`, `PuertoRepositorioCliente`, `PuertoHashContrasena`, `JwtProveedor` |
| Login fallido (intento 1–2) | Error path | 401 | `ExcepcionCredencialesInvalidas` | `LoginManejador`, `PuertoRepositorioCliente`, `PuertoHashContrasena`, `Cliente.incrementarIntentosFallidos()` |
| Login fallido (intento 3 → bloqueo) | Error path | 401 | `ExcepcionCredencialesInvalidas` | `LoginManejador`, `Cliente.bloquearCuenta()` |
| Login con cuenta bloqueada | Error path | 403 | `ExcepcionCuentaBloqueada` | `LoginManejador`, `Cliente.estaBloqueada()` |
| Email no registrado | Error path | 401 | `ExcepcionCredencialesInvalidas` | `LoginManejador`, `PuertoRepositorioCliente` |
| Token válido en request protegida | Happy path | 200 | — | `JwtFiltroAutenticacion`, `JwtProveedor`, `SecurityContext` |
| Token inválido en request protegida | Error path | 401 | — | `JwtFiltroAutenticacion`, `JwtProveedor` |
| Token expirado en request protegida | Error path | 401 | — | `JwtFiltroAutenticacion`, `JwtProveedor` |
| Token ausente en request protegida | Error path | 401 | — | `JwtFiltroAutenticacion`, `SecurityConfig` |
| Desbloqueo exitoso | Happy path | 200 | — | `DesbloqueoManejador`, `PuertoRepositorioCliente`, `Cliente.desbloquearCuenta()` |
| Desbloqueo cliente inexistente | Error path | 422 | `ExcepcionDominio` | `DesbloqueoManejador`, `PuertoRepositorioCliente` |
| Validación de campos null/vacíos | Validation | 400 | `MethodArgumentNotValidException` | `ControladorAutenticacion`, Bean Validation |

---

## 🔍 Troubleshooting

### Problemas Frecuentes y Resolución

#### 1. HTTP 401 — Credenciales Inválidas

| Aspecto | Detalle |
|---|---|
| **Síntoma** | `POST /api/auth/login` retorna 401 con credenciales aparentemente correctas |
| **Causa probable** | Email no registrado, contraseña incorrecta, o hash inconsistente |
| **Diagnóstico** | Verificar que el email existe en el repositorio. Verificar que el hash almacenado corresponde al algoritmo BCrypt. Revisar logs de `LoginManejador` para ver el flujo ejecutado. |
| **Resolución** | Si es entorno de desarrollo, verificar que el adaptador en memoria tiene el cliente precargado. Si el hash no coincide, re-registrar el usuario. |

#### 2. HTTP 403 — Cuenta Bloqueada

| Aspecto | Detalle |
|---|---|
| **Síntoma** | `POST /api/auth/login` retorna 403 incluso con contraseña correcta |
| **Causa probable** | La cuenta fue bloqueada tras 3 intentos fallidos consecutivos |
| **Diagnóstico** | Verificar `cliente.estaBloqueada()` y `cliente.getIntentosFallidosLogin()`. Buscar en logs el evento de bloqueo con el `clienteId`. |
| **Resolución** | Ejecutar desbloqueo administrativo: `PUT /api/auth/desbloqueo/{clienteId}` con token de administrador. |

#### 3. HTTP 401 — Token Inválido en Request Protegida

| Aspecto | Detalle |
|---|---|
| **Síntoma** | Requests a endpoints protegidos retornan 401 con token que previamente funcionaba |
| **Causa probable** | Token expirado (TTL 5 min), clave JWT rotada, o token mal formado |
| **Diagnóstico** | Decodificar el JWT (sin verificar firma) para inspeccionar `exp`. Comparar con timestamp actual. Verificar que `jwt.secret` no cambió entre emisión y validación. |
| **Resolución** | Si expirado: re-autenticarse vía `/api/auth/login`. Si la clave cambió: todos los tokens existentes se invalidan — los usuarios deben re-autenticarse. |

#### 4. Login Lento (Alta Latencia)

| Aspecto | Detalle |
|---|---|
| **Síntoma** | `POST /api/auth/login` tarda > 2 segundos |
| **Causa probable** | BCrypt con factor de costo alto, o latencia del repositorio |
| **Diagnóstico** | Medir tiempo de `PuertoHashContrasena.verificar()` vs `PuertoRepositorioCliente.buscarPorEmail()`. |
| **Resolución** | Ajustar factor de costo de BCrypt si es excesivo. Si es repositorio, verificar índices en el campo `email`. |

#### 5. Desbloqueo No Funciona

| Aspecto | Detalle |
|---|---|
| **Síntoma** | `PUT /api/auth/desbloqueo/{clienteId}` retorna 401 o 403 |
| **Causa probable** | Token del administrador expirado, rol incorrecto, o `clienteId` inválido |
| **Diagnóstico** | Verificar que el token tiene claim `rol: ADMINISTRADOR`. Verificar que el `clienteId` existe en el repositorio. |
| **Resolución** | Re-autenticarse como administrador. Verificar que el UUID del cliente es correcto. |

### Catálogo de Excepciones HTTP

| Código | Excepción | Componente Origen | Causa | Acción del Cliente |
|---|---|---|---|---|
| 400 | `MethodArgumentNotValidException` | `ControladorAutenticacion` (Bean Validation) | Campos requeridos ausentes o formato inválido | Corregir payload según validaciones del `LoginComando` |
| 401 | `ExcepcionCredencialesInvalidas` | `LoginManejador` | Email no encontrado o contraseña incorrecta | Verificar credenciales y reintentar (con precaución por bloqueo) |
| 401 | `ExcepcionTokenInvalido` | `JwtFiltroAutenticacion` / `ManejadorExcepcionesGlobal` | Token JWT con firma inválida o expirado | Re-autenticarse vía `/api/auth/login` |
| 403 | `ExcepcionCuentaBloqueada` | `LoginManejador` | Cuenta bloqueada tras superar intentos fallidos | Contactar administrador para desbloqueo |
| 409 | `ExcepcionConflicto` | Manejadores de aplicación | Conflicto de estado (ej. email ya registrado) | Verificar estado actual del recurso |
| 422 | `ExcepcionDominio` | Manejadores de aplicación | Violación de regla de negocio | Revisar precondiciones de la operación |

---

## 📚 Referencias

| Fuente | Ubicación | Relevancia |
|---|---|---|
| Estrategia de Seguridad JWT | `docs/architecture/seguridad-jwt.md` | Detalle completo de la cadena de seguridad, filtros y configuración |
| Arquitectura del Sistema | `ARQUITECTURA.md` | Estructura hexagonal, capas, patrones CQRS + Mediador |
| Historia de Usuario 1.2 | `docs/stories/1.2.autenticacion-jwt-politica-bloqueo.story.md` | Requisitos funcionales de autenticación y bloqueo |
| Código fuente — LoginManejador | `src/main/java/.../application/login/command/LoginManejador.java` | Implementación del flujo de login |
| Código fuente — DesbloqueoManejador | `src/main/java/.../application/desbloqueo/command/DesbloqueoManejador.java` | Implementación del flujo de desbloqueo |
| Código fuente — JwtFiltroAutenticacion | `src/main/java/.../infrastructure/security/JwtFiltroAutenticacion.java` | Filtro transversal de validación JWT |
| Código fuente — SecurityConfig | `src/main/java/.../infrastructure/config/SecurityConfig.java` | Configuración de Spring Security |
| Código fuente — ManejadorExcepcionesGlobal | `src/main/java/.../api/handler/ManejadorExcepcionesGlobal.java` | Mapeo de excepciones a códigos HTTP |
