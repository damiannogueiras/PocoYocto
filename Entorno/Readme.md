# Configuración del Entorno Yocto en macOS con Docker

Este documento describe los pasos para configurar un entorno de desarrollo para Yocto Project en macOS utilizando Docker de forma segura.

## 1. Requisitos Previos

Asegúrate de tener el siguiente software instalado en tu Mac:

*   **Homebrew**: El gestor de paquetes para macOS.
*   **Docker Desktop**: La herramienta para crear y gestionar contenedores.

**Importante**: Antes de continuar, asegúrate de que la aplicación Docker Desktop esté en ejecución.

## 2. Creación del Entorno Docker

Para asegurar un entorno de compilación limpio y consistente, usaremos un `Dockerfile` y un archivo `.env` para gestionar secretos.

### Paso 1: Configurar la Contraseña en `.env`

Por seguridad, la contraseña del usuario dentro del contenedor no se escribirá directamente en el `Dockerfile`. En su lugar, se pasará como un argumento durante la construcción de la imagen.

1.  **Crea un archivo `.env`** en el directorio raíz del proyecto (`Yocto`).
2.  **Añade la siguiente línea** a este archivo, reemplazando `tu_contraseña_segura` por la que desees:

    ```
    YOCTO_PASS=tu_contraseña_segura
    ```

3.  He creado un archivo `.env.example` para que sirva de guía y un `.gitignore` para asegurar que el archivo `.env`

### Paso 2: Crear el `Dockerfile`

El `Dockerfile` utiliza un argumento (`ARG`) para recibir la contraseña en el momento de la construcción.

### Paso 3: Construir la Imagen Docker

Abre una terminal, asegúrate de estar en el directorio raíz de este proyecto (`Yocto`) y ejecuta el siguiente comando:

Este comando lee el `Dockerfile`, extrae la contraseña de tu archivo `.env` de forma segura y la pasa como argumento para construir la imagen `yocto_env`.

```bash
docker build --build-arg YOCTO_PASS=$(grep YOCTO_PASS .env | cut -d '=' -f2) -t yocto_env -f Entorno/Dockerfile .
```

La construcción puede tardar varios minutos.

### Paso 4: Iniciar el Contenedor

Una vez construida la imagen, inicia un contenedor. Este será tu entorno de trabajo para Yocto.

```bash
docker run -it --name yocto_build -v ~/yocto_projects:/home/yoctouser/yocto_projects yocto_env /bin/bash
```
*   `docker run -it`: Inicia un contenedor en modo interactivo.
*   `--name yocto_build`: Le da un nombre al contenedor para que puedas referenciarlo fácilmente.
*   `-v ~/yocto_projects:/home/yoctouser/yocto_projects`: Monta una carpeta local (`~/yocto_projects`) dentro del contenedor. **Todo tu trabajo se guardará aquí**.
*   `yocto_env`: El nombre de la imagen que quieres usar.
*   `/bin/bash`: Inicia una sesión de terminal dentro del contenedor.

## 3. Despliegue Automatizado (GitHub Actions)

Se ha configurado una GitHub Action para construir y publicar automáticamente la imagen en Docker Hub cuando se crea una etiqueta (tag) en la rama `entorno`.

### Configuración de Secretos en GitHub

Para que la Action funcione, debes configurar los siguientes **Repository Secrets** en GitHub (`Settings > Secrets and variables > Actions`):

1.  `DOCKER_HUB_USERNAME`: Tu nombre de usuario de Docker Hub.
2.  `DOCKER_HUB_TOKEN`: Un Access Token generado en Docker Hub (no uses tu contraseña real).
3.  `YOCTO_PASS`: La contraseña que se usará para el usuario `yoctouser` dentro de la imagen publicada.

### Cómo disparar la publicación

1.  Asegúrate de estar en la rama `entorno`.
2.  Crea una etiqueta que empiece por `img_` y súbela:
    ```bash
    git tag img_1.0
    git push origin img_1.0
    ```