# BTG Pactual V2 - Estándares de Código 📝

## 📋 **Información General**

### Propósito del Documento

Este documento define los estándares de código obligatorios y recomendados para el desarrollo en BTG Pactual V2. Estos estándares garantizan consistencia, legibilidad y mantenibilidad del código.

**Audiencia**: Desarrolladores, Code Reviewers  
**Última Actualización**: 12 de marzo de 2026  
**Estado**: Activo

---

## 🚨 **Estándares Obligatorios**

### 1. Nomenclatura

#### Variables y Funciones

Seguir convenciones Java (`camelCase`) con **nombres en español** para el dominio de negocio y **nombres explícitos** que revelen intención (Clean Code — "Meaningful Names").

```java
// ✅ CORRECTO — nombre revela intención
BigDecimal saldoDisponible = cliente.getSaldo();
void descontarSaldo(BigDecimal monto) { ... }
boolean existePorClienteIdYFondoId(String clienteId, String fondoId) { ... }

// ❌ INCORRECTO — abreviaciones, nombres genéricos
BigDecimal s = c.getSaldo();
void proc(BigDecimal m) { ... }
boolean check(String a, String b) { ... }
```

#### Clases y Componentes

`PascalCase` en español para dominio. Prefijos consistentes según rol arquitectónico:

| Rol | Prefijo/Convención | Ejemplo |
|---|---|---|
| Modelo de dominio | Nombre directo | `Cliente`, `Fondo`, `Suscripcion` |
| Enum de dominio | Nombre directo | `Estado`, `Rol` |
| Puerto de salida | `PuertoRepositorio*` / `Puerto*` | `PuertoRepositorioCliente`, `PuertoNotificacion` |
| Interfaz de servicio | `IServicio*` | `IServicioSuscripcion`, `IServicioFondo` |
| Implementación de servicio | `Servicio*` | `ServicioSuscripcion`, `ServicioFondo` |
| Adaptador de persistencia | `Adaptador*EnMemoria` / `Adaptador*CosmosDb` | `AdaptadorClienteEnMemoria` |
| Adaptador de notificación | `AdaptadorNotificacion*` | `AdaptadorNotificacionEmail` |
| Comando (CQRS — DTO de entrada) | `*Comando` | `FondoComando`, `RegistroComando`, `CancelacionComando` |
| Consulta (CQRS — DTO de entrada) | `*Consulta` | `FondoConsulta`, `SuscripcionesVigentesConsulta` |
| Resultado (CQRS — DTO de salida) | `*Resultado` | `FondoResultado`, `RegistroResultado`, `CancelacionResultado` |
| Manejador (CQRS) | `*Manejador` | `FondoManejador`, `RegistroManejador` |
| Controlador REST | `Controlador*` | `ControladorFondo`, `ControladorAutenticacion` |
| Excepción de dominio | `ExcepcionDominio` | Única excepción de dominio |

> **⚠️ IMPORTANTE:** Los DTOs de entrada y salida de la API son los **Comandos/Consultas y Resultados de la capa de aplicación**. NO existen DTOs separados en `api/dto/`. Los controladores reciben directamente `*Comando` o `*Consulta` como `@RequestBody` y retornan `*Resultado`. Esto permite que los manejadores CQRS tomen decisiones directamente sobre los objetos de entrada sin transformaciones intermedias.

```java
// ✅ CORRECTO
public class ServicioSuscripcion implements IServicioSuscripcion { ... }
public record FondoComando(String clienteId, String fondoId, BigDecimal monto) {}
public class AdaptadorClienteEnMemoria implements PuertoRepositorioCliente { ... }

// ❌ INCORRECTO
public class SuscripcionService implements ISuscripcionService { ... }  // mezcla idiomas
public class FondoCmd { ... }  // abreviación
public class ClienteRepo { ... }  // no sigue convención de adaptador
```

#### Archivos y Directorios

Estructura hexagonal con CQRS. Cada clase en su propio archivo `.java`:

```
src/main/java/btg_pactual_v1/btg_pactual_v2/
├── api/                                    ← Sin DTOs propios
│   ├── controller/
│   │   ├── ControladorFondo.java
│   │   └── ControladorAutenticacion.java
│   └── handler/
│       └── ManejadorExcepcionesGlobal.java
├── application/                            ← DTOs (Comandos/Consultas/Resultados) viven aquí
│   ├── fondo/
│   │   ├── command/
│   │   │   ├── FondoComando.java           ← DTO de entrada (antes SolicitudSuscripcion)
│   │   │   ├── FondoManejador.java
│   │   │   └── FondoResultado.java         ← DTO de salida (antes RespuestaSuscripcion)
│   │   └── query/
│   │       ├── FondoConsulta.java
│   │       ├── FondoManejador.java
│   │       └── FondoResultado.java
│   └── mediador/
│       ├── Manejador.java
│       └── Mediador.java
├── domain/
│   ├── exception/
│   │   └── ExcepcionDominio.java
│   ├── model/
│   │   ├── Cliente.java
│   │   ├── Fondo.java
│   │   └── Suscripcion.java
│   ├── port/out/
│   │   ├── PuertoRepositorioCliente.java
│   │   ├── PuertoRepositorioFondo.java
│   │   └── PuertoRepositorioSuscripcion.java
│   └── service/
│       ├── IServicioFondo.java
│       └── IServicioSuscripcion.java
└── infrastructure/
    ├── adapter/out/persistence/
    │   ├── AdaptadorClienteEnMemoria.java
    │   ├── AdaptadorFondoEnMemoria.java
    │   └── AdaptadorSuscripcionEnMemoria.java
    └── service/
        ├── ServicioFondo.java
        └── ServicioSuscripcion.java
```

### 2. Estructura de Código

#### Organización de Imports

Orden estricto, sin wildcards (`*`):

```java
// ✅ CORRECTO — Orden de imports
// 1. Paquetes del mismo proyecto (domain primero, luego application, luego infrastructure)
import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionDominio;
import btg_pactual_v1.btg_pactual_v2.domain.model.Cliente;
import btg_pactual_v1.btg_pactual_v2.domain.port.out.PuertoRepositorioCliente;

// 2. Frameworks (Spring, Jakarta)
import org.springframework.stereotype.Service;
import jakarta.validation.constraints.NotBlank;

// 3. Librerías Java estándar
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

// ❌ INCORRECTO
import java.util.*;  // wildcard prohibido
import btg_pactual_v1.btg_pactual_v2.domain.model.*;  // wildcard prohibido
```

#### Records para Comandos, Consultas, Resultados y Value Objects Inmutables

Todo Comando, Consulta y Resultado CQRS (que sirven como DTOs de entrada/salida de la API) **debe** ser un `record`. La capa de API NO tiene DTOs propios — los controladores reciben y retornan directamente estos records:

```java
// ✅ CORRECTO — record para comando CQRS (sirve como DTO de entrada de la API)
public record FondoComando(
        @NotBlank(message = "El clienteId es obligatorio")
        String clienteId,

        @NotBlank(message = "El fondoId es obligatorio")
        String fondoId,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal monto
) {}

// ✅ CORRECTO — record para resultado CQRS (sirve como DTO de salida de la API)
public record FondoResultado(
        String suscripcionId,
        String clienteId,
        String fondoId,
        BigDecimal monto,
        String estado,
        LocalDateTime fechaSuscripcion
) {}

// ❌ INCORRECTO — DTO separado en api/dto/ (no crear DTOs en capa API)
public class SolicitudSuscripcion {
    private String clienteId;
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public String getClienteId() { return clienteId; }
}
```

#### Inyección de Dependencias por Constructor

**Siempre** inyección por constructor. Nunca `@Autowired` en campo.

```java
// ✅ CORRECTO
@Service
public class ServicioSuscripcion implements IServicioSuscripcion {

    private final PuertoRepositorioFondo repositorioFondo;
    private final PuertoRepositorioCliente repositorioCliente;
    private final PuertoRepositorioSuscripcion repositorioSuscripcion;

    public ServicioSuscripcion(PuertoRepositorioFondo repositorioFondo,
                               PuertoRepositorioCliente repositorioCliente,
                               PuertoRepositorioSuscripcion repositorioSuscripcion) {
        this.repositorioFondo = repositorioFondo;
        this.repositorioCliente = repositorioCliente;
        this.repositorioSuscripcion = repositorioSuscripcion;
    }
}

// ❌ INCORRECTO
@Service
public class ServicioSuscripcion {
    @Autowired
    private PuertoRepositorioFondo repositorioFondo;  // inyección por campo prohibida
}
```

#### BigDecimal para Valores Monetarios

**Nunca** usar `double`, `float` ni `int` para representar dinero.

```java
// ✅ CORRECTO
private BigDecimal saldo;
public void descontarSaldo(BigDecimal monto) {
    if (this.saldo.compareTo(monto) < 0) {
        throw new ExcepcionDominio("Saldo insuficiente");
    }
    this.saldo = this.saldo.subtract(monto);
}

// ❌ INCORRECTO
private double saldo;
public void descontarSaldo(double monto) {
    this.saldo -= monto;  // pérdida de precisión
}
```

### 3. Manejo de Errores

#### Excepción Única de Dominio

Toda violación de regla de negocio lanza `ExcepcionDominio`. No crear sub-excepciones por cada caso.

```java
// ✅ CORRECTO — excepción con mensaje descriptivo
if (comando.monto().compareTo(fondo.getMontoMinimo()) < 0) {
    throw new ExcepcionDominio(
        "El monto mínimo para vincularse al fondo " + fondo.getNombre()
        + " es $" + fondo.getMontoMinimo()
    );
}

// ❌ INCORRECTO — excepción genérica o personalizada innecesaria
throw new RuntimeException("Error");  // genérica
throw new MontoInsuficienteException(monto);  // sub-excepción innecesaria
```

#### Manejo Global de Excepciones

`ManejadorExcepcionesGlobal` centraliza la traducción de excepciones a respuestas HTTP. Los controladores **nunca** hacen try/catch.

```java
// ✅ CORRECTO — el controlador recibe el Comando directamente y deja que la excepción fluya
@PostMapping("/suscribir")
public ResponseEntity<FondoResultado> suscribir(@RequestBody @Valid FondoComando comando) {
    FondoResultado resultado = mediador.enviar(comando);
    return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
}

// ❌ INCORRECTO — try/catch en controlador
@PostMapping("/suscribir")
public ResponseEntity<?> suscribir(@RequestBody FondoComando comando) {
    try {
        // lógica
    } catch (ExcepcionDominio e) {
        return ResponseEntity.status(422).body(Map.of("error", e.getMessage()));
    }
}
```

### 4. Restricciones Arquitectónicas de Codificación

#### Regla de Dependencias entre Capas

Las dependencias **solo** fluyen hacia abajo. Ninguna capa inferior puede importar clases de capas superiores.

```
SECURITY → API → APPLICATION → DOMAIN ← INFRASTRUCTURE
```

```java
// ✅ CORRECTO — Dominio no importa nada externo
package btg_pactual_v1.btg_pactual_v2.domain.model;

import btg_pactual_v1.btg_pactual_v2.domain.exception.ExcepcionDominio;
import java.math.BigDecimal;

public class Cliente { ... }

// ❌ INCORRECTO — Dominio importa Application
package btg_pactual_v1.btg_pactual_v2.domain.service;

import btg_pactual_v1.btg_pactual_v2.application.fondo.command.FondoComando;  // ¡VIOLACIÓN!

public interface IServicioSuscripcion {
    FondoResultado suscribir(FondoComando comando);  // dominio depende de aplicación
}
```

**Imports permitidos por capa:**

| Capa | Puede importar de |
|---|---|
| `domain/` | Solo `java.*`, `jakarta.validation.*` (anotaciones), otros paquetes de `domain/` |
| `application/` | `domain/` + `java.*` |
| `infrastructure/` | `domain/` + `application/` + `java.*` + frameworks (Spring, SDK externos) |
| `api/` | `application/` + `java.*` + Spring Web + Jakarta Validation |

#### CQRS — Separación Estricta Command / Query

Cada caso de uso tiene su propio paquete `command/` o `query/` con 3 archivos:

```
application/
└── {entidad}/
    ├── command/
    │   ├── {Entidad}Comando.java      // record de entrada
    │   ├── {Entidad}Manejador.java    // @Component handler
    │   └── {Entidad}Resultado.java    // record de salida
    └── query/
        ├── {Entidad}Consulta.java
        ├── {Entidad}Manejador.java
        └── {Entidad}Resultado.java
```

Los resultados de command y query pueden tener campos diferentes. No reutilizar el mismo `Resultado` entre ambos.

#### Patrón Strategy — Uso Obligatorio

Cuando existen múltiples variantes de un comportamiento, usar patrón Strategy para evitar cadenas de `if/else` o `switch`:

```java
// ✅ CORRECTO — Strategy para notificaciones (HU 2.4)
public interface PuertoNotificacion {
    void enviar(String destinatario, String mensaje);
}

@Component
public class AdaptadorNotificacionEmail implements PuertoNotificacion {
    @Override
    public void enviar(String destinatario, String mensaje) { /* email */ }
}

@Component
public class AdaptadorNotificacionSms implements PuertoNotificacion {
    @Override
    public void enviar(String destinatario, String mensaje) { /* SMS */ }
}

// ❌ INCORRECTO — if/else para seleccionar estrategia
public void notificar(String tipo, String destinatario, String mensaje) {
    if ("EMAIL".equals(tipo)) { /* enviar email */ }
    else if ("SMS".equals(tipo)) { /* enviar SMS */ }
    else if ("PUSH".equals(tipo)) { /* enviar push */ }
}
```

#### Modelos de Dominio con Lógica Encapsulada

Los modelos de dominio **no son anémicos**. Deben contener lógica de negocio relevante:

```java
// ✅ CORRECTO — lógica de negocio en el modelo
public class Cliente {
    private BigDecimal saldo;

    public void descontarSaldo(BigDecimal monto) {
        if (this.saldo.compareTo(monto) < 0) {
            throw new ExcepcionDominio("Saldo insuficiente");
        }
        this.saldo = this.saldo.subtract(monto);
    }

    public void abonarSaldo(BigDecimal monto) {
        this.saldo = this.saldo.add(monto);
    }
}

// ❌ INCORRECTO — modelo anémico, lógica en servicio
public class Cliente {
    private BigDecimal saldo;
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
}

// Servicio hace la lógica que debería estar en el modelo
if (cliente.getSaldo().compareTo(monto) < 0) { throw ... }
cliente.setSaldo(cliente.getSaldo().subtract(monto));
```

### 5. Pruebas Unitarias Obligatorias

#### Uso obligatorio de Test Data Builder y patrón 3A (Arrange-Act-Assert)

Todas las pruebas deben seguir el patrón **3A** con comentarios explícitos y usar **Test Data Builder** para construir datos de prueba.

```java
// ✅ OBLIGATORIO — Test Data Builder + Patrón 3A
@Test
@DisplayName("201 - suscripción exitosa con datos válidos")
void suscripcionExitosa() throws Exception {
    // Arrange
    String cuerpo = new SolicitudSuscripcionBuilder()
            .conClienteId("cliente-1")
            .conFondoId("fondo-1")
            .conMonto(new BigDecimal("75000"))
            .buildJson();

    // Act
    ResultActions resultado = mockMvc.perform(post("/api/fondos/suscribir")
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo));

    // Assert
    resultado
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.suscripcionId").isNotEmpty())
            .andExpect(jsonPath("$.estado").value("ACTIVO"));
}

// ❌ INCORRECTO — sin 3A, sin builder, datos inline
@Test
void test1() throws Exception {
    mockMvc.perform(post("/api/fondos/suscribir")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"clienteId\":\"c1\",\"fondoId\":\"f1\",\"monto\":75000}"))
            .andExpect(status().isCreated());
}
```

#### Estructura del Test Data Builder

```java
public class SolicitudSuscripcionBuilder {

    private String clienteId = "cliente-1";
    private String fondoId = "fondo-1";
    private BigDecimal monto = new BigDecimal("75000");

    public SolicitudSuscripcionBuilder conClienteId(String clienteId) {
        this.clienteId = clienteId;
        return this;
    }

    public SolicitudSuscripcionBuilder conFondoId(String fondoId) {
        this.fondoId = fondoId;
        return this;
    }

    public SolicitudSuscripcionBuilder conMonto(BigDecimal monto) {
        this.monto = monto;
        return this;
    }

    public SolicitudSuscripcion build() {
        return new SolicitudSuscripcion(clienteId, fondoId, monto);
    }

    public String buildJson() throws Exception {
        return new ObjectMapper().writeValueAsString(
            Map.of("clienteId", clienteId, "fondoId", fondoId, "monto", monto)
        );
    }
}
```

#### Convenciones de Nombres en Tests

| Elemento | Convención | Ejemplo |
|---|---|---|
| Clase de test | `{Clase}Test` o `{Controlador}E2ETest` | `ControladorFondoE2ETest` |
| `@DisplayName` | Código HTTP + descripción en español | `"201 - suscripción exitosa con datos válidos"` |
| `@Nested` | Agrupa por endpoint o método | `@Nested class SuscribirFondo` |
| Nombre del método | `camelCase` descriptivo | `suscripcionExitosa()`, `saldoInsuficiente()` |
| Builder | `{Entidad}Builder` | `SolicitudSuscripcionBuilder`, `ClienteBuilder` |

#### Tests E2E: Reset de Estado

Los adaptadores en memoria exponen `reiniciar()`. Usar `@BeforeEach` para garantizar aislamiento:

```java
@BeforeEach
void reiniciarEstado() {
    adaptadorCliente.reiniciar();
    adaptadorSuscripcion.reiniciar();
}
```

---

## 💡 **Convenciones Recomendadas**

### 1. Documentación de API con OpenAPI

Anotar controladores con `@Tag` y endpoints con `@Operation`:

```java
@RestController
@RequestMapping("/api/fondos")
@Tag(name = "Fondos", description = "Operaciones sobre fondos BTG Pactual")
public class ControladorFondo {

    @PostMapping("/suscribir")
    @Operation(summary = "Suscribir cliente a un fondo (Command)")
    public ResponseEntity<RespuestaSuscripcion> suscribir(...) { ... }
}
```

### 2. Adaptadores en Memoria Thread-Safe

Usar `ConcurrentHashMap` para adaptadores en memoria:

```java
@Component
public class AdaptadorFondoEnMemoria implements PuertoRepositorioFondo {
    private final Map<String, Fondo> almacen = new ConcurrentHashMap<>();
}
```

### 3. Mediador con Registro Automático

Nuevos handlers se registran automáticamente. Solo crear la clase `@Component` que implemente `Manejador<T,R>`:

```java
@Component("cancelacionManejador")
public class CancelacionManejador implements Manejador<CancelacionComando, CancelacionResultado> {

    @Override
    public CancelacionResultado manejar(CancelacionComando comando) { ... }

    @Override
    public Class<CancelacionComando> tipoDeSolicitud() {
        return CancelacionComando.class;
    }
}
```

### 4. Bean Validation en DTOs de Entrada

Validar en el DTO, no en el servicio. Mensajes en español:

```java
public record SolicitudRegistro(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato del email no es válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 5, message = "La contraseña debe tener al menos 5 caracteres")
        String contrasena
) {}
```

### 5. Controladores Delgados

El controlador solo mapea DTO → Comando/Consulta, invoca el Mediador, y mapea Resultado → Respuesta. **No contiene lógica de negocio**.

```java
// ✅ CORRECTO — controlador delgado
@PostMapping("/suscribir")
public ResponseEntity<RespuestaSuscripcion> suscribir(@RequestBody @Valid SolicitudSuscripcion solicitud) {
    FondoResultado resultado = mediador.enviar(
            new FondoComando(solicitud.clienteId(), solicitud.fondoId(), solicitud.monto())
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(new RespuestaSuscripcion(
            resultado.suscripcionId(), resultado.clienteId(), resultado.fondoId(),
            resultado.monto(), resultado.estado(), resultado.fechaSuscripcion()
    ));
}
```

---

## 🔧 **Configuración de Herramientas**

### Build Tool — Gradle (Groovy DSL)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.3'
    id 'io.spring.dependency-management' version '1.1.7'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2'
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### Formatter / Linter

No hay linter configurado actualmente. Se recomienda agregar **Checkstyle** con las siguientes reglas mínimas:

```xml
<!-- checkstyle.xml (recomendado) -->
<module name="Checker">
    <module name="TreeWalker">
        <module name="AvoidStarImport"/>           <!-- sin wildcards -->
        <module name="UnusedImports"/>              <!-- imports limpios -->
        <module name="FinalLocalVariable"/>         <!-- inmutabilidad -->
        <module name="NeedBraces"/>                 <!-- siempre llaves -->
        <module name="OneTopLevelClass"/>           <!-- una clase por archivo -->
    </module>
</module>
```

### Scripts de Ejecución

```bash
# Compilar
./gradlew build

# Ejecutar tests
./gradlew test

# Ejecutar aplicación
./gradlew bootRun

# Ver reporte de tests
# build/reports/tests/test/index.html

# Swagger UI (en ejecución)
# http://localhost:8080/swagger-ui.html
```

---

## 🚀 **Mejores Prácticas Específicas**

### Java 24

- Usar `record` para clases inmutables (DTOs, comandos, consultas, resultados)
- Usar `Optional<T>` como retorno de búsquedas (nunca como parámetro)
- Usar `var` solo cuando el tipo es obvio por el lado derecho de la asignación
- Usar text blocks (`"""`) para strings multilinea
- Usar `sealed` interfaces cuando las implementaciones son finitas y conocidas

```java
// ✅ records, Optional, text blocks
public record FondoConsulta(String fondoId) {}
Optional<Fondo> buscarPorId(String id);

// ✅ var cuando el tipo es obvio
var almacen = new ConcurrentHashMap<String, Fondo>();

// ❌ var cuando el tipo no es obvio
var resultado = mediador.enviar(comando);  // ¿qué tipo retorna?
```

### Spring Boot 4.0.3

- Usar `@RestControllerAdvice` para manejo global de excepciones
- Usar `@Valid` en parámetros de controlador para activar Bean Validation
- Inyección por constructor (Spring lo autodetecta con un solo constructor)
- `@Component` con nombre explícito cuando hay múltiples implementaciones del mismo contrato

```java
@Component("fondoComandoManejador")  // nombre explícito para desambiguar
public class FondoManejador implements Manejador<FondoComando, FondoResultado> { ... }
```

---

## 📚 **Referencias y Recursos**

### Documentación Oficial

- [Java Language Specification (JLS 24)](https://docs.oracle.com/en/java/javase/24/)
- [Spring Boot 4.x Reference](https://docs.spring.io/spring-boot/reference/)
- [Jakarta Bean Validation 3.1](https://jakarta.ee/specifications/bean-validation/)
- [SpringDoc OpenAPI](https://springdoc.org/)

### Principios de Diseño

- **Clean Code** (Robert C. Martin) — Nombres significativos, funciones pequeñas, SRP
- **Hexagonal Architecture** (Alistair Cockburn) — Ports & Adapters
- **CQRS** (Greg Young) — Separación de comandos y consultas
- **Strategy Pattern** (GoF) — Intercambio de algoritmos/comportamientos en runtime

### Herramientas

- **Build**: Gradle 9.3.1 (Groovy DSL)
- **Testing**: JUnit 5 + MockMvc
- **API Docs**: SpringDoc OpenAPI 3.0.2 + Swagger UI
- **Linting**: N/A (recomendado: Checkstyle)

---

**NOTA**: Estos estándares fueron generados analizando el código base existente y las prácticas del equipo. Deben mantenerse alineados con las convenciones reales de codificación del proyecto.

---

_Documento generado con Método Ceiba - Arquitecto_  
_Última actualización: 12 de marzo de 2026_  
_Versión: 1.0_
