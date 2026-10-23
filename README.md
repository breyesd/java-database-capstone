# Smart Clinic Management System

Sistema web de gestión de clínicas con dashboards para administradores, doctores y pacientes. La aplicación permite administrar doctores, registrar pacientes, reservar citas, consultar disponibilidad y gestionar recetas médicas.

## Funcionalidades

- Autenticación de administradores, doctores y pacientes mediante JWT.
- Autorización por rol con Spring Security y contraseñas almacenadas con BCrypt.
- Gestión de doctores: alta, edición, eliminación, búsqueda y disponibilidad.
- Registro y autenticación de pacientes.
- Reserva, consulta, actualización y cancelación de citas.
- Gestión de recetas médicas con almacenamiento en MongoDB.
- Dashboards de administrador y doctores renderizados con Thymeleaf.
- Interfaz de paciente implementada con HTML, CSS y JavaScript.

## Arquitectura

El proyecto sigue una arquitectura de tres capas:

```text
Navegador
   │
   ▼
Spring Security + filtro JWT
   │
   ▼
Controllers REST / Controllers MVC
   │
   ▼
Services (reglas de negocio)
   │
   ├── Repositories JPA ──► MySQL
   └── Repositories Mongo ─► MongoDB
```

| Capa | Responsabilidad |
|---|---|
| Presentación | Controllers REST, dashboards Thymeleaf y frontend estático |
| Aplicación | Controllers, services, autenticación, validación y reglas de negocio |
| Datos | Entidades JPA en MySQL y documentos MongoDB |

## Stack tecnológico

- Java 17
- Spring Boot 3.4.4
- Spring Web, Spring MVC y Thymeleaf
- Spring Data JPA e Hibernate
- Spring Data MongoDB
- Spring Security
- JJWT 0.12.6
- Bean Validation
- MySQL 8
- MongoDB 6
- Maven 3.9 o superior
- Docker y Docker Compose

## Estructura del proyecto

```text
.
├── app/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/project/back_end/
│       │   │   ├── config/
│       │   │   ├── controllers/
│       │   │   ├── DTO/
│       │   │   ├── models/
│       │   │   ├── mvc/
│       │   │   ├── repo/
│       │   │   └── services/
│       │   └── resources/
│       │       ├── static/
│       │       ├── templates/
│       │       └── application.properties
│       └── test/
├── .github/workflows/
└── README.md
```

## Requisitos

### Opción recomendada: Docker

- Docker Engine o Docker Desktop.
- Docker Compose v2 o superior.
- Puertos disponibles: `8080`, `3306` y `27017`.

### Ejecución local sin Docker

- JDK 17.
- Maven 3.9 o superior.
- MySQL 8 accesible.
- MongoDB 6 accesible, o una versión compatible con el driver de Spring Boot 3.4.x.
- MongoDB Shell (`mongosh`), si se usará el comando de comprobación local.

## Inicio rápido con Docker Compose

Compose crea la aplicación, MySQL y MongoDB. La aplicación espera a que ambas bases de datos superen sus healthchecks antes de iniciar.

Desde la raíz del repositorio:

```bash
cd app
docker compose up --build
```

Cuando la aplicación esté lista, ábrela en:

```text
http://localhost:8080/
```

Comandos útiles:

```bash
# Ver el estado de los servicios
docker compose ps

# Seguir los logs de la aplicación
docker compose logs -f app

# Detener y eliminar los contenedores
docker compose down
```

Para reconstruir la imagen después de modificar el código:

```bash
docker compose up --build
```

### Consideraciones de Docker Compose

- La configuración incluida es únicamente para desarrollo local.
- MySQL usa `root/root` y la base `mydb`.
- MongoDB se inicia sin autenticación y usa la base `my_mongo_db`.
- Los puertos de las bases de datos se publican en el host. No expongas estos servicios en un entorno público.
- No hay volúmenes persistentes: al eliminar los contenedores con `docker compose down`, se pierden los datos guardados en MySQL y MongoDB.
- La imagen se construye en dos etapas: Maven con JDK 17 compila el proyecto y una JRE 17 ejecuta únicamente el JAR resultante.

## Ejecución local con Maven

### 1. Preparar MySQL

Crea la base de datos usada por la configuración local:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS cms;"
```

### 2. Iniciar MongoDB

MongoDB debe estar disponible en `localhost:27017`. Puedes comprobar la conectividad con:

```bash
mongosh --quiet --eval "db.runCommand({ ping: 1 })"
```

Si MongoDB exige autenticación, comprueba la conexión con la URI completa:

```bash
mongosh "mongodb://usuario:tu-password@localhost:27017/prescriptions?authSource=admin" --quiet --eval "db.runCommand({ ping: 1 })"
```

Usa una URI sin autenticación si el servidor local no la requiere:

```text
mongodb://localhost:27017/prescriptions
```

Si MongoDB exige autenticación, incluye las credenciales y el origen de autenticación:

```text
mongodb://usuario:tu-password@localhost:27017/prescriptions?authSource=admin
```

### 3. Configurar la aplicación

Edita `app/src/main/resources/application.properties` y sustituye los valores locales:

```properties
spring.datasource.url=jdbc:mysql://localhost/cms?usessl=false
spring.datasource.username=root
spring.datasource.password=tu-password
spring.data.mongodb.uri=mongodb://usuario:tu-password@localhost:27017/prescriptions?authSource=admin
jwt.secret=cambia-esta-clave-por-un-secreto-seguro
```

No reutilices credenciales de ejemplo ni guardes secretos reales en el repositorio.

### 4. Iniciar Spring Boot

```bash
cd app
mvn spring-boot:run
```

La aplicación queda disponible en `http://localhost:8080/`.

También puedes construir y ejecutar el JAR por separado:

```bash
cd app
mvn clean package -DskipTests
java -jar target/back-end-0.0.1-SNAPSHOT.jar
```

## Configuración

Spring Boot permite reemplazar las properties mediante variables de entorno.

| Property | Variable de entorno | Descripción |
|---|---|---|
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | URL JDBC de MySQL |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | Usuario de MySQL |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | Contraseña de MySQL |
| `spring.data.mongodb.uri` | `SPRING_DATA_MONGODB_URI` | URI de MongoDB |
| `jwt.secret` | `JWT_SECRET` | Clave de firma de los tokens JWT |
| `server.port` | `SERVER_PORT` | Puerto HTTP de la aplicación |

La property `api.path` está configurada actualmente con el valor `/`. No la modifiques sin actualizar de forma coordinada los controllers, las reglas de Spring Security y el frontend, porque actualmente solo se aplica a parte de los controllers.

Docker Compose ya reemplaza las variables de MySQL y MongoDB para usar los nombres de servicio internos de la red.

## Modelo de datos

### MySQL

- `Admin`: administrador del sistema.
- `Doctor`: datos profesionales, contacto y horarios disponibles.
- `Patient`: datos personales y de contacto.
- `Appointment`: relación entre doctor y paciente, fecha, hora y estado.

Los horarios de doctor se almacenan como intervalos `HH:mm-HH:mm`. Una cita dura una hora y utiliza el estado `0` para `scheduled` o `1` para `completed`.

### MongoDB

- `Prescription`: paciente, id de la cita, medicamento, dosis y notas del doctor.
- La colección utilizada es `prescriptions`.
- `Prescription.appointmentId` es una referencia lógica al id de la cita en MySQL, sin foreign key entre bases de datos.

## API REST

La API utiliza JSON. En las rutas protegidas, el JWT se envía como último segmento de la URL, siguiendo el contrato existente del frontend.

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/admin` | Público | Inicia sesión como administrador |
| `POST` | `/doctor/login` | Público | Inicia sesión como doctor |
| `GET` | `/doctor` | Público | Lista doctores |
| `GET` | `/doctor/filter/{name}/{time}/{speciality}` | Público | Filtra doctores |
| `POST` | `/doctor/{token}` | Administrador | Crea un doctor |
| `PUT` | `/doctor/{token}` | Administrador | Actualiza un doctor |
| `DELETE` | `/doctor/{id}/{token}` | Administrador | Elimina un doctor y sus citas |
| `GET` | `/doctor/availability/{user}/{doctorId}/{date}/{token}` | Administrador, doctor o paciente | Consulta disponibilidad |
| `POST` | `/patient` | Público | Registra un paciente |
| `POST` | `/patient/login` | Público | Inicia sesión como paciente |
| `GET` | `/patient/{token}` | Paciente | Consulta sus datos |
| `GET` | `/patient/{id}/{user}/{token}` | Paciente o doctor | Consulta el historial de citas |
| `GET` | `/patient/filter/{condition}/{name}/{token}` | Paciente | Filtra sus citas |
| `GET` | `/appointments/{date}/{patientName}/{token}` | Doctor | Lista citas de una fecha |
| `POST` | `/appointments/{token}` | Paciente | Reserva una cita |
| `PUT` | `/appointments/{token}` | Paciente | Actualiza una cita |
| `DELETE` | `/appointments/{id}/{token}` | Paciente | Cancela una cita |
| `POST` | `/prescription/{token}` | Doctor | Guarda una receta y completa la cita |
| `GET` | `/prescription/{appointmentId}/{token}` | Doctor | Consulta la receta de una cita |

Los endpoints MVC de dashboards son:

| Método | Ruta | Acceso |
|---|---|---|
| `GET` | `/adminDashboard/{token}` | Administrador |
| `GET` | `/doctorDashboard/{token}` | Doctor |

### Ejemplo de inicio de sesión

```bash
curl -X POST http://localhost:8080/patient/login \
  -H "Content-Type: application/json" \
  -d '{"email":"paciente@example.com","password":"tu-password"}'
```

Una autenticación correcta devuelve:

```json
{
  "token": "<jwt>"
}
```

Los clientes de API reciben `401 Unauthorized` o `403 Forbidden` cuando falla la autenticación o el acceso por rol. Las peticiones del navegador que aceptan `text/html` se redirigen a `/`. Las operaciones que fallan por validación usan `400 Bad Request`; los conflictos de datos, `409 Conflict`.

## Frontend

- Los dashboards de administrador y doctor se renderizan en el servidor mediante Thymeleaf.
- La interfaz de paciente usa HTML, CSS y JavaScript servido desde `app/src/main/resources/static/`.
- La URL base del frontend se define en `app/src/main/resources/static/js/config/config.js`:

```javascript
export const API_BASE_URL = "http://localhost:8080";
```

Si cambias el host o el puerto de la aplicación, actualiza también `API_BASE_URL`.

## Compilación y pruebas

Ejecuta los comandos desde `app/`:

```bash
# Compilar
mvn -q compile

# Ejecutar todas las pruebas
mvn test

# Construir el JAR
mvn clean package
```

La prueba de contexto de Spring necesita MySQL y MongoDB accesibles con la configuración vigente. Los workflows de GitHub Actions compilan el backend y ejecutan pasos de Checkstyle, HTML/CSS/JS y Hadolint. Los jobs de Checkstyle y frontend toleran errores (`|| true`), por lo que sus hallazgos no bloquean el pipeline; Hadolint sí puede fallar el job correspondiente.

## Antes de usar en producción

La configuración actual está orientada al desarrollo. Antes de desplegar:

1. Sustituye las credenciales de Compose por secretos gestionados fuera del repositorio.
2. Habilita autenticación en MongoDB y restringe el acceso a las bases de datos.
3. Publica únicamente el puerto HTTP de la aplicación; mantén MySQL y MongoDB en una red privada.
4. Usa un secreto JWT único y rotable.
5. Añade volúmenes persistentes o migraciones gestionadas para los datos.
6. Revisa `spring.jpa.hibernate.ddl-auto=update` y usa migraciones controladas en producción.
7. Ejecuta la aplicación con un usuario no privilegiado y configura límites de recursos.
8. Protege el tráfico con TLS y evita exponer JWT en URLs o registros; planifica una migración al encabezado `Authorization: Bearer`.

## Solución de problemas

### El puerto 8080 está ocupado

Detén el proceso que lo utiliza o cambia el mapeo `8080:8080` en `app/docker-compose.yml`.

### La aplicación no conecta con MySQL o MongoDB

Verifica los logs del servicio de aplicación y el estado de los contenedores:

```bash
cd app
docker compose ps
docker compose logs app
```

### Cambié `application.properties` y no se refleja

Las properties empaquetadas se cargan al construir la imagen. Reconstruye el contenedor:

```bash
cd app
docker compose up --build
```

### El frontend no encuentra la API

Comprueba que `API_BASE_URL` en `config.js` coincide con el host y el puerto publicados por la aplicación.
