---

## Iteración 2 — Seguridad

> Autenticación, autorización y rate limiter sobre la API REST de la App Móvil.

---

### Qué se implementó

Se protegieron los endpoints de la App Móvil usando **Jakarta Security con Basic Auth**. Solo los clientes registrados pueden acceder a sus propios datos. El endpoint de histórico de cargas tiene además un **rate limiter** basado en Token Bucket (Bucket4j) para evitar abuso de un endpoint costoso en recursos.

---

### Autenticación — Basic Auth

Basic Auth es el mecanismo de autenticación HTTP más simple. La App Móvil incluye en cada request un header con la cédula y contraseña del cliente codificadas en Base64:

```
Authorization: Basic MTIzNDU2Nzg6Y2xhdmUxMjM=
```

Lo que viaja codificado es simplemente `cedula:contraseña`. WildFly decodifica ese header y llama al `ClienteIdentityStore`, que busca el cliente en la base de datos y verifica la contraseña con BCrypt.

#### Diagrama de secuencia

```mermaid
sequenceDiagram
    participant App as App Móvil
    participant WF as WildFly
    participant IS as ClienteIdentityStore
    participant BD as Base de Datos

    App->>WF: GET /historico\nAuthorization: Basic MTIz...
    WF->>WF: Decodifica Base64 → "12345678:clave123"
    WF->>IS: validate(cedula, contrasena)
    IS->>BD: buscarPorCedula("12345678")
    BD-->>IS: Cliente encontrado
    IS->>IS: BCrypt.checkpw("clave123", hash) → true
    IS-->>WF: CredentialValidationResult("12345678", ["CLIENTE"])
    WF->>WF: Verifica @RolesAllowed("CLIENTE") → OK
    WF-->>App: 200 OK
```

Si las credenciales son incorrectas o el cliente no existe, WildFly responde **401 Unauthorized** sin llegar al endpoint.

> El `ClienteIdentityStore` inyecta `InterfaceLocalCliente` en lugar del repositorio directamente, respetando la arquitectura modular: los módulos externos no acceden a la infraestructura interna de otro módulo, sino a través de las interfaces que este expone.

---

### Endpoints — tabla de acceso

Todos los controladores tienen `@DenyAll` a nivel de clase. Cada método declara explícitamente quién puede acceder.

| Endpoint | Acceso | Actor |
|----------|--------|-------|
| `POST /api/clientes/registrar` | Público | Cualquiera |
| `GET /api/clientes` | Público | Gestor Web |
| `POST /api/cargas/estaciones` | Público | Gestor Web |
| `GET /api/cargas/estaciones` | Público | Gestor Web |
| `POST /api/cargas/cargadores` | Público | Gestor Web |
| `POST /api/clientes/{cedula}/medioPago` | `CLIENTE` | App Móvil |
| `POST /api/clientes/{cedula}/reclamos` | `CLIENTE` | App Móvil |
| `POST /api/cargas/iniciar` | `CLIENTE` | App Móvil |
| `POST /api/cargas/finalizar` | `CLIENTE` | App Móvil |
| `GET /api/cargas/activa` | `CLIENTE` | App Móvil |
| `GET /api/cargas/historico` | `CLIENTE` + Rate Limiter | App Móvil |
| `GET /api/pagos/{cedula}/listarPagos` | `CLIENTE` | App Móvil |

---

### Autorización a nivel de recurso

Además de verificar que el usuario está autenticado, se verifica que solo pueda operar sobre sus propios datos. Un cliente autenticado como `12345678` que intente acceder a datos de `111111` recibe **403 Forbidden**.

| Código | Significado |
|--------|-------------|
| `401 Unauthorized` | No autenticado o credenciales incorrectas |
| `403 Forbidden` | Autenticado pero intentando operar sobre datos de otro cliente |

---

### Rate Limiter — `/historico`

El endpoint de histórico está protegido por un rate limiter basado en el algoritmo **Token Bucket**:

```
Balde con capacidad inicial de 10 tokens
├── Cada request consume 1 token
├── Si el balde está vacío → 429 Too Many Requests
└── Se recargan 5 tokens por segundo
```

En la práctica el sistema acepta hasta 5 requests por segundo de forma sostenida. Los primeros 10 requests pasan todos gracias a la capacidad inicial del balde.

#### Resultado de prueba con JMeter

JMeter configurado a 15 requests por segundo contra `/historico` con credenciales válidas:

![Grafica JMeter Rate Limiter](./jmeter_rate_limiter.png)

- **Línea roja (200):** requests procesados correctamente — había token disponible. Se estabiliza en ~5 por segundo, que es la tasa de recarga configurada.
- **Línea azul (429):** requests bloqueados por el rate limiter — balde vacío.

La clave para leer el gráfico: **rojo + azul ≈ 15** en cada segundo, porque JMeter siempre envía 15 requests. Lo que varía es cuántos pasan y cuántos son bloqueados según los tokens disponibles.

---

### Pruebas con curl

#### Endpoints públicos

```bash
# Registrar cliente
curl -X POST http://localhost:8080/TallerJavaEquipo6/api/clientes/registrar -H "Content-Type: application/json" -d "{\"cedula\":\"12345678\",\"nombreCompleto\":\"Juan Perez\",\"telefono\":\"099123456\",\"contrasena\":\"clave123\",\"tipo\":\"COMUN\"}"

# Ver clientes
curl http://localhost:8080/TallerJavaEquipo6/api/clientes

# Crear estacion y cargador
curl -X POST http://localhost:8080/TallerJavaEquipo6/api/cargas/estaciones -H "Content-Type: application/json" -d "{\"descripcion\":\"Estacion Centro\",\"calle\":\"18 de Julio\",\"departamento\":\"Montevideo\",\"longitud\":-34,\"latitud\":-56}"
curl -X POST http://localhost:8080/TallerJavaEquipo6/api/cargas/cargadores -H "Content-Type: application/json" -d "{\"idEstacion\":1,\"tipo\":\"RAPIDO\",\"tieneCable\":true,\"tipoConector\":\"TIPO2\",\"potenciaMinima\":22}"
```

#### Sin credenciales — deben dar 401

```bash
curl "http://localhost:8080/TallerJavaEquipo6/api/cargas/historico?cedulaCliente=12345678&fechaIni=2026-01-01&fechaFin=2026-12-31"
curl -X POST http://localhost:8080/TallerJavaEquipo6/api/cargas/iniciar -H "Content-Type: application/json" -d "{\"cedulaCliente\":\"12345678\",\"idCargador\":1,\"idMedioPago\":1}"
```

#### Con credenciales correctas

```bash
# Agregar medio de pago
curl --user 12345678:clave123 -X POST http://localhost:8080/TallerJavaEquipo6/api/clientes/12345678/medioPago -H "Content-Type: application/json" -d "{\"tipo\":\"TARJETA\",\"numero\":\"1234567890123456\",\"tipoTarjeta\":\"VISA\",\"fechaVencimiento\":\"2027-12-01\"}"

# Iniciar y ver carga activa
curl --user 12345678:clave123 -X POST http://localhost:8080/TallerJavaEquipo6/api/cargas/iniciar -H "Content-Type: application/json" -d "{\"cedulaCliente\":\"12345678\",\"idCargador\":1,\"idMedioPago\":1}"
curl --user 12345678:clave123 "http://localhost:8080/TallerJavaEquipo6/api/cargas/activa?cedulaCliente=12345678"

# Ver historico y pagos
curl --user 12345678:clave123 "http://localhost:8080/TallerJavaEquipo6/api/cargas/historico?cedulaCliente=12345678&fechaIni=2026-01-01&fechaFin=2026-12-31"
curl --user 12345678:clave123 "http://localhost:8080/TallerJavaEquipo6/api/pagos/12345678/listarPagos?fechaIni=2026-01-01&fechaFin=2026-12-31"
```

#### Autorización a nivel de recurso — debe dar 403

```bash
# Cliente 12345678 intenta operar sobre datos de otro cliente
curl --user 12345678:clave123 -X POST http://localhost:8080/TallerJavaEquipo6/api/clientes/111111/medioPago -H "Content-Type: application/json" -d "{\"tipo\":\"TARJETA\",\"numero\":\"1234567890123456\",\"tipoTarjeta\":\"VISA\",\"fechaVencimiento\":\"2027-12-01\"}"
```
