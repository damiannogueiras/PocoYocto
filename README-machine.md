# Configuración para qemuarm64-v1

## Introducción

Este documento describe cómo configurar y construir una imagen Yocto para la máquina emulada `qemuarm64`. El objetivo es generar una imagen mínima con solo SSH y sin entorno gráfico.

---

### Fase 1: Creación de rama

- Rama base: `init`
- Nueva rama: `bsp/qemuarm64-v1`
- Comando:

```bash
git checkout -b bsp/qemuarm64-v1 init
```

---

### Fase 1b: Nueva versión v2

La versión `qemuarm64-v2` hereda todo el comportamiento de `v1` y añade un paquete extra llamado `marcatemporal` que instala:

- un script Python que registra la hora en `/home/admin/marcatemporal.txt`
- una entrada de cron que se ejecuta cada 15 segundos

La configuración base de `local.conf` no cambia: se usa la misma `MACHINE`, la misma `DISTRO` y la misma imagen mínima, pero el objetivo de BitBake es distinto.

- Nueva rama sugerida: `bsp/qemuarm64-v2`
- Comando:

```bash
git checkout -b bsp/qemuarm64-v2 bsp/qemuarm64-v1
```

---

### Fase 2: Lanzamiento del contenedor de entorno

1. Copiar o crear el archivo `docker-compose.yaml` en el directorio raíz del repositorio.
2. Iniciar el contenedor:

```bash
docker-compose up -d
```

3. El contenedor se lanza como `pocoyocto-qemuarm64` y Toaster se expone en `http://localhost:8001`.

4. Acceder al contenedor:

```bash
docker exec -it pocoyocto-qemuarm64 bash
```

5. Inicializar el entorno Yocto:

```bash
cd /home/pocoyoctouser/poky
source oe-init-build-env ../build
```

---

### Fase 3: Configuración de capas (`bblayers.conf`)

Asegurarse de que `build/conf/bblayers.conf` incluya al menos las siguientes capas:

- `/home/pocoyoctouser/poky/meta`
- `/home/pocoyoctouser/poky/meta-poky`
- `/home/pocoyoctouser/poky/meta-yocto-bsp`
- `/home/pocoyoctouser/build/metas/meta-openembedded/meta-oe`
- `/home/pocoyoctouser/build/metas/meta-openembedded/meta-networking`
- `/home/pocoyoctouser/build/metas/meta-openembedded/meta-python`
- `/home/pocoyoctouser/build/metas-propias/meta-template`

> Importante para `qemuarm64-v2`: esta capa contiene la receta `marcatemporal` y la receta de imagen `core-image-minimal-qemuarm64-v2`.

---

### Fase 4: Configuración de la imagen (`local.conf`)

Editar `build/conf/local.conf` con estas claves:

```conf
MACHINE = "qemuarm64"
DISTRO ?= "poky"
IMAGE_FEATURES = "ssh-server-dropbear"
INHERIT += "extrausers rm_work"
EXTRA_USERS_PARAMS = "useradd -m -p '$6$88XM7bshHOZ/Q0bN$yGOqPCkJknC3DQWRbcZxYwZ4HgMUxJlSmv6vItIv3LdFWztQwnzcpQij7Ujs2jl22sNSe1NvUD3ITV0xWXxqD/' admin;"
INHERIT:remove = "create-spdx"
```

- `INHERIT:remove = "create-spdx"`: deshabilita la generación de manifests SPDX. En entornos de demo o desarrollo esto evita fallos intermitentes por artefactos corruptos de builds anteriores.

- `MACHINE = "qemuarm64"`: emulación ARM 64 en QEMU.
- `ssh-server-dropbear`: habilita acceso SSH.
- `extrausers`: crea el usuario `admin` con contraseña `pinux`.
- `rm_work`: reduce el uso de disco durante la compilación.

---

### Fase 5: Construcción de la imagen

Desde el directorio de build:

```bash
bitbake core-image-minimal
```

---

### Fase 5b: Construcción de la versión v2

La nueva versión `v2` es igual a la v1 pero añade el script Python y el cron de 15 segundos. Eso se logra con la receta de imagen `core-image-minimal-qemuarm64-v2`, que instala además `python3`, `cronie` y `marcatemporal`.

- El script guarda el timestamp cada 15 segundos en `/home/admin/marcatemporal.txt`.
- No es necesario cambiar `local.conf` salvo `MACHINE` y `DISTRO` si aún no están definidos.
- Asegurate de que `meta-template` esté presente en `bblayers.conf`.

```bash
bitbake core-image-minimal-qemuarm64-v2
```


Verificar que el kernel fue deployado

```bash
ls /home/pocoyoctouser/build/tmp/deploy/images/qemuarm64/Image
```

Si no existe, forzar el deploy del kernel (mucho más rápido que una build completa)

```bash
bitbake virtual/kernel
```
---

### Fase 6: Verificación en emulador

Antes de lanzar QEMU por primera vez, compilar la receta nativa que provee el binario `qemu-system-aarch64`. No se genera automáticamente como parte de la imagen:

```bash
bitbake qemu-helper-native
```

Para correr el emulador QEMU desde el mismo contenedor y la misma sesión de Yocto, arrancar usando el `.qemuboot.conf` generado:

```bash
runqemu /home/pocoyoctouser/build/tmp/deploy/images/qemuarm64/core-image-minimal-qemuarm64-v2-qemuarm64.qemuboot.conf nographic slirp
```

> Nota: el nombre del archivo varía según la versión compilada. Verificar el nombre exacto con:
> ```bash
> ls /home/pocoyoctouser/build/tmp/deploy/images/qemuarm64/*.qemuboot.conf
> ```

- `nographic`: no usa ventana gráfica.
- `slirp`: redirección de red user-mode.

Con el emulador corriendo, conectarse desde el host macOS:

```bash
ssh -p 2222 admin@localhost
```

Contraseña: `pinux`


---

### Notas de Toaster

- Toaster corre en `http://localhost:8001`.
- El build solo aparece si la sesión de `bitbake` tiene la misma variable `TOASTER_DIR` que la sesión donde arrancó Toaster.
- Si no aparece, seguir el log directo con:

```bash
docker exec pocoyocto-qemuarm64 tail -f /home/pocoyoctouser/output/bitbake.log
```
