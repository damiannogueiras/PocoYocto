# Bitácora de Operaciones - Agente `seguir-readme-machine`

## 26 de abril de 2026

### 1. Inicio de construcción `qemuarm64-v2`
- **Paso del README:** Fase 1b (creación de rama) y Fase 5b (construcción).
- **Acción:** Se verificó que el usuario creó la rama `bsp/qemuarm64-v2`.
- **Resultado:** OK.
- **Observaciones:** Se procede a levantar el entorno y lanzar la compilación.

### 2. Lanzamiento de entorno y compilación
- **Paso del README:** Fase 2 y Fase 5b.
- **Acción:**
  - `docker-compose up -d`
  - `docker exec pocoyocto-qemuarm64 bash -c "cd /home/pocoyoctouser/poky && source oe-init-build-env ../build && bitbake core-image-minimal-qemuarm64-v2"`
- **Resultado:** OK. La compilación está en curso.
- **Observaciones:** El proceso de `bitbake` se lanzó en modo asíncrono.

### 3. Arreglo de configuración de Toaster
- **Paso del README:** Notas de Toaster.
- **Acción:** Se detectó que la configuración de Toaster en `docker-compose.yaml` y `PocoYocto-env/docker-compose-template.yml` era incorrecta, causando que no se vieran las compilaciones lanzadas con `docker exec`. Se modificaron ambos archivos para simplificar el comando de inicio y depender de las variables de entorno.
- **Resultado:** OK. Los archivos fueron corregidos.
- **Observaciones:** El cambio tomará efecto cuando se reinicie el contenedor con `docker-compose down && docker-compose up -d`.

### 4. Compilación de `qemuarm64-v2` finalizada
- **Paso del README:** Fase 5b.
- **Acción:** El proceso de `bitbake core-image-minimal-qemuarm64-v2` finalizó.
- **Resultado:** OK.
- **Observaciones:** La imagen se generó correctamente. Se procede a la verificación en QEMU.

### 5. Fallo al reiniciar el contenedor tras fix de Toaster
- **Paso del README:** Reinicio de entorno.
- **Acción:** Se reinició el contenedor para aplicar los cambios de Toaster.
- **Resultado:** FALLA.
- **Error:** `bash: source: toaster: file not found`
- **Diagnóstico:** El comando en `docker-compose.yaml` intentaba hacer `source toaster` pero `toaster` es un script que solo está disponible después de que se hace `source oe-init-build-env`. La sintaxis del command estaba mal estructurada.

### 6. Error de permisos en bind-mounts al arrancar el contenedor
- **Paso del README:** Arranque del entorno.
- **Acción:** El contenedor intentaba ejecutar `chown -R pocoyoctouser:pocoyoctouser` sobre los directorios montados desde el host macOS (bind-mounts de `metas/`).
- **Resultado:** FALLA.
- **Error:** `chown: changing ownership of '/home/pocoyoctouser/build/metas/meta-openembedded/.git/objects/pack/...': Permission denied`
- **Causa raíz:** El UID del usuario `pocoyoctouser` dentro del contenedor (1000) no coincidía con el UID del usuario del host macOS (501). Docker en macOS no puede hacer `chown` sobre archivos de bind-mounts que pertenecen a otro UID.

### 7. Solución: UID mapping entre host y contenedor
- **Paso del README:** Configuración del entorno Docker.
- **Acción:**
  - Se modificó `PocoYocto-env/Dockerfile` para aceptar el build arg `HOST_UID` y crear `pocoyoctouser` con ese UID: `useradd -m -s /bin/bash -u ${HOST_UID} pocoyoctouser`
  - Se modificó `docker-compose.yaml` para pasar `HOST_UID: "${UID:-1000}"` como build arg, resuelto automáticamente del shell del host (UID=501 en macOS).
  - Se eliminó el `chown -R` sobre los bind-mounts del `command`, dejando solo el `chown` sobre el volumen Docker `output` y el directorio raíz de `build`.
  - Se aplicaron los mismos cambios en `PocoYocto-env/docker-compose-template.yml`.
  - Se reconstruyó la imagen: `docker-compose build --no-cache && docker-compose up -d`
- **Resultado:** OK. El contenedor arrancó correctamente.

### 8. Intento de arranque de imagen v2 en QEMU
- **Paso del README:** Fase 6 (verificación en QEMU).
- **Acción:** `runqemu qemuarm64 core-image-minimal-qemuarm64-v2 nographic`
- **Resultado:** FALLA inicial, luego parcial.
- **Error:** `runqemu - ERROR - IMAGE_LINK_NAME wasn't set to find corresponding .qemuboot.conf file`
- **Diagnóstico:** La variable `TMPDIR` configurada en las variables de entorno del `docker-compose.yaml` apuntaba a `/home/pocoyoctouser/output/tmp`, pero la compilación generó los artefactos en `/home/pocoyoctouser/build/tmp`. El `runqemu` no encontraba el `.qemuboot.conf`.
- **Workaround aplicado:** Pasar el path completo al `.qemuboot.conf`:
  `runqemu /home/pocoyoctouser/build/tmp/deploy/images/qemuarm64/core-image-minimal-qemuarm64-v2-qemuarm64.rootfs.qemuboot.conf nographic`
- **Estado actual:** FALLA. El workaround del path directo tampoco funcionó (ver paso 9).

### 9. Segundo intento QEMU - con `slirp` y nombre de imagen
- **Paso del README:** Fase 6 (verificación en QEMU).
- **Acciones intentadas:**
  1. `runqemu qemuarm64 core-image-minimal-qemuarm64-v2 nographic slirp` → mismo error `IMAGE_LINK_NAME wasn't set`
  2. `runqemu /path/completo/core-image-minimal-qemuarm64-v2-qemuarm64.rootfs.qemuboot.conf nographic` → error `Nothing PROVIDES 'None'` + `TUN /dev/net/tun unavailable`
- **Resultado:** FALLA.
- **Diagnóstico:** El `runqemu` no puede resolver el nombre de imagen `core-image-minimal-qemuarm64-v2` porque la variable `TMPDIR` configurada en el `docker-compose.yaml` apunta a `/home/pocoyoctouser/output/tmp`, pero la compilación generó los artefactos en `/home/pocoyoctouser/build/tmp` (el TMPDIR efectivo al momento de compilar era distinto). Por eso `IMAGE_LINK_NAME` queda vacía y `runqemu` no puede encontrar los artefactos.
- **Acción siguiente pendiente:** Corregir `TMPDIR` en `docker-compose.yaml` para que apunte a `/home/pocoyoctouser/build/tmp`, o bien volver a compilar con el `TMPDIR` correcto ya seteado, para que `runqemu` pueda resolver el nombre de imagen sin necesidad de paths absolutos.

---

## 28 de abril de 2026

### 10. Fallo al arrancar QEMU - binario `qemu-system-aarch64` no encontrado
- **Paso del README:** Fase 6 (verificación en QEMU).
- **Acción:** `runqemu /home/pocoyoctouser/build/tmp/deploy/images/qemuarm64/core-image-minimal-qemuarm64.rootfs.qemuboot.conf nographic slirp`
- **Resultado:** FALLA.
- **Error:** `No QEMU binary '/home/pocoyoctouser/build/tmp/work/aarch64-linux/qemu-helper-native/1.0/recipe-sysroot-native/usr/bin/qemu-system-aarch64' could be found`
- **Diagnóstico:** `runqemu` busca el binario `qemu-system-aarch64` en el sysroot nativo de la receta `qemu-helper-native`. Esa receta (y su dependencia `qemu-native`) no se buildea automáticamente como parte del build de la imagen; hay que compilarla explícitamente antes del primer `runqemu`.
- **Nota adicional:** El path `aarch64-linux/qemu-helper-native` es correcto: el contenedor corre sobre Apple Silicon (aarch64), por eso las recetas native se compilan para `aarch64-linux`.
- **Fix:**
  ```bash
  source /home/pocoyoctouser/poky/oe-init-build-env /home/pocoyoctouser/build
  bitbake qemu-helper-native
  ```
- **Estado:** OK. Tras buildear `qemu-helper-native`, QEMU arrancó correctamente y se llegó al prompt de login.

### 11. Errores en cadena de recetas con sstate corrupto
- **Paso del README:** Fase 5b (recompilación desde cero).
- **Acciones:** Se intentó recompilar `core-image-minimal-qemuarm64-v2` tras un reset del contenedor. Fallaron en cadena: `python3` (undefined references en deepfreeze), `icu` (PKG_PROG_PKG_CONFIG sin expandir), `dbus` y `libxml2` (JSON vacío en do_create_spdx), `libsm` (sstate manifest faltante).
- **Causa raíz:** Múltiples builds previas interrumpidas dejaron artefactos parciales en `tmp/`. El `cleansstate` receta por receta no alcanzaba porque los manifests de sstate compartidos también estaban corruptos.
- **Fix definitivo:** Borrado completo de `tmp/` y recompilación desde cero:
  ```bash
  rm -rf /home/pocoyoctouser/build/tmp
  bitbake core-image-minimal-qemuarm64-v2
  ```
- **Fix adicional:** Se deshabilitó `create-spdx` agregando `INHERIT:remove = "create-spdx"` en `local.conf` para evitar fallos por JSONs corruptos en entornos de demo.
- **Resultado:** OK. Build completo exitoso (4895 tareas). Artefactos generados en `/home/pocoyoctouser/build/tmp/deploy/images/qemuarm64/`.
- **Observación:** El `.qemuboot.conf` generado se llama `core-image-minimal-qemuarm64-v2-qemuarm64.qemuboot.conf` (sin `.rootfs.` en el nombre). Se actualizó el README-machine.md con el path correcto.

### 12. Verificación final en QEMU - OK
- **Paso del README:** Fase 6 (verificación en QEMU).
- **Acción:** Se arrancó QEMU con el `.qemuboot.conf` correcto y se verificó el funcionamiento del cron de `marcatemporal`.
- **Resultado:** OK.
- **Observaciones:**
  - Login con `admin` / `pinux` exitoso.
  - `/home/admin/marcatemporal.txt` se actualiza cada ~15 segundos como esperado.
  - El warning `Nothing PROVIDES 'None'` al arrancar `runqemu` es inofensivo — ocurre porque `IMAGE_LINK_NAME` no está resuelto en el `.qemuboot.conf`. No afecta el arranque.
- **Estado:** VERIFICACIÓN COMPLETA. La imagen `qemuarm64-v2` funciona correctamente.
