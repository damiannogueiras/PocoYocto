# README - i.MX95 EVK (sistema mínimo, sin GUI) con Poky

Resumen
-------
Plan paso a paso para construir una imagen Yocto mínima para la placa i.MX95 EVK. 

### Objetivo ###

Sistema sin entorno gráfico, solo terminal con herramientas básicas y acceso remoto (SSH).

Contrato (entrada / salida / éxito)
----------------------------------
- Entrada: poky como repositorio base. Usamos versión 'scarthgap'
- Salida: una imagen mínima arrancable para i.MX95 EVK (SD card / eMMC image) que arranca a shell, con SSH y utilidades básicas instaladas.
- Criterio de éxito: la placa arranca, se accede por consola serie y se puede iniciar sesión por SSH.

Checklist rápido (lo que vas a hacer)
-------------------------------------
- [ ] Preparar entorno Docker y entrar en shell del contenedor.
- [ ] Inicializar entorno `bitbake`.
- [ ] Revisar las layers sincronizadas y detectar la `MACHINE` correcta o crear una propia.
- [ ] Crear directorio de build y configurar `bblayers.conf` y `local.conf` si es necesario.
- [ ] Compilar imagen mínima (core-image-minimal o imagen personalizada con SSH).
- [ ] Flashear imagen en SD/eMMC y pruebas en placa.

Pasos detallados
----------------
1) Preparar el entorno (usar Docker como indica el repo)
   - Asegurate de tener el archivo `.env` en la raíz con `YOCTO_PASS` y levantá el contenedor según `PocoYocto-env/Readme.md`.
   - Copiar docker-compose-template.yml y peronalizarlo:
     - `yocto-output-imx95:/home/yoctouser/yocto_output` (para los tmp)
     - `build-imx95:/home/yoctouser/yocto_projects/poky/build` (directorio de salida)

```bash
# desde la raíz del repo (host mac)
docker compose up -d
# entrar al contenedor ya levantado
docker exec -it yocto-minimal bash
```

2) Inicializar entorno Bitbake

```bash
source poky/oe-init-build-env /home/yoctouser/yocto_projects/poky/build
```

3) Layers necesarias para el i.mx95

Necesitamos la layer `meta-freescale:

Ve a tu carpeta de metas. Clona la capa comunitaria de Freescale y añadela

```
git clone -b scarthgap https://git.yoctoproject.org/meta-freescale
git clone -b scarthgap https://github.com/Freescale/meta-freescale-distro.git
#(desde build)
bitbaker-layer add-layer metas/meta-freescale
bitbaker-layer add-layer metas/meta-freescale-distro

```
Tambien necesitamos la oe

```
git clone -b scarthgap https://github.com/openembedded/meta-openembedded.git
#(desde build)
bitbaker-layer add-layer metas/meta-openembedded/meta-oe
bitbaker-layer add-layer metas/meta-openembedded/meta-python
```

4) Nuestra capa personalizada

En el directorio `metas-propias` configuramos nuestras capas

Luego la añadimos

` bitbake-layers add-layer ../../meta-propias/meta-minimal-imx95 `

5) Revisar `conf/bblayers.conf` y `conf/local.conf`

Ejemplo mínimo para `bblayers.conf` (asegurate de usar las rutas reales relativas al `TOPDIR`):

```conf/bblayers.conf
# ...existing code...
BBLAYERS ?= " \
  /home/pocoyoctouser/poky/meta \
  /home/pocoyoctouser/poky/meta-poky \
  /home/pocoyoctouser/poky/meta-yocto-bsp \
  /home/pocoyoctouser/build/metas/meta-freescale \
  /home/pocoyoctouser/build/metas/meta-openembedded/meta-oe \
  /home/pocoyoctouser/build/metas/meta-openembedded/meta-python \
  /home/pocoyoctouser/build/metas-propias/meta-minimal-imx95 \
"
```

- Editá `conf/local.conf` y ajustá estas variables mínimas:

```conf/local.conf
# This sets the default machine to be qemux86-64 if no other machine is selected:
MACHINE ??= "imx95vk"

#
# Where to place downloads
#
# During a first build the system will download many different source code tarballs
# from various upstream projects. This can take a while, particularly if your network
# connection is slow. These are all stored in DL_DIR. When wiping and rebuilding you
# can preserve this directory to speed up this part of subsequent builds. This directory
# is safe to share between multiple builds on the same machine too.
#
# The default is a downloads directory under TOPDIR which is the build directory.
#
DL_DIR ?= "/home/yoctouser/output/downloads"

#
# Where to place shared-state files
#
# BitBake has the capability to accelerate builds based on previously built output.
# This is done using "shared state" files which can be thought of as cache objects
# and this option determines where those files are placed.
#
# You can wipe out TMPDIR leaving this directory intact and the build would regenerate
# from these files if no changes were made to the configuration. If changes were made
# to the configuration, only shared state files where the state was still valid would
# be used (done using checksums).
#
# The default is a sstate-cache directory under TOPDIR.
#
SSTATE_DIR ?= "/home/yoctouser/output/sstate-cache"

#
# Where to place the build output
#
# This option specifies where the bulk of the building work should be done and
# where BitBake should place its temporary files and output. Keep in mind that
# this includes the extraction and compilation of many applications and the toolchain
# which can use Gigabytes of hard disk space.
#
# The default is a tmp directory under TOPDIR.
#
TMPDIR = "/home/yoctouser/output/tmp"

#
# Distribution configuration
#
DISTRO ?= "poky"


#
# Package Management configuration
# We default to rpm:
PACKAGE_CLASSES ?= "package_rpm"

#
# SDK target architecture
#
# This variable specifies the architecture to build SDK items for and means
# you can build the SDK packages for architectures other than the machine you are
# running the build on (i.e. building i686 packages on an x86_64 host).
# Supported values are i686, x86_64, aarch64
#SDKMACHINE ?= "i686"

#
# Extra image configuration defaults
#  "debug-tweaks"   - make an image suitable for development
#                     e.g. ssh root access has a blank password
EXTRA_IMAGE_FEATURES ?= "debug-tweaks"

# By default disable interactive patch resolution (tasks will just fail instead):
PATCHRESOLVE = "noop"

#
# Disk Space Monitoring during the build
#
BB_DISKMON_DIRS ??= "\
    STOPTASKS,${TMPDIR},1G,100K \
    STOPTASKS,${DL_DIR},1G,100K \
    STOPTASKS,${SSTATE_DIR},1G,100K \
    STOPTASKS,/tmp,100M,100K \
    HALT,${TMPDIR},100M,1K \
    HALT,${DL_DIR},100M,1K \
    HALT,${SSTATE_DIR},100M,1K \
    HALT,/tmp,10M,1K"

# ELIMINACIÓN TOTAL DE GRÁFICOS Y CARACTERÍSTICAS PESADAS
DISTRO_FEATURES:remove = "x11 wayland vulkan opengl opencl directfb bluetooth 3g nfc"
MACHINE_FEATURES:remove = "accel-graphics accel-video touchscreen"

# Forzar BusyBox (Sustituye herramientas GNU grandes por versiones mini)
PREFERRED_PROVIDER_virtual/base-utils = "busybox"

# No instalar paquetes recomendados (Solo lo que pedimos explícitamente)
NO_RECOMMENDATIONS = "1"

# Optimización de espacio: borrar archivos temporales de trabajo al terminar cada receta
# Útil si tienes poco espacio en el Docker
INHERIT += "rm_work"
```



6) Identificar la `MACHINE` disponible o crear una
   - Listá los archivos `conf/machine` en las layers clonadas para encontrar el nombre correcto:

```bash
find . -path "*/conf/machine/*" -type f -print | sed -e 's#.*/conf/machine/##' | sort -u
# o desde el build dir (si TOPDIR apunta correctamente):
ls ${TOPDIR}/../meta-*/conf/machine || true
```

- Si no aparece `imx95-19x19-evk`, copiá una máquina similar y adaptá `KERNEL_IMAGETYPE`, `UBOOT_ARCH`, `MACHINE_FEATURES` y las entradas de WIC.

7) Kernel y U-Boot
   - Confirmá que las recetas para `linux-imx` y `u-boot` estén presentes en las layers sincronizadas. Si necesitás fijar versiones:

```conf
PREFERRED_VERSION_linux-imx = "<versión-deseada>"
PREFERRED_VERSION_u-boot = "<versión-deseada>"
```

8) Compilar la imagen mínima
   - Desde el build dir configurado (ejemplo `build-imx95`):

```bash
bitbake core-image-minimal
# o
bitbake core-image-base
```

9) Generar artefacto de SD/eMMC (usar wic si está soportado)
   - Si las recipes soportan `wic` o hay recetas específicas para SD, usar `do_image_wic` o los pasos provistos por NXP:

```bash
bitbake core-image-minimal -c do_image_wic
```

   - Para grabar en SD desde tu Mac (después de copiar la imagen al host), usar `dd` con cuidado:

```bash
sudo dd if=core-image-minimal-imx95-19x19-evk.wic of=/dev/rdiskN bs=4M conv=fsync
sync
```

10) Primer arranque y pruebas
   - Conectá la consola serie (p. ej. 115200 8N1) y observá el bootloader y kernel logs.
   - Iniciá sesión en la consola.
   - Comprobá red y ssh:

```bash
# en la placa
ip a
ping 8.8.8.8
systemctl status sshd
# desde host
ssh root@<ip_de_la_placa>
```

11) Pruebas útiles dentro del sistema mínimo
   - Ver uso de disco: `df -h`
   - Ver procesos: `top` o `ps aux`
   - Revisar dmesg: `dmesg | less`

Secciones de troubleshooting (problemas comunes)
-----------------------------------------------
- Error en `repo sync`: volvé a ejecutar `repo sync` o ejecutá `repo sync -j1` para reducir concurrencia y ver logs más claros.
- No existe la `MACHINE`: buscá en las carpetas `meta-*/conf/machine`. Si no la encontrás, cloná la BSP correcta o hacé una `machine` mínima copiando una `imx` distinta y adaptando parámetros.
- Kernel no compila: revisá logs en `tmp/work/.../temp/log.do_compile`. Podés necesitar backports o versiones de toolchain.
- Imagen no arranca: verificá particionado en WIC y la compatibilidad del U-Boot. Revisá que `u-boot` y `device tree` (DTB) correspondan a la placa.
- SSH no arranca: confirmá que `openssh` está instalado en la imagen y que `sshd` está habilitado en systemd (`systemctl enable sshd`).

Notas y recomendaciones finales
------------------------------
- El manifiesto `imx-manifest` de NXP es la fuente principal aquí; revisá el README del manifiesto para instrucciones específicas de la release `imx-linux-scarthgap`.
- Versiones: siempre fijá las ramas del manifiesto y las layers para que coincidan con la release que querés usar.
- Si querés un proceso más reproducible, puedo añadir un `script` en `yocto_projects/imx` que automatice la instalación de `repo`, inicialización del manifiesto y `repo sync`.

Checklist de entrega (verificá antes de cerrar)
-----------------------------------------------
- [ ] `conf/bblayers.conf` contiene las layers necesarias sincronizadas por `repo`.
- [ ] `conf/local.conf` tiene `MACHINE` correcto y `IMAGE_INSTALL` con ssh.
- [ ] `bitbake core-image-minimal` completa sin errores.
- [ ] La imagen grabada arranca en la placa y permite login por SSH.

Si querés, puedo:
- generar el script `clone-layers.sh` / `init-imx-repo.sh` que instale `repo`, inicialice `imx-manifest` en la rama `imx-linux-scarthgap` y haga `repo sync` (lo dejo listo para ejecutar dentro del contenedor), o
- buscar dentro del workspace sincronizado el script exacto de NXP para crear el `build` y dejar un ejemplo de uso preciso.

Decime cuál preferís y lo hago; si confirmás también la ruta exacta donde querés sincronizar (por ejemplo `yocto_projects/imx`), lo dejo tal cual y te genero el script listo para ejecutar dentro del contenedor.
