# Arquitectura BTG Pactual V2

## Stack Tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 24 |
| Framework | Spring Boot 4.0.3 |
| Build | Gradle 9.3.1 |
| Documentación API | SpringDoc OpenAPI 3.0.2 |
| Persistencia | En memoria (ConcurrentHashMap) |
| Tests | JUnit 5 + MockMvc |

---

## Patrón Arquitectónico

**Arquitectura Hexagonal (Ports & Adapters)** combinada con **CQRS** y **Mediador**.

### Principios aplicados
- El **dominio** no depende de ninguna capa externa
- Toda **implementación de interfaces** vive en infraestructura
- La **capa de aplicación** orquesta mediante CQRS sin conocer el modelo de dominio
- La **API** solo conoce el Mediador y los objetos de comando/consulta

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
│   │   ├── Cliente.java
│   │   ├── Fondo.java
│   │   └── Suscripcion.java
│   ├── port/
│   │   └── out/
│   │       ├── PuertoRepositorioCliente.java
│   │       ├── PuertoRepositorioFondo.java
│   │       └── PuertoRepositorioSuscripcion.java
│   └── service/
│       ├── IServicioFondo.java
│       └── IServicioSuscripcion.java
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
│   ├── service/
│   │   ├── ServicioSuscripcion.java            ← implements IServicioSuscripcion
│   │   └── ServicioFondo.java                  ← implements IServicioFondo
│   └── adapter/
│       └── out/
│           └── persistence/
│               ├── AdaptadorFondoEnMemoria.java
│               ├── AdaptadorClienteEnMemoria.java
│               └── AdaptadorSuscripcionEnMemoria.java
│
└── api/                                        ← Adaptador HTTP de entrada
    ├── controller/
    │   └── ControladorFondo.java
    ├── dto/
    │   ├── SolicitudSuscripcion.java
    │   ├── RespuestaSuscripcion.java
    │   └── RespuestaFondo.java
    └── handler/
        └── ManejadorExcepcionesGlobal.java
```

---

## Flujo de una Solicitud

### POST `/api/fondos/suscribir` (Command)

```
HTTP Request
    └─► ControladorFondo
            │  crea FondoComando(clienteId, fondoId, monto)
            └─► Mediador.enviar(FondoComando)
                    │  resuelve por tipo → FondoManejador (command)
                    └─► FondoManejador.manejar(FondoComando)
                            │  delega al servicio de dominio
                            └─► IServicioSuscripcion → ServicioSuscripcion
                                    ├─► PuertoRepositorioFondo → AdaptadorFondoEnMemoria
                                    │       valida que el fondo exista
                                    ├─► PuertoRepositorioCliente → AdaptadorClienteEnMemoria
                                    │       valida saldo suficiente
                                    │       descuenta saldo (lógica de negocio en Cliente)
                                    ├─► PuertoRepositorioSuscripcion → AdaptadorSuscripcionEnMemoria
                                    │       guarda la suscripción
                                    └─► FondoResultado (command)
                    └─► ControladorFondo mapea a RespuestaSuscripcion
HTTP Response 201
```

### GET `/api/fondos/{fondoId}` (Query)

```
HTTP Request
    └─► ControladorFondo
            │  crea FondoConsulta(fondoId)
            └─► Mediador.enviar(FondoConsulta)
                    │  resuelve por tipo → FondoManejador (query)
                    └─► FondoManejador.manejar(FondoConsulta)
                            └─► IServicioFondo → ServicioFondo
                                    └─► PuertoRepositorioFondo → AdaptadorFondoEnMemoria
                                            └─► FondoResultado (query)
                    └─► ControladorFondo mapea a RespuestaFondo
HTTP Response 200
```

---

## Responsabilidad de cada Capa

### Dominio
- Entidades de negocio con lógica encapsulada (`Cliente.descontarSaldo`)
- Interfaces de repositorios (`PuertoRepositorioX`) — define qué necesita, no cómo
- Interfaces de servicios (`IServicioX`) — contratos que infraestructura implementa
- Excepción de dominio (`ExcepcionDominio`)
- **No conoce** Spring, infraestructura, ni capa de aplicación

### Aplicación
- Patrón **CQRS** organizado por entidad de dominio
- `Manejador<T,R>` — interfaz genérica que todo handler implementa
- `Mediador` — resuelve automáticamente el handler según el tipo de solicitud enviada
- `FondoComando` / `FondoConsulta` — objetos de entrada
- `FondoResultado` — objetos de salida
- **No conoce** el modelo de dominio ni infraestructura

### Infraestructura
- **Única capa** que implementa interfaces y manipula el modelo de dominio
- `ServicioX` — contiene la lógica de negocio llamando repositorios
- `AdaptadorXEnMemoria` — implementación en memoria reemplazable por BD real
- **Conoce** dominio y aplicación

### API
- Controlador REST que recibe DTOs, invoca el `Mediador` y retorna respuesta HTTP
- `SolicitudSuscripcion` — DTO de entrada con validaciones Bean Validation
- `RespuestaX` — DTOs de salida
- `ManejadorExcepcionesGlobal` — traduce `ExcepcionDominio` a HTTP 422 y validaciones a HTTP 400
- **Solo conoce** el Mediador y los objetos de comando/consulta de la capa de aplicación

---

## Patrón Mediador

**"MediatR"** Permite al controlador enviar cualquier solicitud sin conocer qué handler la procesa.

```java
// Registro automático al arrancar Spring
Mediador
  ├── FondoComando.class  →  FondoManejador (command)
  └── FondoConsulta.class →  FondoManejador (query)

// En el controlador
mediador.enviar(new FondoComando(...));   // resuelve command handler
mediador.enviar(new FondoConsulta(...));  // resuelve query handler
```

Para agregar un nuevo caso de uso basta con crear un `@Component` que implemente `Manejador<T,R>`. El Mediador lo registra automáticamente sin modificar el controlador.

---

## Reglas de Negocio

| Regla | Implementación |
|---|---|
| Monto mínimo por fondo | `ServicioSuscripcion` valida `monto >= fondo.montoMinimo` |
| Saldo suficiente del cliente | `Cliente.descontarSaldo()` lanza `ExcepcionDominio` si no alcanza |
| Sin suscripciones duplicadas | `AdaptadorSuscripcionEnMemoria.existePorClienteIdYFondoId()` |

---

## Fondos Precargados

| ID | Nombre | Monto Mínimo | Categoría |
|---|---|---|---|
| fondo-1 | FPV_BTG_PACTUAL_RECAUDADORA | $75.000 | FPV |
| fondo-2 | FPV_BTG_PACTUAL_ECOPETROL | $125.000 | FPV |
| fondo-3 | DEUDAPRIVADA | $50.000 | FIC |
| fondo-4 | FDO-ACCIONES | $250.000 | FIC |
| fondo-5 | FPV_BTG_PACTUAL_DINAMICA | $100.000 | FPV |

## Clientes Precargados

| ID | Nombre | Saldo |
|---|---|---|
| cliente-1 | Juan Rodriguez | $500.000 |
| cliente-2 | Maria Lopez | $1.000.000 |

---

## Endpoints

| Método | URL | Descripción | HTTP |
|---|---|---|---|
| POST | `/api/fondos/suscribir` | Suscribir cliente a un fondo | 201 / 400 / 422 |
| GET | `/api/fondos/{fondoId}` | Consultar datos de un fondo | 200 / 422 |

### Ejemplo POST `/api/fondos/suscribir`

**Request:**
```json
{
  "clienteId": "cliente-1",
  "fondoId": "fondo-1",
  "monto": 75000
}
```

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
  "errores": ["clienteId: El clienteId es obligatorio"]
}
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
│                     API                         │
│  ControladorFondo → Mediador                    │
│  ControladorFondo → FondoComando / FondoConsulta│
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│                APPLICATION                      │
│  Mediador → FondoManejador (command/query)      │
│  FondoManejador → IServicioX (domain)           │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│                  DOMAIN                         │
│  IServicioX  (interfaces)                       │
│  PuertoRepositorioX  (interfaces)               │
│  Fondo, Cliente, Suscripcion  (modelos)         │
│  ExcepcionDominio                               │
└────────────────────┬────────────────────────────┘
                     │ implementa
┌────────────────────▼────────────────────────────┐
│               INFRASTRUCTURE                    │
│  ServicioX     implements IServicioX            │
│  AdaptadorXEnMemoria  implements PuertoX        │
└─────────────────────────────────────────────────┘

Regla: las flechas van hacia abajo. Ninguna capa
       inferior conoce a las capas superiores.
```
