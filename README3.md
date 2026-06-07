---

## Iteración 2 — Seguridad

> Se protegió la API REST de la App Móvil con autenticación, autorización y un límite de consultas en el endpoint de histórico.

---

### Qué se hizo

Se agregó seguridad a los endpoints que usa la App Móvil. Para poder usarlos, el cliente tiene que identificarse con su cédula y contraseña. Además, se puso un límite de consultas en el endpoint de histórico para que no se pueda abusar de él.

---

### Cómo funciona el login — Basic Auth

Cuando la App Móvil hace una consulta, manda en el encabezado del request la cédula y contraseña del cliente en formato codificado (Base64):

```
Authorization: Basic MTIzNDU2Nzg6Y2xhdmUxMjM=
```

Lo que viaja codificado es simplemente `cedula:contraseña`. WildFly decodifica eso y llama al `ClienteIdentityStore`, que busca al cliente en la base de datos y verifica la contraseña usando BCrypt.

#### Diagrama — qué pasa cuando se hace una consulta

```mermaid
sequenceDiagram
    participant App as App Móvil
    participant Auth as Jakarta Security
    participant IS as ClienteIdentityStore
    participant BD as Base de Datos
    participant API as CargaAPI

    App->>Auth: GET /historico\nAuthorization: Basic MTIz...
    Auth->>Auth: Decodifica Base64 → "12345678:clave123"
    Auth->>IS: validate(cedula, contrasena)
    IS->>BD: buscarPorCedula("12345678")
    BD-->>IS: Cliente encontrado
    IS->>IS: BCrypt.checkpw("clave123", hash) → true
    IS-->>Auth: CredentialValidationResult("12345678", ["CLIENTE"])
    Auth->>Auth: Verifica @RolesAllowed("CLIENTE") → OK
    Auth->>API: request autenticado
    API-->>App: 200 OK con datos
```

Si la cédula o contraseña son incorrectas, el servidor responde **401 Unauthorized** sin llegar al endpoint.

---

### Qué endpoints requieren login

Por defecto, todos los endpoints están bloqueados (`@DenyAll` en la clase). Cada método declara explícitamente si es público o requiere login.

| Endpoint | Acceso | Quién lo usa |
|----------|--------|-------|
| `POST /api/clientes/registrar` | Público | Cualquiera |
| `GET /api/clientes` | Público | Gestor Web |
| `POST /api/cargas/estaciones` | Público | Gestor Web |
| `GET /api/cargas/estaciones` | Público | Gestor Web |
| `POST /api/cargas/cargadores` | Público | Gestor Web |
| `POST /api/clientes/{cedula}/medioPago` | Requiere login | App Móvil |
| `POST /api/clientes/{cedula}/reclamos` | Requiere login | App Móvil |
| `POST /api/cargas/iniciar` | Requiere login | App Móvil |
| `POST /api/cargas/finalizar` | Requiere login | App Móvil |
| `GET /api/cargas/activa` | Requiere login | App Móvil |
| `GET /api/cargas/historico` | Requiere login + límite de consultas | App Móvil |
| `GET /api/pagos/{cedula}/listarPagos` | Requiere login | App Móvil |

---

### Verificación de que cada cliente solo accede a sus propios datos

No alcanza con verificar que el cliente esté logueado. También se verifica que solo pueda operar sobre sus propios datos. Si el cliente `12345678` intenta consultar o modificar datos del cliente `111111`, el servidor le responde **403 Forbidden**.

| Código | Qué significa |
|--------|-------------|
| `401 Unauthorized` | No se identificó o la contraseña es incorrecta |
| `403 Forbidden` | Está logueado pero está intentando acceder a datos de otro cliente u otro rol |

---

### Límite de consultas en `/historico`

El endpoint de histórico genera mucha carga en la base de datos, por eso se le puso un límite usando el algoritmo **Token Bucket** (balde de tokens):

```
Balde con 10 tokens al arrancar
├── Cada consulta consume 1 token
├── Si no hay tokens → 429 Too Many Requests
└── Se agregan 5 tokens por segundo
```

En la práctica el sistema deja pasar hasta 5 consultas por segundo de forma continua. Las primeras 10 pasan todas gracias a los tokens iniciales del balde.

#### Cómo se configuró la prueba en JMeter

**Arrivals Thread Group** — define cuántos requests por segundo manda JMeter:

![JMeter Arrivals Thread Group](./img/jmeter_thread_group.png)

| Parámetro | Valor | Qué significa |
|-----------|-------|-------------|
| Target Rate | 15 | 15 consultas por segundo |
| Ramp Up Time | 5 seg | Tarda 5 segundos en llegar a las 15 consultas/seg |
| Hold Target Rate Time | 30 seg | Mantiene esa carga durante 30 segundos |

**HTTP Request** — define a qué endpoint apunta cada consulta:

![JMeter HTTP Request](./img/http_request.png)

Apunta a `GET /TallerJavaEquipo6/api/cargas/historico` en `localhost:8080` con los parámetros `cedulaCliente=12345678`, `fechaIni=2026-01-01` y `fechaFin=2026-12-31`.

**HTTP Authorization Manager** — agrega las credenciales a cada consulta automáticamente:

![JMeter Authorization Manager](./img/autenticacion_manager.png)

Configura `username=12345678` y `password=clave123` para `http://localhost:8080`. JMeter las codifica en Base64 y las manda en el header `Authorization: Basic ...` de cada consulta, simulando exactamente lo que haría la App Móvil.

#### Resultado — gráfica Response Codes per Second

![Grafica JMeter Rate Limiter](./img/grafica_jmeter.png)

- **Línea roja (200):** consultas que llegaron al servidor y fueron respondidas — había lugar en el balde. Al principio la línea arranca más alta porque el balde empieza lleno con 10 tokens. Una vez que se gastan esos tokens iniciales, se estabiliza en ~5 por segundo, que es cuántos tokens se agregan por segundo.
- **Línea azul (429):** consultas bloqueadas — el balde estaba vacío.

La relación entre la configuración y el resultado de la grafica: JMeter manda 15 consultas por segundo, el límite deja pasar 5 (los que se recargan por segundo) y bloquea 10. Por eso rojo + azul = 15 en cada segundo — lo que cambia es cuántas pasan y cuántas son bloqueadas dependiendo de cuántos tokens haya en el balde en ese momento.
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

#### Verificación de acceso a datos propios — debe dar 403

```bash
# Cliente 12345678 intenta operar sobre datos de otro cliente
curl --user 12345678:clave123 -X POST http://localhost:8080/TallerJavaEquipo6/api/clientes/111111/medioPago -H "Content-Type: application/json" -d "{\"tipo\":\"TARJETA\",\"numero\":\"1234567890123456\",\"tipoTarjeta\":\"VISA\",\"fechaVencimiento\":\"2027-12-01\"}"
```
