## 🐳 Docker — Guía técnica y práctica

### 📦 1. Conceptos base

| Elemento          | Descripción                                                      |
| ----------------- | ---------------------------------------------------------------- |
| **Image**         | Plantilla inmutable que define un entorno (SO + binarios + app). |
| **Container**     | Instancia en ejecución de una imagen. Efímera.                   |
| **Dockerfile**    | Script declarativo que define cómo construir una imagen.         |
| **Build context** | Todo el contenido del directorio al hacer `docker build`.        |
| **Registry**      | Repositorio de imágenes (`Docker Hub`, `ghcr.io`, `harbor`…).    |

---

### ⚙️ 2. Flujo básico

```bash
docker build -t myapp:latest .     # Crear imagen
docker images                      # Ver imágenes
docker run -p 8080:8080 myapp      # Ejecutar contenedor
docker ps -a                       # Ver contenedores activos o parados
docker stop <id>                   # Parar
docker rm <id>                     # Eliminar contenedor
docker rmi <id>                    # Eliminar imagen
```

---

### 🧩 3. Estructura típica del proyecto

```
project/
 ├─ Dockerfile
 ├─ docker-compose.yml
 ├─ .dockerignore
 └─ src/...
```

`.dockerignore` evita copiar basura:

```
target
build
.git
.idea
node_modules
```

---

### 🧱 4. Buenas prácticas

* Un **WORKDIR** claro (`/app`, `/srv`, etc.).
* No copiar todo el contexto si no es necesario (`COPY . .` copia más de la cuenta).
* Siempre usar versiones de base explícitas (`eclipse-temurin:25-jdk-alpine`).
* No usar `latest` en producción.
* Multi-stage builds → menor tamaño de imagen.
* Variables configurables con `ENV` o `ARG`.
* Limitar permisos: `USER 1000:1000` cuando sea posible.
* Siempre exponer puertos usados (`EXPOSE 8080`).
* Montar volúmenes si necesitas persistencia (`-v data:/var/lib/...`).

---

### 🧰 5. Docker Compose

Permite orquestar varios servicios:

```yaml
services:
  app:
    build: .
    ports: ["8080:8080"]
  prometheus:
    image: prom/prometheus
    ports: ["9090:9090"]
```

Comandos:

```bash
docker compose up -d
docker compose logs -f app
docker compose down
```

---

### 🔍 6. Observabilidad

* **Logs**: `docker logs <container>`
* **Stats**: `docker stats`
* **Inspect**: `docker inspect <id>` → JSON completo
* **Exec** dentro: `docker exec -it <container> sh`

---

### 🧹 7. Limpieza

```bash
docker system prune -af   # Limpia todo (imágenes, contenedores, cache)
docker volume prune -f
```

---

### 🧩 8. Tips extra

* Usa **BuildKit** para mejor rendimiento:

  ```bash
  export DOCKER_BUILDKIT=1
  ```
* Puedes pasar variables:

  ```bash
  docker build --build-arg JAR_NAME=app.jar .
  ```
* Para debug de imágenes:

  ```bash
  docker run -it --entrypoint sh myapp
  ```