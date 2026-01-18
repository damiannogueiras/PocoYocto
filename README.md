# Plan para Sistema Embebido con Yocto

## Introducción

Este documento describe el plan crear sistema embebido generado con el Proyecto Yocto. 

El objetivo es proporcionar una guía estructurada para verificar la estabilidad, funcionalidad y rendimiento de la imagen del sistema antes de su despliegue.

Describe el procedimiento a llevar para la creación de ramas y actualización de ficheros con el fin de mantener varios sistemas embebidos en el mismo repositorio.

## Fases

El proceso se divide en las siguientes fases:

1. **Configuracion de la rama**: Creación de la rama específica para el sistema deseado
2. **Lanzamiento del contenedor del Entorno**: Verificación del contenedor Docker y la estructura de Yocto.
3. **Construcción de Capas**: Generación de las capas necesarias, ya sean las publicas (`metas`) o las personalizadas (`metas-propias`)
4. **Construcción de Imagen**: Construcción de la imágen
5. **Documentación de Resultados**: Reporte de los hallazgos.

---
### Fase 1: Creación de rama

Desde el commit etiquetado con ´init´, creamos una nueva rama.

Nombre: `bsp/machine-version`

En esta rama haremos toda la configuración y construiremos la imágen.

**Crear/modificar** un `README-machine.md` con las instrucciones de configuración y proceso

Preferiblemente que este fichero pueda ser ejecutado por IA

Para nuevos sistemas, volvemos a crear una rama nueva desde `init`

Si queremos hacer una versión diferente de un sistema ya creado, la nueva rama la podemos hacer desde la rama del sistema ya creado, asi mantenemos los ficheros.

### Fase 2: Lanzamiento del contenedor Entorno

**Objetivo**: Asegurar que el entorno de desarrollo y construcción esté operativo.

**Contexto**: El entorno está en el repositorio [PocoYocto-env](https://github.com/damiannogueiras/PocoYocto-env.git)

**Pasos**:

2.0 **Configurar Volumenes** `docker-compose.yaml`:

  - Volumen de configuracion: `build-machine:/home/yoctouser/yocto_projects/poky/meta-poky/build`
  - Volumen de salida: `yocto-output-machine:/home/yoctouser/yocto_output`

2.1 **Iniciar el Contenedor Docker**:

Tenemos el `docker-copmpose-template.yaml` en el directorio del repositorio (está como un submódulo de git)

Lo copiamos en el directo base como `docker-compose.yaml` y lo personalizamos

Lo lanzamos con: `docker-compose up -d`

2.2 **Inicializar el entorno de construcción de Yocto**:
    *   Navega al directorio poky (ya está en clonado en la imagen Docker)
    *   Ejecuta `source oe-init-build-env`
    *   Esto creará un directorio `build` en el que se realizará la construcción de la imagen.

2.3 **Chequeear layers**:
    *   Ejecuta `yocto-check-layer-wrapper` para verificar que todos los layers estén correctamente configurados.

### Fase 2: Construcción de Imagen

**Objetivo**: Generar una imagen del sistema operativo que incluya los paquetes de prueba.

**Pasos**:

1.  **Configurar la Build para Pruebas**:
    *   Copiar el fichero /home/yoctouser/yocto_projects/poky/meta-poky/conf/local.conf.sample a build/conf/local.conf
    *   Edita el fichero `conf/local.conf` de tu directorio de build.
    *   Asegúrate de que las siguientes variables estén presentes para incluir los paquetes de `ptest` (package testing):
        ```
        EXTRA_IMAGE_FEATURES += " ptest-pkgs"
        ```
    *   Asegúrate de que la variable `TMPDIR` esté configurada correctamente para evitar problemas de permisos y distinción de may y min de MacOS:
        ```
        TMPDIR = "${HOME}/tmp"
        ```
    *   Tipo de paquetes
        ```
        PACKAGE_CLASSES ?= "package_deb"
        ```

2.  **Lanzar la Construcción (Build)**:
    *   Desde el directorio de build, ejecuta el comando `bitbake` para construir tu imagen deseada. Se recomienda empezar con una imagen base como `core-image-minimal` o `core-image-base`.
        ```
        bitbake core-image-minimal
        ```
    *   Este proceso puede tardar un tiempo considerable dependiendo de los recursos de tu máquina.

### Fase 3: Despliegue y Arranque con QEMU

**Objetivo**: Arrancar la imagen generada en un entorno emulado para realizar las primeras pruebas sin necesidad de hardware físico.

**Pasos**:

1.  **Ejecutar QEMU**:
    *   Yocto incluye un script para facilitar la ejecución con QEMU. Una vez finalizada la build, ejecuta:
        ```
        runqemu qemux86-64
        ```
        *(Reemplaza `qemux86-64` por la arquitectura de tu `MACHINE` si es diferente, ej: `qemuarm`)*.
    *   Se abrirá una ventana emulando el arranque de tu sistema. Deberías ver los logs de arranque del kernel y finalmente un prompt de login.

2.  **Login en el Sistema**:
    *   Por defecto, el usuario es `root` sin contraseña.

### Fase 4: Pruebas Manuales Básicas

**Objetivo**: Realizar una verificación rápida (smoke test) de que el sistema base es funcional.

**Checklist**:

-   [ ] **Acceso al sistema**: ¿Puedes hacer login?
-   [ ] **Kernel y Arranque**: Ejecuta `dmesg` y busca mensajes de error críticos.
-   [ ] **Sistema de Ficheros**: Ejecuta `df -h`. ¿El espacio en disco es el esperado? ¿Las particiones están montadas correctamente?
-   [ ] **Red**: Ejecuta `ifconfig` o `ip a`. ¿La interfaz de red está levantada? ¿Puedes hacer ping a una dirección externa (ej: `ping 8.8.8.8`)?
-   [ ] **Gestión de Paquetes**: Si tu imagen incluye un gestor (ej. `rpm`, `opkg`), verifica que puedes listar los paquetes instalados. Por ejemplo: `opkg list-installed`.

### Fase 5: Pruebas Automatizadas (ptest)

**Objetivo**: Ejecutar los tests automatizados que vienen empaquetados en la imagen gracias a la configuración de la Fase 2.

**Pasos**:

1.  **Listar Tests Disponibles**:
    *   Una vez logueado en la terminal de QEMU, puedes ver qué paquetes incluyen tests `ptest`:
        ```
        ls /usr/lib/*/ptest/
        ```

2.  **Ejecutar Todos los Tests**:
    *   Yocto proporciona un script para lanzar todos los tests de manera secuencial.
        ```
        ptest-runner
        ```

3.  **Ejecutar un Test Específico**:
    *   Si solo quieres probar un paquete, puedes navegar a su directorio de `ptest` y ejecutarlo manualmente. Por ejemplo, para `coreutils`:
        ```
        cd /usr/lib/coreutils/ptest/
        ./run-ptest
        ```

4.  **Analizar Resultados**:
    *   Los resultados se mostrarán en la consola. Presta atención a los tests marcados como `FAIL` o `SKIP`. Los logs detallados suelen guardarse en un subdirectorio `results`.

### Fase 6: Documentación de Resultados

**Objetivo**: Consolidar los resultados de las pruebas de manera clara y accionable.

**Contenido del Reporte**:

*   **Resumen Ejecutivo**: Breve descripción de los resultados generales.
*   **Versiones**:
    *   Versión/hash del commit de tu proyecto.
    *   Imagen de Yocto generada (`core-image-minimal`, etc.).
*   **Resultados de Pruebas Manuales**: Checklist de la Fase 4 con el estado de cada punto.
*   **Resultados de Pruebas Automatizadas**:
    *   Log de la salida de `ptest-runner`.
    *   Lista de los tests que fallaron, con un análisis inicial de la causa si es posible.
*   **Incidencias Abiertas**: Lista de bugs o problemas encontrados, con los pasos para reproducirlos.

---
