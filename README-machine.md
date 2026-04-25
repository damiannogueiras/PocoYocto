# Plantilla de Configuración para Raspberry Pi 4 (64-bit)

## Introducción

Este documento contiene los pasos esquemáticos para configurar, construir y desplegar una imagen de Yocto para el
sistema **Raspberry Pi 4 (64-bit)**. Está diseñado para ser interpretado por una IA.

---

### Fase 1: Creación de Rama

El propósito es aislar la configuración de la nueva máquina.

- **rama_base**: `tag init en main`
- **nueva_rama**: `bsp/raspberrypi4-64-v1`
- **comando**: `git checkout -b bsp/raspberrypi4-64-v1 init`

---

### Fase 2: Lanzamiento del Entorno

Asegura que el entorno de construcción esté operativo.

1. **Configurar e Iniciar Contenedor**:
  
- El archivo `docker-compose.yaml` ya está en el directorio raíz del repo.
  
  - **nombre_contenedor**: `pocoyocto-raspberrypi4-64`
  - **volumenes**:
      - Volumen de configuracion: `build-raspberrypi4-64:/home/pocoyoctouser/build`
      - Volumen de salida: `output-raspberrypi4-64:/home/pocoyoctouser/output`
  - **comando**: `docker-compose up -d`

2. **Acceder al Contenedor**:
    - **nombre_contenedor**: `pocoyocto-raspberrypi4-64`
    - **comando**: `docker exec -it pocoyocto-raspberrypi4-64 bash`
   
3. **Inicializar Entorno Yocto**:
    - **directorio_poky**: `/home/pocoyoctouser/poky`
    - **comando**: `source oe-init-build-env ../build`
    - **directorio_build_resultante**: `/home/pocoyoctouser/build`

---

### Fase 3: Configuración de Capas (`bblayers.conf`)

Define qué capas (layers) se usarán para la construcción.

- **archivo**: `conf/bblayers.conf`
- **capas_requeridas**:
    - `/home/pocoyoctouser/poky/meta`
    - `/home/pocoyoctouser/poky/meta-poky`
    - `/home/pocoyoctouser/poky/meta-yocto-bsp`
    - `/home/pocoyoctouser/build/metas/meta-openembedded/meta-oe`
    - `/home/pocoyoctouser/build/metas-propias/meta-template`
    - `/home/pocoyoctouser/build/metas/meta-raspberrypi`  # ⚠️ Requiere agregar meta-raspberrypi como submódulo en metas/

---

### Fase 4: Configuración de la Imagen (`local.conf`)

Define la máquina, distribución y características de la imagen.

- **archivo**: `conf/local.conf`
- **configuracion**:
    - `MACHINE`: `"raspberrypi4-64"`
    - `DISTRO`: `"poky"`
    - `IMAGE_FEATURES:append`: `" ssh-server-dropbear"`
    - `IMAGE_INSTALL:append`: `" sudo"`
    - `INHERIT +=`: `"rm_work extrausers"`
    - `EXTRA_USERS_PARAMS`: `"useradd -P pinux admin;"`

> **Nota**: `EXTRA_USERS_PARAMS` usa la clase `extrausers` para crear el usuario `admin` con contraseña `pinux`.
> En producción, se recomienda usar una contraseña hasheada en lugar de texto plano.

---

### Fase 5: Construcción de la Imagen

Genera el artefacto final (.wic/.sdimg).

- **imagen_objetivo**: `core-image-minimal`
- **comando**: `bitbake core-image-minimal`

---

### Fase 6: Grabado en Hardware

1. **Copiar imagen al host**:
    - **imagen_origen**: `/home/pocoyoctouser/output/tmp/deploy/images/raspberrypi4-64/`
    - **destino_host**: `~/Downloads/`
    - **comando**: `docker cp pocoyocto-raspberrypi4-64:/home/pocoyoctouser/output/tmp/deploy/images/raspberrypi4-64/ ~/Downloads/`

2. **Grabar en tarjeta SD**:
    - **dispositivo**: `/dev/rdiskX`  (verificar con `diskutil list` en macOS)
    - **imagen**: `core-image-minimal-raspberrypi4-64.rootfs.wic.bz2`
    - **comando**: `sudo dd if=core-image-minimal-raspberrypi4-64.rootfs.wic of=/dev/rdiskX bs=4m conv=sync`

---

### Fase 7: Documentación y Verificación

Reporte de los resultados y pruebas.

- Conectarse por SSH: `ssh admin@<ip_de_la_rpi>`
- Contraseña: `pinux`
