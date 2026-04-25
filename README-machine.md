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

---

### Fase 4: Configuración de la imagen (`local.conf`)

Editar `build/conf/local.conf` con estas claves:

```conf
MACHINE = "qemuarm64"
DISTRO ?= "poky"
IMAGE_FEATURES = "ssh-server-dropbear"
INHERIT += "extrausers rm_work"
EXTRA_USERS_PARAMS = "useradd -m -p '$6$88XM7bshHOZ/Q0bN$yGOqPCkJknC3DQWRbcZxYwZ4HgMUxJlSmv6vItIv3LdFWztQwnzcpQij7Ujs2jl22sNSe1NvUD3ITV0xWXxqD/' admin;"
```

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

### Fase 6: Verificación en emulador

Para correr el emulador QEMU desde el mismo contenedor y la misma sesión de Yocto:

```bash
runqemu qemuarm64 core-image-minimal nographic slirp
```

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
- El build solo aparece si Toaster se inició en la misma sesión donde se ejecuta `bitbake`.
- Si no aparece, seguir el log directo con:

```bash
docker exec pocoyocto-qemuarm64 tail -f /home/pocoyoctouser/output/bitbake.log
```
