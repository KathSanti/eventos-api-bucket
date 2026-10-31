# Eventos API — cambios y cómo ejecutarla

## 1. Cómo ejecutar (desarrollo)

1. Abre en VS Code **la carpeta `eventos-api-bucket`** (no una carpeta superior),
   para que la aplicación encuentre el archivo `.env`.
2. En la Terminal, dentro de esa carpeta:
   ```bash
   ./mvnw clean compile        # primero: tiene que decir BUILD SUCCESS
   ./mvnw spring-boot:run      # arranca en http://localhost:8080 con el perfil dev
   ```
   (o el botón **Run** sobre `EventosApiBucketApplication` en VS Code).
3. Al arrancar se crean solos:
   - el **administrador**, con el email y la contraseña que están en `.env`;
   - **3 eventos de ejemplo** (solo en el perfil dev).

> `.env` tiene secretos **solo de desarrollo** y está en `.gitignore`, así que no se sube a GitHub.
> Si lo borras, copia `.env.example` como `.env` y genera una clave nueva con `openssl rand -base64 32`.

## 2. Probar en Postman

1. Postman → **Import** → `postman/eventos-api.postman_collection.json`.
2. En la colección → pestaña **Variables** → pega en `adminPassword` el valor `ADMIN_PASSWORD` de tu `.env`.
3. Ejecuta las carpetas en orden (1 → 5). Los tokens se guardan solos en `{{token}}` y `{{tokenAdmin}}`,
   y cada request tiene un test que verifica el código esperado.

| Prueba | Esperado |
|---|---|
| GET `/api/eventos` sin token | 200 |
| Registro / login | 201 / 200 + token |
| `/api/clientes/me` sin token | **401** |
| `/api/clientes/me` con token | 200 |
| Token con una letra cambiada | **401** (la firma no coincide) |
| Crear evento como CLIENTE / como ADMIN | **403** / 201 |
| Comprar como CLIENTE / como ADMIN | 201 / **403** |
| 5 contraseñas incorrectas seguidas, y luego un 6.º intento | Los 5 primeros dan 401; el 6.º da **423** (cuenta bloqueada 10 min, incluso con la clave correcta) |

Para probar la expiración: cambia `jwt.expiration-ms=60000` (1 minuto), reinicia, haz login, espera 1 minuto → 401.

---

## 3. Qué se cambió y por qué

### Fase 0 — Que compile y arranque
| Archivo | Cambio | Por qué |
|---|---|---|
| `model/Evento` | `BigDeimal` → `BigDecimal`; `disponible = true` por defecto | Error de tipeo; los eventos nuevos quedaban no disponibles |
| `model/Compra` | `Interger`, `LocalDataTime` corregidos; `precio_total` y `evento_id` como columnas propias; `Id` → `id`; borrado el import `ru.INN` | Dos atributos no pueden apuntar a la misma columna |
| `model/Rol` | Un solo nombre: `ADMIN` (también en `@PreAuthorize` y `AdminBootstrap`) | `hasRole('ADMINISTRADOR')` compilaba, pero nadie podía crear eventos |
| `model/Cliente` | `Rol rol` con `@Enumerated(EnumType.STRING)`; `getAuthorities()` con `@Override`; `email` único | ORDINAL guarda 0/1 y es frágil; Spring solo llama al método del contrato |
| DTOs | `EventoDto` → `EventoResponse`; nuevos `NuevoEventoRequest`, `CompraRequest` y `ClienteAdminResponse`; `CompraResponse` unificado (se borraron `CompraResponseDTO` y `EventoDetalle`) | Una convención: `XxxRequest` entra y `XxxResponse` sale |
| `repository/*` | `ffindBy` → `findBy`; `existsByRol`, `existsByNombreIgnoreCase`; `@EntityGraph` en el historial | La BD filtra y no se carga todo en memoria |
| `security/ClienteRepository` | **Borrado** | Clase vacía con el mismo nombre que el repositorio real |
| `service/JwtService` | `java.util.Date` y el `@Value` de Spring; imports que faltaban | El autoimport eligió las clases equivocadas |
| `application.properties` | Comentarios con `#`; claves `app.admin.*` iguales a las del código | `//` no es comentario; las claves deben coincidir exactas |

### Fase 1 — JWT bien conectado
| Archivo | Cambio | Por qué |
|---|---|---|
| `SecurityConfig` | `.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)` | **El bug principal**: el filtro corría fuera de la cadena de seguridad y el token nunca se leía |
| `JwtAuthFilter` | Ya no es `@Component`; se crea en `SecurityConfig` | Así no queda registrado dos veces |
| `ManejadorErroresSeguridad` (nuevo) | Responde 401 y 403 en JSON | Los rechazos en filtros no llegan a `GlobalExceptionHandler` |
| `AuthService.login` | Llama a `registrarIntentoFallido` y `reiniciarIntentos` | El bloqueo estaba escrito pero nunca se usaba |
| `GlobalExceptionHandler` | `LockedException` (423), `DisabledException`, JSON inválido, error genérico 500 sin detalles | Mensajes claros sin filtrar información interna |
| `JwtService` | Claims `iss` y `aud` que se exigen al validar | Lo pide la materia: un token de otro sistema no sirve |
| `SecurityConfig` | CORS con orígenes leídos desde las properties | El frontend en otro puerto/dominio necesita permiso explícito |
| `AuthService` | Email normalizado (minúsculas, sin espacios) | `Ana@Mail.com` y `ana@mail.com` son la misma cuenta |

### Fase 2 — Entornos (perfiles)
- `application.properties`: lo común. Sin secretos, solo `${VARIABLES}`.
- `application-dev.properties`: H2 en memoria, `create-drop`, CORS para localhost.
- `application-prod.properties`: preparado para EC2 (MySQL y CORS desde variables de entorno). **Aún no se usa.**
- `src/test/resources/application-test.properties` y `@ActiveProfiles("test")`: el test `contextLoads` ya no depende de tu `.env`.

### Fase 3 — Backend completo
- `POST /api/compras`: el cliente sale del token, el precio sale de la BD y se valida que el evento esté disponible.
- `GET /api/clientes` (admin): lista de clientes, sin contraseñas.
- `PATCH /api/clientes/{id}/desbloquear` (admin).
- `config/DatosDevLoader`: eventos de ejemplo solo en dev.

## 4. Endpoints

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/auth/registro` | Público |
| POST | `/api/auth/login` | Público |
| GET | `/api/eventos` | Público |
| GET | `/api/eventos/{nombre}` | Público |
| POST | `/api/eventos` | ADMIN |
| GET | `/api/clientes/me` | Autenticado |
| GET | `/api/clientes/me/compras` | Autenticado |
| POST | `/api/compras` | CLIENTE |
| GET | `/api/clientes` | ADMIN |
| PATCH | `/api/clientes/{id}/desbloquear` | ADMIN |
| *cualquier otra* | — | Denegada |

## 5. Siguiente paso: producción (EC2)
Cuando todo lo anterior funcione, en el servidor se definen estas variables de entorno:
`SPRING_PROFILES_ACTIVE=prod`, `JWT_SECRET` (**otra** clave), `ADMIN_EMAIL`, `ADMIN_PASSWORD`,
`DB_URL`, `DB_USER`, `DB_PASSWORD` y `CORS_ORIGINS`. El mismo `.jar` corre en ambos entornos;
lo único que cambia es la configuración.

---

# Semana 2 — Resumen de compra en AWS S3 + Docker + CI/CD

## 6. Antes de probar en local

1. En AWS Academy: **Start Lab** → **AWS Details** → **AWS CLI** y copia los 3 valores.
2. Pégalos en tu `.env` junto con el nombre de tu bucket:
   ```
   S3_BUCKET=nombre-de-tu-bucket
   AWS_ACCESS_KEY_ID=...
   AWS_SECRET_ACCESS_KEY=...
   AWS_SESSION_TOKEN=...
   ```
   Cambian **cada vez** que inicias el laboratorio. Si ves un **502** en los endpoints de S3, casi siempre es eso.
3. `./mvnw spring-boot:run` (perfil dev con H2), o `docker compose up --build` (perfil prod con MySQL).

## 7. Endpoints nuevos

| Método | Ruta | Qué hace | Acceso |
|---|---|---|---|
| GET | `/api/compras/{numero}/resumen` | Genera el resumen y lo descarga como archivo (.txt) | Dueño o ADMIN |
| POST | `/api/compras/{numero}/resumen/s3` | Genera el resumen y lo sube a S3 en la carpeta `{numero}/` | Dueño o ADMIN |
| PUT | `/api/compras/{numero}/resumen/s3` | **Modificar**: reemplaza el archivo por uno corregido (form-data `file`) | Dueño o ADMIN |
| GET | `/api/compras/{numero}/resumen/s3` | **Descargar** desde S3 | Dueño o ADMIN |
| DELETE | `/api/compras/{numero}/resumen/s3` | **Borrar** de S3 | Dueño o ADMIN |

`{numero}` es el `numeroResumen` que ahora devuelve `POST /api/compras` (es el id de la compra).
En S3 queda como `15/resumen-compra-15.txt`: la "carpeta" es el número del resumen.

En Postman, carpeta **6**: primero ejecuta "3 › Comprar entradas", que guarda `{{numeroResumen}}`.
Para guardar el archivo: flecha junto a **Send** → **Send and Download**.

## 8. Clases nuevas (capas como en la materia)

| Clase | Responsabilidad |
|---|---|
| `config/StorageConfig` | Credenciales de AWS Academy (3 partes, con session token). Spring Cloud AWS usa este bean en vez del suyo. |
| `model/Asset` | Datos de un archivo en S3 (clave, carpeta, nombre, tipo, tamaño). Inmutable con `lombok.Value`. |
| `repository/S3Repository` + `S3RepositoryImpl` | La **única** capa que habla con S3 (`S3Template`): subir, descargar, existe, eliminar. El bucket viene de la configuración. |
| `service/ResumenCompraService` | Genera el texto del resumen y valida que el usuario sea **dueño o ADMIN** (si no, 404). |
| `service/AwsService` + `AwsServiceImpl` | Orquesta: valida el permiso → arma la clave `{numero}/...` → llama al repositorio. Valida el archivo del PUT (.txt, no vacío, máx. 1 MB). |
| `controller/AwsController` | Endpoints de S3. `CompraController` suma el de generar el archivo local. |
| `GlobalExceptionHandler` | Agrega 400 (archivo inválido o faltante), 413 (muy grande) y 502 (error de AWS: token vencido, bucket inexistente). |

**¿Por qué no `/s3/uploadFile?bucketname=...&filepath=...` como en la guía?** Ahí el cliente elige el bucket y la ruta,
y cualquier usuario podría sobrescribir o borrar el resumen de otro. Aquí el bucket es fijo (configuración) y la clave
la arma el servidor a partir del número.

**Recomendación para el bucket:** déjalo **privado** (Block Public Access activado). La app entra con credenciales, así que
funciona igual, y un resumen tiene datos personales. Activa **Versioning**: cada "modificar" guarda la versión anterior.

## 9. Docker

- `Dockerfile`: multi-etapa. La etapa de compilación corre en la plataforma nativa (`--platform=$BUILDPLATFORM`),
  así el build para ARM64 no emula Maven y es mucho más rápido.
- `.dockerignore`: **ya no excluye `src/`** (el Dockerfile lo copia; antes el build fallaba). Excluye `.env` y `target/`.
- `docker-compose.yml` (local y GitHub Actions), con servicios `db` (MySQL 8.4) y `app` (`build: .`, puerto 8080).
  Errores que tenía la versión anterior:
  - el servicio `app` estaba **dos veces** (YAML usa solo el último);
  - `depends_on: mysql`, pero el servicio se llamaba `db`;
  - `deploy/compose.ec2.yml` estaba escrito como si fuera un servicio, cuando es **otro archivo**;
  - faltaban `JWT_SECRET`, `CORS_ORIGINS` y las variables de AWS.
- `deploy/compose.ec2.yml`: usa la imagen de Docker Hub (no compila), `restart: unless-stopped`, **puerto 80**,
  MySQL sin exponer a internet y límites de memoria para 1 GB.
- `application-prod.properties`: la URL de la BD se arma con `DB_HOST`, `DB_PORT` y `DB_NAME`
  (sirve con Docker Compose y con una BD en otra EC2).

## 10. GitHub Actions (`.github/workflows/ci_cd.yml`)

**Secrets:** `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`, `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`,
`EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`, `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `DB_PASSWORD`.
**Variables:** `AWS_REGION`, `S3_BUCKET`, `DOCKER_IMAGE_NAME` y `CORS_ORIGINS` (opcional).

En la EC2: Docker y el plugin de Compose instalados, puerto **80** abierto en el Security Group (y 22 para SSH).
En Postman cambia `baseUrl` a `http://<IP-publica-EC2>`.

---

## 11. Probar con AWS RDS (temporal) y volver al contenedor

Hay **dos modos** para la base de datos y los eliges con la variable de GitHub `DB_MODE`:

| `DB_MODE` | Compose que usa el job 3 | Dónde está MySQL |
|---|---|---|
| `contenedor` (o vacío) | `deploy/compose.ec2.yml` | Contenedor dentro de la EC2 |
| `rds` | `deploy/compose.ec2-rds.yml` | AWS RDS (lo administra Amazon) |

Al cambiar de modo, `--remove-orphans` apaga el contenedor que sobra (MySQL al pasar a RDS) y libera memoria.
Los datos del contenedor quedan guardados en su volumen y vuelven si regresas a `contenedor`.

### Crear el RDS (consola de AWS → RDS → Create database)
1. **Standard create** → **MySQL** → versión **8.4.x**.
2. **Templates:** *Free tier* (o *Dev/Test*). **Deployment:** Single-AZ.
3. **DB instance identifier:** `eventos-db`.
4. **Master username:** `eventos_admin`. **Credentials:** *Self managed*. Como contraseña usa la misma de tu secret
   `DB_PASSWORD` (sin `/`, `"`, `@` ni espacios: la de `openssl rand -hex 16` sirve).
5. **Instance class:** `db.t3.micro` (o `db.t4g.micro`). **Storage:** 20 GB, sin *autoscaling*.
6. **Connectivity:** marca **Connect to an EC2 compute resource** y elige tu instancia `eventos-api`.
   AWS crea solo los Security Groups para que **solo esa EC2** llegue al puerto 3306.
   **Public access: No.**
7. **Additional configuration:**
   - **Initial database name:** `eventos_db`.
   - Desmarca *Enable automated backups*.
   - Desmarca *Enhanced monitoring*: en el Learner Lab falla, porque necesita un rol que no se puede crear.
8. **Create database** y espera unos 10 minutos, hasta que quede *Available*. Copia el **Endpoint** (pestaña *Connectivity & security*).

### Activarlo en GitHub
- Secrets: `DB_HOST` = el endpoint y `DB_USER` = `eventos_admin`.
- Variable: `DB_MODE` = `rds`.
- Ve a **Actions** → **Run workflow**. El job 3 dirá "Desplegar en AWS EC2 (BD rds)".
- RDS empieza vacío. El administrador se crea solo (AdminBootstrap) y Hibernate crea las tablas.

### Volver al contenedor y borrar el RDS
1. Variable `DB_MODE` = `contenedor` (o bórrala) → **Run workflow**.
2. RDS → selecciona `eventos-db` → **Actions → Delete**:
   - desmarca *Create final snapshot* y *Retain automated backups*;
   - escribe `delete me` para confirmar.
3. Opcional: borra los Security Groups `rds-ec2-*` / `ec2-rds-*` que creó el asistente.

## 12. Ver los datos con MySQL Workbench (túnel SSH)

MySQL **nunca** se abre a internet (ni el contenedor ni RDS). Workbench entra **a través de la EC2** por SSH;
a eso se le llama *bastion host*, y es la práctica estándar.

Antes, una vez: `chmod 400 ~/Downloads/labsuser.pem` en la Terminal.

Workbench → **+** (nueva conexión) → **Connection Method: Standard TCP/IP over SSH**:

| Campo | Modo `contenedor` | Modo `rds` |
|---|---|---|
| SSH Hostname | `IP-publica-EC2:22` | `IP-publica-EC2:22` |
| SSH Username | `ec2-user` | `ec2-user` |
| SSH Key File | `labsuser.pem` | `labsuser.pem` |
| MySQL Hostname | `127.0.0.1` | el endpoint de RDS |
| MySQL Server Port | `3306` | `3306` |
| Username | `eventos_app` | `eventos_admin` |
| Password | tu `DB_PASSWORD` (*Store in Keychain*) | tu `DB_PASSWORD` |

**Test Connection** → **OK** → abre el esquema `eventos_db` y verás las tablas `cliente`, `evento` y `compra`.
(En la tabla `cliente`, la columna `password` muestra el hash BCrypt: así se ve una contraseña bien guardada.)

Alternativa rápida desde la EC2 (EC2 Instance Connect), solo en modo contenedor:
```bash
cd ~/eventos-api && docker compose -f compose.ec2.yml exec db mysql -u eventos_app -p eventos_db
```

## 13. Administrador restringido por IP

Con la variable de GitHub `ADMIN_ALLOWED_IPS` (IP o CIDR, separadas por coma), por ejemplo `190.20.30.40/32`:
- el **login** de un ADMIN desde otra IP responde **403** y no entrega token;
- un **token de ADMIN** usado desde otra IP responde **403**, aunque el token sea válido (por si te lo roban);
- los clientes no se ven afectados.

**Tu IP pública:** en la Terminal, `curl -4 ifconfig.me`. Usa `-4`, porque la EC2 trabaja con IPv4.
Si te conectas desde otra red (la de Duoc, tu celular), agrégala con una coma.
Tras cambiar la variable, haz **Run workflow** para que llegue a la EC2.

Para probarlo en local, pon `ADMIN_ALLOWED_IPS=127.0.0.1,::1` en tu `.env`: el admin funciona en localhost.
Si pones otra IP, verás el 403.

**¿Por qué no se hace con el Security Group?** El Security Group filtra por **puerto**, y clientes y admin usan el
mismo (80); solo la aplicación sabe quién es admin. El Security Group sí sirve para el SSH, pero el puerto 22 debe
quedar abierto, porque GitHub Actions entra por ahí desde IPs que cambian.

Se usa la IP real de la conexión (`getRemoteAddr()`) y **no** el header `X-Forwarded-For`, que cualquiera puede inventar.
