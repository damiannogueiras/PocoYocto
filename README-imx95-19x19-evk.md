# README - i.MX95 19x19 EVK (sistema mínimo, sin GUI)

Resumen
-------
Plan paso a paso para construir una imagen Yocto mínima para la placa i.MX95 19x19 EVK. Objetivo: sistema sin entorno gráfico, solo terminal con herramientas básicas y acceso remoto (SSH). Este documento usa la herramienta `repo` para sincronizar las layers oficiales de NXP desde el manifiesto `imx-manifest` (rama `imx-linux-scarthgap`) en lugar de depender de un `poky` upstream separado.

Contrato (entrada / salida / éxito)
----------------------------------
- Entrada: acceso a internet para usar `repo` y sincronizar el manifiesto NXP `imx-manifest` en `yocto_projects/imx` (u otra ruta equivalente).
- Salida: una imagen mínima arrancable para i.MX95 EVK (SD card / eMMC image) que arranca a shell, con SSH y utilidades básicas instaladas.
- Criterio de éxito: la placa arranca, se accede por consola serie y se puede iniciar sesión por SSH.

Supuestos importantes (los dejo explícitos)
-----------------------------------------
- No vamos a usar un `poky` independiente; en su lugar usaremos el manifiesto de NXP `https://github.com/nxp-imx/imx-manifest` y su rama `imx-linux-scarthgap` para sincronizar todas las layers necesarias.
- Asumo que clonás/sincronizás el manifiesto debajo de `yocto_projects/imx` dentro del contenedor Docker del repo. Si preferís otra ruta, adaptá los comandos.
- No conozco con certeza el nombre exacto de la `MACHINE` para tu board; propongo usar `imx95-19x19-evk` como `MACHINE` objetivo; si no existe, te indico cómo detectarlo o crear una máquina personalizada.
- Yocto/NXP manifest: ajustá la rama del manifiesto si necesitás otra (ej. otra release de NXP).

Checklist rápido (lo que vas a hacer)
-------------------------------------
- [ ] Preparar entorno Docker y entrar en shell del contenedor.
- [ ] Inicializar `repo` con el manifiesto `imx-manifest` (rama `imx-linux-scarthgap`) y `repo sync`.
- [ ] Revisar las layers sincronizadas y detectar la `MACHINE` correcta o crear una propia.
- [ ] Crear directorio de build y configurar `bblayers.conf` y `local.conf` si es necesario.
- [ ] Compilar imagen mínima (core-image-minimal o imagen personalizada con SSH).
- [ ] Flashear imagen en SD/eMMC y pruebas en placa.

Pasos detallados
----------------
1) Preparar el entorno (en tu mac: usar Docker como indica el repo)
   - Asegurate de tener el archivo `.env` en la raíz con `YOCTO_PASS` y levantá el contenedor según `PocoYocto-env/Readme.md`.
   - Dentro de macOS (no ejecutar aquí, solo referencia):

```bash
# desde la raíz del repo (host mac)
docker compose up -d
# entrar al contenedor ya levantado
docker exec -it yocto-minimal bash
```

   - Dentro del contenedor, creá el directorio para el trabajo con NXP y movete a él:

```bash
mkdir -p /home/yoctouser/yocto_projects/imx
cd /home/yoctouser/yocto_projects/imx
```

2) Instalar y usar `repo` para sincronizar el manifiesto NXP
   - Si no tenés la herramienta `repo`, instalala (ejemplo):

```bash
# instalar repo (si no está disponible)
curl https://storage.googleapis.com/git-repo-downloads/repo > /usr/local/bin/repo
chmod a+x /usr/local/bin/repo
```

   - Inicializá el workspace con el manifiesto de NXP y sincronizá las layers (usá la rama `imx-linux-scarthgap`):

```bash
repo init -u https://github.com/nxp-imx/imx-manifest -b imx-linux-scarthgap
repo sync -j$(nproc)
```

   - Esto clonará todas las layers y repositorios definidos en el manifiesto. Tardará varios minutos según la conexión.

3) Revisar las capas y localizar scripts de configuración
   - El manifiesto de NXP suele incluir scripts de inicialización o instrucciones para preparar el entorno. Buscá scripts como `setup-environment`, `fsl-setup-release.sh`, `imx-setup-release.sh` u otros que el manifiesto provea:

```bash
# buscar scripts típicos en el workspace sincronizado
grep -R "setup-" -n . || true
ls -la | sed -n '1,200p'
```

   - Si encontrás un script de entorno (por ejemplo `setup-environment` o `fsl-setup-release.sh`), leé su cabecera y usalo para crear el build dir.

4) Crear y entrar al directorio de build usando el script del manifest
   - Si el manifiesto proporciona un script estándar para inicializar el build, ejecutalo. Ejemplo genérico (adaptá la ruta si el script está en otra carpeta):

```bash
# ejemplo genérico: buscar el script y usarlo
source ./scripts/fsl-setup-release.sh -b build-imx95 2>/dev/null || true
# O, si existe setup-environment:
# source ./setup-environment build-imx95

# Si no hay script, podés crear el directorio y usar bitbake directamente:
mkdir -p build-imx95
cd build-imx95
```

   - Nota: el script del manifiesto normalmente exporta `TOPDIR`/`OECORE` y prepara `conf/`.

5) Revisar `conf/bblayers.conf` y `conf/local.conf`
   - Si el script de NXP ya generó `conf/`, revisá `conf/bblayers.conf` para confirmar que incluye las layers sincronizadas por `repo`. Si no, editá y añadí las rutas a las layers que necesites.

Ejemplo mínimo para `bblayers.conf` (asegurate de usar las rutas reales relativas al `TOPDIR`):

```conf
# ...existing code...
BBLAYERS ?= " \
  ${TOPDIR}/../meta \
  ${TOPDIR}/../meta-freescale \
  ${TOPDIR}/../meta-freescale-boards \
  ${TOPDIR}/../meta-oe \
  ${TOPDIR}/../meta-python \
  ${TOPDIR}/../meta-networking \
"
# ...existing code...
```

   - Editá `conf/local.conf` y ajustá estas variables mínimas (ejemplo):

```conf
MACHINE ?= "imx95-19x19-evk"  # confirmar que exista en las capas sincronizadas
DISTRO ?= "poky"
DL_DIR ?= "${TOPDIR}/downloads"
SSTATE_DIR ?= "${TOPDIR}/sstate-cache"
TMPDIR = "${TOPDIR}/tmp"

IMAGE_INSTALL_append = " openssh-sftp-server busybox-locales iproute-tc iputils-ping"
EXTRA_IMAGE_FEATURES += "ssh-server-openssh"

DISTRO_FEATURES_append = " systemd"
VIRTUAL-RUNTIME_init_manager = "systemd"

PREFERRED_PROVIDER_virtual/kernel = "linux-imx"
PREFERRED_PROVIDER_virtual/bootloader = "u-boot"

BB_NUMBER_THREADS ?= "$(nproc)"
PARALLEL_MAKE ?= "-j$(nproc)"
```

   - Adaptá `DISTRO` si el manifiesto indica un `distro` preferido distinto.

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
