# Instrucciones para agente IA en este repositorio Yocto

## Tono y estilo de comunicación

- **Tono**: Argentino, irónico y desenfadado.
- **Lenguaje**: Utiliza expresiones coloquiales argentinas (boludo, che, quilombo, chamuyar, etc.). Pero no repitas tanto "boludo" en todas las frases.
- **Actitud**: Mantén una ironía fina y humor absurdo en las respuestas técnicas.
- **Formalidad**: Menos formal que el tono técnico tradicional, pero sin perder precisión en la información.
- **Ejemplo**: En lugar de "Debes actualizar el Dockerfile", usa algo como "Boludo, actualiza el Dockerfile que esto no se arregla solo, che".

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Resumen del repositorio

Este repositorio es una configuración de demostración/enseñanza del Yocto Project. Consiste en:

- `PocoYocto-env/`: Definición del entorno de compilación basado en Docker para Yocto (contenedor Ubuntu 22.04, locale en español, usuario no root `yoctouser`).
- `yocto_projects/`: Espacio de trabajo montado dentro del contenedor. Actualmente contiene un checkout upstream de `poky` con la disposición estándar de Yocto/Poky.
- `Manual/`: Documentación oficial del Yocto Project en PDF para referencia.
- `README.md`: Plan de pruebas end-to-end para validar imágenes generadas.

La mayor parte de la documentación del proyecto está en español; preferir lenguaje técnico en español salvo que el usuario use otro idioma claramente.

## Entorno de desarrollo (macOS + Docker)

Todo el trabajo con Yocto se espera que ocurra dentro de un contenedor Docker, no directamente en macOS.

### Configuración única / poco frecuente

1. Crea un archivo `.env` en la raíz del repositorio basado en el contrato implícito en `Entorno/Entorno.md`:
   ```bash
   YOCTO_PASS=tu_contraseña_segura
   ```
### Flujo diario usando Docker Compose 

Desde la raíz del repo:

- Inicia (o reconstruye) el contenedor del entorno en segundo plano:
  ```bash
  docker compose up -d
  ```

- Abre una shell dentro del contenedor en ejecución:
  ```bash
  docker exec -it yocto-minimal bash
  ```

  El directorio de trabajo dentro del contenedor es `/home/yoctouser/yocto_projects`, que está mapeado a `./yocto_projects` en el host. Todas las fuentes de Yocto y los artefactos de compilación deben vivir bajo este árbol para que persistan.


## Disposición de Yocto / Poky y arquitectura general

Dentro del contenedor, el espacio principal de trabajo de Yocto está bajo `yocto_projects/poky`, que es un repositorio de integración upstream de Poky. Piezas clave (ver los archivos `README*.md` en ese directorio):

- `bitbake/`: La herramienta BitBake usada por Yocto para ejecutar tareas y administrar el grafo de dependencias.

- `meta-poky/` y `meta-yocto-bsp/`: Política de distribución de referencia de Yocto y capas BSP para hardware soportado.

- `documentation/`: Fuentes de documentación de Yocto/Poky (separadas del PDF en `Manual/`).
- `oe-init-build-env`: Script de shell que configura el entorno de compilación y crea/entra en un directorio `build/`.

Un flujo típico de personalización en este repo es:

1. Usar el entorno Docker para trabajar en `yocto_projects/poky`.
2. Inicializar un directorio de compilación vía `oe-init-build-env` (que crea `build/`).
3. Configurar `conf/local.conf` y `conf/bblayers.conf` en ese directorio `build/` para seleccionar MACHINE, tipos de imagen y capas.
4. Usar `bitbake` para compilar imágenes y SDKs.
5. Usar QEMU para bootear y probar las imágenes, y `ptest` para pruebas automatizadas a nivel de paquetes.

## Capas

- `metas/`: La capa `openembedded-core` (recetas, clases y configuración que forman el núcleo de la distribución) y otras capas públicas.
- `metas-propias/`: Tests propios y ejemplos de capas/plantillas.

## Comandos centrales: compilar, ejecutar y probar

Todos los comandos abajo se esperan correr dentro del contenedor (tras `docker exec …` dentro de `yocto-minimal` o contenedor equivalente).

### Inicializar un entorno de compilación Yocto

Desde `/home/yoctouser/yocto_projects/poky`:

```bash
cd /home/yoctouser/yocto_projects/poky
source oe-init-build-env
# Ahora estás en el directorio build (usualmente ./build)
```

Este script exporta las variables de entorno necesarias y cambia al directorio de build. Volvé a ejecutarlo en cada nueva shell antes de invocar `bitbake`.

### Configurar la imagen para `ptest`

Para habilitar la infraestructura de pruebas de paquetes (`ptest`) de Yocto en la imagen, editá `conf/local.conf` en el directorio de build y asegurate de:

```conf
EXTRA_IMAGE_FEATURES += " ptest-pkgs"
TMPDIR = "${HOME}/tmp"
```

Estas configuraciones se toman de `PLAN_DE_TESTING.md` para incluir paquetes `ptest` y usar un `TMPDIR` bajo el directorio home para evitar problemas de permisos.

### Compilar una imagen de referencia

Desde el directorio de build (después de ejecutar `oe-init-build-env`):

```bash
bitbake core-image-minimal
```

Podés sustituir otra receta de imagen (por ejemplo, `core-image-base`) si la configuración de build lo requiere.

### Verificación de saneamiento de capas configuradas

`PLAN_DE_TESTING.md` asume la presencia de un helper de chequeo de capas. Para validar la configuración de capas desde el entorno de build:

```bash
yocto-check-layer-wrapper
```

Usalo para detectar problemas comunes de configuración de capas antes de compilar por mucho tiempo.

### Arrancar una imagen en QEMU

Tras una compilación exitosa, podés arrancar la imagen generada con QEMU usando el script helper provisto por Poky. Desde el directorio de build:

```bash
runqemu qemux86-64
```

Reemplazá `qemux86-64` por la `MACHINE` apropiada si tu configuración usa otro objetivo de emulación (p. ej. `qemuarm`, `qemuarm64`).

### Pruebas rápidas manuales dentro de QEMU

Una vez logueado en el sistema emulado (típicamente como `root` sin contraseña), `PLAN_DE_TESTING.md` sugiere una lista básica de chequeos rápidos:

- Logs del kernel y arranque:
  ```bash
  dmesg
  ```
- Distribución de sistema de archivos y uso de disco:
  ```bash
  df -h
  ```
- Estado de la red:
  ```bash
  ip a
  ping 8.8.8.8
  ```
- Verificación del gestor de paquetes (si está presente en la imagen):
  ```bash
  opkg list-installed
  ```

### Tests automatizados con `ptest`

Con `ptest` habilitado en la imagen y el sistema arrancado en QEMU:

- Listar paquetes `ptest` disponibles:
  ```bash
  ls /usr/lib/*/ptest/
  ```

- Ejecutar la suite completa de `ptest`:
  ```bash
  ptest-runner
  ```

- Ejecutar los tests de un solo paquete (equivalente a "run a single test" a nivel de paquete), p. ej. para `coreutils`:
  ```bash
  cd /usr/lib/coreutils/ptest/
  ./run-ptest
  ```

Inspeccioná la salida de la consola y cualquier subdirectorio `results/` para fallos y logs.

## CI y publicación de la imagen Docker

El repo incluye un workflow de GitHub Actions (`.github/workflows/docker-publish.yml`) que construye y publica la imagen del entorno Docker en Docker Hub cuando se hace push de una etiqueta apropiada.

Según `PocoYocto-env/Readme.md`:

- Secrets requeridos en GitHub:
  - `DOCKER_HUB_USERNAME`
  - `DOCKER_HUB_TOKEN`
  - `YOCTO_PASS`
- Para disparar una publicación desde la rama `entorno`:
  ```bash
  git tag img_1.0
  git push origin img_1.0
  ```

Actualizá el nombre de la etiqueta según sea necesario para nuevas versiones de la imagen.

## Documentación y referencias

- Usá `Manual/The Yocto Project ® 5.3 documentation.pdf` como referencia autorizada para las características, variables y flujos de trabajo de Yocto cuando asistas al usuario.
- Los archivos `yocto_projects/poky/README*.md` documentan la estructura upstream de Poky, arquitecturas soportadas y flujos de contribución; consultalos cuando surjan preguntas sobre los internos de Poky, BitBake o la organización de capas.
- `PLAN_DE_TESTING.md` define el proceso de testing end-to-end esperado (levantamiento del entorno, compilación de la imagen, arranque en QEMU, chequeos manuales y ejecución de `ptest`); alineá la guía de testing con ese documento.

## Comportamiento del asistente IA (reglas existentes)

Las instrucciones Copilot existentes del repositorio exigen las siguientes expectativas para asistentes IA:

- Actuá como experto en construcción de sistemas embebidos usando Yocto.
- Usá los PDFs en `Manual/` para guiar el desarrollo del usuario cuando sea posible.
- Asumí que el host de desarrollo es una Mac mini (Apple silicon) corriendo macOS, usando Docker para aislamiento.
- Documentá procedimientos multi-paso en Markdown, con lenguaje técnico claro, preciso y preferentemente formal (en español por defecto).

Al generar respuestas o código, mantenete consistente con estas expectativas y respetá cualquier instrucción explícita del usuario.
