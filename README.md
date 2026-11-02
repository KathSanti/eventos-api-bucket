# Eventos API — Spring Boot, JWT, AWS S3 y CI/CD

API REST para vender entradas a eventos. Cada compra genera un **resumen de compra** que se descarga como
archivo y se guarda en **AWS S3** (carpeta = número del resumen), donde también se puede modificar, descargar y borrar.
Cada `push` a `main` prueba, construye y despliega la API automáticamente en **AWS EC2** con **GitHub Actions**.

## Tecnologías
Java 21 · Spring Boot 4.1 (Web, JPA, Security, Validation) · JWT (jjwt) · Spring Cloud AWS (S3) ·
MySQL 8.4 / H2 · Docker y Docker Compose · GitHub Actions

## Recursos de AWS
Todo se crea en **us-east-1** (AWS Academy Learner Lab).

| Recurso | Para qué sirve | Configuración |
|---|---|---|
| **S3** `eventos-resumenes` | Guarda los resúmenes de compra: `15/resumen-compra-15.txt` | Privado (Block public access), con Versioning |
| **EC2** `eventos-api` | Servidor donde corre la API en Docker | Ubuntu 24.04, t3.small, key `vockey`, puertos 22 y 80 |
| **IP elástica** `184.194.141.160` | IP pública fija de la EC2 (no cambia al reiniciar el lab) | URL de la API: `http://184.194.141.160` |
| **RDS** `eventos-db` | Base de datos MySQL administrada | MySQL 8.4, db.t3.micro, sin acceso público, base `eventos_db` |

## Endpoints

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/registro` · `/api/auth/login` | Público | Registro e inicio de sesión (devuelven un JWT) |
| GET | `/api/eventos` · `/api/eventos/{nombre}` | Público | Lista y detalle de eventos |
| POST | `/api/eventos` | ADMIN | Crear evento |
| POST | `/api/compras` | CLIENTE | Comprar entradas (devuelve `numeroResumen`) |
| GET | `/api/clientes/me` · `/api/clientes/me/compras` | Autenticado | Perfil e historial |
| GET | `/api/compras/{numero}/resumen` | Dueño o ADMIN | Generar el resumen como archivo |
| POST | `/api/compras/{numero}/resumen/s3` | Dueño o ADMIN | Subir el resumen a S3 |
| PUT | `/api/compras/{numero}/resumen/s3` | Dueño o ADMIN | Modificar (form-data `file`, .txt) |
| GET | `/api/compras/{numero}/resumen/s3` | Dueño o ADMIN | Descargar desde S3 |
| DELETE | `/api/compras/{numero}/resumen/s3` | Dueño o ADMIN | Borrar de S3 |
| GET / PATCH | `/api/clientes` · `/api/clientes/{id}/desbloquear` | ADMIN | Listar clientes y desbloquear cuentas |

Las rutas protegidas usan el header `Authorization: Bearer <token>`.

## Ejecutar en local

```bash
cp .env.example .env            # completa JWT_SECRET (y S3/AWS si quieres probar el bucket)
./mvnw spring-boot:run          # H2 en memoria → http://localhost:8080
# o, con MySQL en Docker:
docker compose up --build
```

## Desplegar en AWS

1. **Learner Lab:** **Start Lab** y entra a la consola (región us-east-1).
2. **S3:** *Create bucket*, con *Block all public access* marcado y *Versioning* activado.
3. **EC2:** *Launch instance* con Ubuntu 24.04, `t3.small` y key `vockey`. En el Security Group abre **SSH 22** y **HTTP 80**.
4. **IP elástica:** *Elastic IPs → Allocate*, y después *Associate* a la instancia.
5. **Docker en la EC2:** *Connect → EC2 Instance Connect*:
   ```bash
   curl -fsSL https://get.docker.com | sudo sh && sudo usermod -aG docker ubuntu
   ```
6. **RDS:** *Crear base de datos → MySQL*, con estas opciones:
   - plantilla *Capa gratuita*;
   - usuario `admin` con contraseña *Autoadministrado*;
   - clase `db.t3.micro` (filtro "Clases ampliables");
   - *Conectarse a un recurso de EC2* → `eventos-api`, con acceso público **No**;
   - base inicial `eventos_db`;
   - **sin** monitorización mejorada.

   Copia el **Punto de enlace** cuando quede disponible.
7. **Docker Hub:** *Account settings → Personal access tokens → Generate* (Read & Write).
8. **JWT_SECRET:** genéralo en la Terminal:
   ```bash
   openssl rand -base64 32
   ```
9. **GitHub:** carga los secrets y variables de la tabla de abajo.
10. **Desplegar:** haz `push` a `main` o ve a *Actions → CI/CD - Eventos API → Run workflow*.

## Secrets y variables de GitHub Actions
*Settings → Secrets and variables → Actions.* Escribe cada valor **sin Enter al final**.

| Secret | Valor |
|---|---|
| `AWS_ACCESS_KEY_ID` · `AWS_SECRET_ACCESS_KEY` · `AWS_SESSION_TOKEN` | Learner Lab → *AWS Details* → *AWS CLI* → *Show*  |
| `DOCKERHUB_USERNAME` · `DOCKERHUB_TOKEN` | Usuario y token de Docker Hub |
| `EC2_HOST` | IP elástica |
| `EC2_USER` | `ubuntu` |
| `EC2_SSH_KEY` | Contenido completo de `labsuser.pem` (*AWS Details → Download PEM*) |
| `JWT_SECRET` | El generado con `openssl` |
| `ADMIN_EMAIL` · `ADMIN_PASSWORD` | Credenciales del administrador inicial (las eliges tú) |
| `DB_HOST` · `DB_USER` · `DB_PASSWORD` | Punto de enlace de RDS · `admin` · contraseña maestra de RDS |

| Variable | Valor |
|---|---|
| `AWS_REGION` | `us-east-1` |
| `S3_BUCKET` | `eventos-resumenes` |
| `DOCKER_IMAGE_NAME` | `eventos-api` |
| `DB_MODE` | `rds` (o `contenedor` para usar MySQL dentro de la EC2) |
| `ADMIN_ALLOWED_IPS` | *Opcional:* IPs autorizadas para el admin, por ejemplo `190.20.30.40/32` |

**En cada sesión del Learner Lab** las credenciales AWS vencen. Actualiza los **3 secrets AWS** y haz
**Run workflow**; si no, los endpoints de S3 responden 502.

## Pipeline CI/CD (`.github/workflows/ci_cd.yml`)
1. **Tests:** `mvn verify` con H2.
2. **Levantar backend:** construye la imagen, levanta app + MySQL con Docker Compose, verifica que la API responda (y que sin token devuelva 401) y publica la imagen en Docker Hub.
3. **Desplegar en EC2:** por SSH copia el compose, descarga la imagen y la levanta.

## Probar con Postman
Importa `postman/eventos-api.postman_collection.json` y configura las variables `baseUrl` = `http://184.194.141.160`
y `adminPassword`. Ejecuta las carpetas en orden; los tokens y el `numeroResumen` se guardan solos.
En producción no hay eventos de ejemplo: primero crea uno como admin.

## Ver la base de datos (MySQL Workbench)
*Connection Method:* **Standard TCP/IP over SSH**
- **SSH:** host `184.194.141.160:22`, usuario `ubuntu`, key file `labsuser.pem`. Antes, haz `chmod 400 labsuser.pem`.
- **MySQL:** host = punto de enlace de RDS, puerto `3306`, usuario `admin`, schema `eventos_db`.

## Seguridad
- **JWT:** firma HS256 con expiración y validación de `iss` y `aud`.
- **Contraseñas** guardadas con BCrypt.
- **Bloqueo:** la cuenta se bloquea tras 5 intentos fallidos.
- **Roles:** CLIENTE y ADMIN, con denegación por defecto de cualquier ruta no declarada.
- **Resúmenes privados:** cada cliente solo accede a los suyos (protección IDOR).
- **Admin por IP:** el administrador solo puede entrar desde IPs autorizadas.
- **Secretos fuera del código:** viven en GitHub Secrets.
- **Datos privados:** RDS y S3 no son públicos.