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

**Contexto**: El entorno está en el repositorio [PocoYocto-env](https://github.com/damiannogueiras/PocoYocto-env.git).

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

1.  **Configurar la Build**:
    *   Copiar el fichero /home/yoctouser/yocto_projects/poky/meta-poky/conf/local.conf.sample a build/conf/local.conf
    *   Edita el fichero `conf/local.conf` de tu directorio de build.
    *   Asegúrate de que la variable `TMPDIR` esté configurada correctamente para evitar problemas de permisos y distinción de may y min de MacOS:
        ```
        TMPDIR = "${HOME}/tmp"
        ```

2.  **Lanzar la Construcción (Build)**:
    *   Desde el directorio de build, ejecuta el comando `bitbake` para construir tu imagen deseada. Se recomienda empezar con una imagen base como `core-image-minimal` o `core-image-base`.
        ```
        bitbake core-image-minimal
        ```
    *   Este proceso puede tardar un tiempo considerable dependiendo de los recursos de tu máquina.
