# Plan para Sistema Embebido con Yocto

## Introducción

Este documento describe el plan para crear un sistema embebido generado con el Proyecto Yocto.
El objetivo es proporcionar una guía estructurada para verificar la estabilidad, funcionalidad y rendimiento de la imagen del sistema antes de su despliegue.
Describe el procedimiento a llevar para la creación de ramas y actualización de ficheros con el fin de mantener varios sistemas embebidos en el mismo repositorio.

## Configuración para Raspberry Pi 4 64-bit

Esta sección describe los pasos para construir una imagen de Yocto para la **Raspberry Pi 4 64-bit** con las siguientes características:
- Sin entorno gráfico (solo línea de comandos).
- Acceso por SSH habilitado.
- Soporte para Wi-Fi preconfigurado.
- Limpieza automática del directorio de trabajo de recetas (`rm_work`).

### 1. Preparación del Entorno

0. Configurar los volumenes para que la configuración y la salida de ficheros queden fuer del contenedor
    
   - Volumen de build: build-rpi4-64
   - Volumes de datos de salida: yocto_output-rpi-4-64
   - nombre: pocoyocto-rpi4-64

1.  **Iniciar Contenedor**:
    ```bash
    cd PocoYocto-env
    docker-compose up -d
    ```
2.  **Acceder al Contenedor**:
    ```bash
    docker exec -it yocto-minimal bash
    ```
3.  **Inicializar Entorno Yocto**:
    Se utilizará el directorio `build-rpi4-64` para la configuración.
    ```bash
    cd /home/yoctouser/PocoYocto
    source yocto_projects/poky/oe-init-build-env build-rpi4
    ```

### 2. Configuración de Wi-Fi

La imagen se construirá con un fichero de configuración para Wi-Fi. **Debe editar este fichero para incluir sus credenciales.**

-   **Archivo a editar**: `metas-propias/meta-raspberrypi4-64-custom/recipes-connectivity/wpa_supplicant/files/wpa_supplicant.conf`
-   **Contenido**:
    ```
    ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev
    update_config=1
    country=US

    network={
        ssid="your_ssid"
        psk="your_password"
    }
    ```
    Reemplace `"your_ssid"` y `"your_password"` con los datos de su red.

### 3. Construcción de la Imagen

Una vez configurado el entorno, puede lanzar la construcción de la imagen.

-   **Comando**:
    ```bash
    bitbake core-image-minimal
    ```

La imagen resultante (`.wic.bz2`) se encontrará en:
`build-rpi4/tmp/deploy/images/raspberrypi4-64/`

### 4. Despliegue en Hardware

1.  **Copiar la imagen del contenedor al host**:
    ```bash
    docker cp yocto-minimal:/home/yoctouser/PocoYocto/build-rpi4/tmp/deploy/images/raspberrypi4-64/core-image-minimal-raspberrypi4-64.wic.bz2 .
    ```
2.  **Descomprimir la imagen**:
    ```bash
    bunzip2 core-image-minimal-raspberrypi4-64.wic.bz2
    ```
3.  **Grabar en la tarjeta SD**:
    Identifique el dispositivo de su tarjeta SD (ej: `/dev/rdiskX` en macOS, `/dev/sdX` en Linux).
    ```bash
    sudo dd if=core-image-minimal-raspberrypi4-64.wic of=/dev/rdiskX bs=4m conv=sync
    ```

### 5. Verificación

- Inserte la SD en la Raspberry Pi y arranque el sistema.
- Conéctese por SSH. El nombre de host será `raspberrypi4-64`.
- Verifique la conexión a la red.
- `dmesg` no debería mostrar errores críticos.
- `df -h` debería mostrar el sistema de archivos.

---
