# Plantilla de Configuración para [nombre_del_sistema]

## Introducción

Este documento contiene los pasos esquemáticos para configurar, construir y desplegar una imagen de Yocto para el
sistema **[nombre_del_sistema]**. Está diseñado para ser interpretado por una IA.

---

# Plantilla de Configuración para [nombre_del_sistema]

## Introducción

Este documento contiene los pasos esquemáticos para configurar, construir y desplegar una imagen de Yocto para el
sistema **[nombre_del_sistema]**. Está diseñado para ser interpretado por una IA.

---

### Fase 1: Creación de Rama

El propósito es aislar la configuración de la nueva máquina.

- **rama_base**: `[tag init en main | bsp/nombre_maquina-version]`
- **nueva_rama**: `bsp/[nombre_maquina]-[nueva_version]`
- **comando**: `git checkout -b [nueva_rama] [rama_base]`

---

### Fase 2: Lanzamiento del Entorno

Asegura que el entorno de construcción esté operativo.

1. **Configurar e Iniciar Contenedor**:
  
- Copiar `docker-compose-template.yml` al directorio principal como `docker-compose.yaml`
  
- Personalizarlo

  - **nombre_contenedor**: `[pocoyocto-machine]`
  - **volumenes**:
      - Volumen de configuracion: `[build-machine]:/home/yoctouser/yocto_projects/poky/build`
      - Volumen de salida: `[yocto-output-machine]:/home/yoctouser/yocto_output`
      - Al final del archivo los nombres de los `volumes:(...)`
  - **comando**: `docker-compose up -d`

2. **Acceder al Contenedor**:
    - **nombre_contenedor**: `[pocoyocto-machine]`
    - **comando**: `docker exec -it [nombre_contenedor] bash`
   
3. **Inicializar Entorno Yocto**:
    - **directorio_poky**: `/home/yoctouser/yocto_projects/poky`
    - **comando**: `source oe-init-build-env`
    - **directorio_build_resultante**: `/home/yoctouser/yocto_projects/poky/build`

---

### Fase 3: Configuración de Capas (`bblayers.conf`)

Define qué capas (layers) se usarán para la construcción.

- **archivo**: `conf/bblayers.conf`
- **capas_requeridas**:
    - `[ruta/a/poky/meta]`
    - `[ruta/a/poky/meta-poky]`
    - `[ruta/a/metas]`
    - `[ruta/a/metas-propias]`

---

### Fase 4: Configuración de la Imagen (`local.conf`)

Define la máquina, distribución y características de la imagen.

- **archivo**: `conf/local.conf`
- **configuracion**:
    - `MACHINE`: `"[nombre_maquina]"` (ej: "raspberrypi4-64")
    - `DISTRO`: `"[nombre_distro]"` (ej: "poky")
    - `IMAGE_FEATURES`: `"[feature1 feature2]"` (ej: "ssh-server-dropbear")
    - `IMAGE_INSTALL:append`: ` " [paquete1 paquete2]"`
    - `INHERIT +=`: `"[rm_work]"`

---

### Fase 5: Construcción de la Imagen

Genera el artefacto final (.sdimg).

- **imagen_objetivo**: `[core-image-minimal]`
- **comando**: `bitbake [imagen_objetivo]`

---

#### Si es Hardware:

1. **Copiar imagen al host**:
    - **imagen_origen**: `[/home/yoctouser/yocto_output]`
    - **destino_host**: `[directorio/local]`
    - **comando**: `docker cp [nombre_contenedor]:[imagen_origen] [destino_host]`
2. **Grabar en tarjeta SD**:
    - **dispositivo**: `[/dev/rdiskX]`
    - **imagen**: `[nombre_de_la_imagen]`
    - **comando**: `sudo dd if=[imagen] of=[dispositivo] bs=4m conv=sync`

---

### Fase 7: Documentación y Verificación

Reporte de los resultados y pruebas.

- **Checklist de Verificación Manual**:
    - `[ ]` Login exitoso.
    - `[ ]` `dmesg` sin errores críticos.
    - `[ ]` `df -h` muestra sistema de ficheros correcto.
    - `[ ]` `ping [ip_externa]` funciona.
- **Resultados de Pruebas Automatizadas (si aplica)**:
    - **comando**: `ptest-runner`
    - **fallos**: `[lista_de_tests_fallidos]`
- **Incidencias**:
    - `[ID_incidencia]`: [Descripción breve del problema].

---