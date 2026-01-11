# Guía: Imagen Yocto para Raspberry Pi Zero W con solo terminal y SSH por WiFi

Objetivo: construir una imagen Yocto para **Raspberry Pi Zero W** que:

- Arranque en **modo solo consola** (sin entorno gráfico).
- Habilite **SSH** desde el primer arranque.
- Se conecte automáticamente a la WiFi con:
  - SSID: `wifi`
  - Contraseña: `perlitalagatita`

La guía está escrita para que un agente de IA pueda ejecutarla paso a paso.

---

## 0. Suposiciones

- Ya tienes este repositorio clonado en el host (macOS).
- Tienes Docker instalado y funcionando.
- El repositorio contiene:
  - `Entorno/Dockerfile` y/o `docker-compose.yml`
  - `yocto_projects/poky` (checkout de Poky).
- Todo el trabajo de Yocto se hace **dentro del contenedor**, no directamente en macOS.

Si algo de esto no es cierto, ajusta la ruta o los comandos según tu entorno.

---

## 1. Arrancar el entorno de desarrollo (host macOS)

### 1.1. Ir al directorio del repositorio

```bash
cd /Users/mini/Damian/Yocto
```

### 1.2. Levantar el contenedor con Docker Compose

```bash
docker compose up -d
```

- Si falla, revisa que exista un `docker-compose.yml` en este directorio.
- El contenedor resultante se asume que se llama `yocto-minimal` (ajusta el nombre si es distinto).

### 1.3. Entrar al contenedor

```bash
docker exec -it yocto-minimal bash
```

Dentro del contenedor, el directorio de trabajo será:

```bash
cd /home/yoctouser/yocto_projects
```

---

## 2. Preparar Poky y las capas necesarias

Todo lo que sigue es **dentro del contenedor**.

### 2.1. Asegurarse de que existe `poky`

```bash
cd /home/yoctouser/yocto_projects
ls
```

Debe aparecer un directorio `poky`. Si no existe, habría que clonarlo (no se detalla aquí porque ya debería venir preparado en este entorno).

### 2.2. Clonar la capa `meta-raspberrypi`

```bash
cd /home/yoctouser/yocto_projects
git clone git://git.yoctoproject.org/meta-raspberrypi
```

> Si prefieres GitHub, podrías usar:
> `git clone https://github.com/agherzan/meta-raspberrypi.git`

### 2.3. (Opcional, pero recomendable) Clonar meta-openembedded

Esto da acceso a paquetes adicionales si hicieran falta:

```bash
cd /home/yoctouser/yocto_projects
git clone https://github.com/openembedded/meta-openembedded.git
```

---

## 3. Inicializar el directorio de build de Yocto

### 3.1. Ejecutar `oe-init-build-env`

```bash
cd /home/yoctouser/yocto_projects/poky
source oe-init-build-env
```

Este comando:

- Exporta variables de entorno.
- Crea (si no existe) y entra en el directorio de build, típicamente `build/`.

Después de ejecutarlo, deberías estar en algo como:

```bash
pwd
# /home/yoctouser/yocto_projects/poky/build
```

En adelante, se asume que estás en ese directorio `build/`.

---

## 4. Configurar capas en `bblayers.conf`

Archivo: `conf/bblayers.conf` dentro del `build/`.

### 4.1. Crear `bblayers.conf`

```bash
cd /home/yoctouser/yocto_projects/poky/build

cat > conf/bblayers.conf << 'EOF'
# POKY_BBLAYERS_CONF_VERSION is increased each time build/conf/bblayers.conf
# changes incompatibly
POKY_BBLAYERS_CONF_VERSION = "2"

BBPATH = "${TOPDIR}"
BBFILES ?= ""

BBLAYERS ?= " \
  /home/yoctouser/yocto_projects/poky/meta \
  /home/yoctouser/yocto_projects/poky/meta-poky \
  /home/yoctouser/yocto_projects/meta-raspberrypi \
  /home/yoctouser/yocto_projects/meta-openembedded/meta-oe \
"
EOF
```

Ajusta rutas si las capas están en otro sitio, pero con el layout estándar del repo deberían ser correctas.

---

## 5. Crear una capa propia para la imagen y la configuración WiFi

Crearemos una capa llamada `meta-mini` para:

- Definir una receta de imagen personalizada.
- Configurar la WiFi `wifi` / `perlitalagatita`.

### 5.1. Crear la capa con `bitbake-layers`

```bash
cd /home/yoctouser/yocto_projects/poky/build
bitbake-layers create-layer ../meta-mini
```

Esto debería crear `/home/yoctouser/yocto_projects/meta-mini`.

### 5.2. Añadir `meta-mini` al `bblayers.conf`

```bash
cd /home/yoctouser/yocto_projects/poky/build
bitbake-layers add-layer ../meta-mini
```

Puedes verificar que aparece en `conf/bblayers.conf` (última línea del `BBLAYERS`).

---

## 6. Configurar la máquina, SSH y WiFi en `local.conf`

Archivo: `conf/local.conf`.

### 6.1. Sobrescribir `local.conf` con la configuración deseada

```bash
cd /home/yoctouser/yocto_projects/poky/build

cat > conf/local.conf << 'EOF'
MACHINE ?= "raspberrypi0-wifi"

DISTRO ?= "poky"
PACKAGE_CLASSES ?= "package_rpm"
EXTRA_IMAGE_FEATURES ?= "debug-tweaks"

# Carpeta temporal en HOME (suele evitar problemas de permisos)
TMPDIR = "${HOME}/tmp"

# Número de tareas de compilación paralelas (ajustar según CPU)
BB_NUMBER_THREADS ?= "4"
PARALLEL_MAKE ?= "-j 4"

# Tipo de imagen: queremos una imagen para SD de Raspberry Pi
IMAGE_FSTYPES += " rpi-sdimg"

# Activar servidor SSH ligero (dropbear) en la imagen
EXTRA_IMAGE_FEATURES += " ssh-server-dropbear"

# Paquetes adicionales para red inalámbrica
IMAGE_INSTALL:append = " wpa-supplicant wireless-tools iw"

# Habilitar UART (útil para consola serie)
ENABLE_UART = "1"
EOF
```

Si tu máquina host es muy potente puedes subir los valores de `BB_NUMBER_THREADS` y `PARALLEL_MAKE`.

---

## 7. Añadir receta para la configuración WiFi y servicio

Ahora vamos a:

- Crear un paquete `wifi-config-mini` que instale `/etc/wpa_supplicant/wpa_supplicant.conf` con el SSID y clave.
- Hacer que `wpa_supplicant` arranque automáticamente para `wlan0`.

### 7.1. Crear el árbol de directorios en `meta-mini`

```bash
cd /home/yoctouser/yocto_projects/meta-mini

mkdir -p recipes-connectivity/wifi-config-mini/files
```

### 7.2. Crear el archivo de configuración de `wpa_supplicant`

Ruta de destino en el sistema: `/etc/wpa_supplicant/wpa_supplicant.conf`.

```bash
cd /home/yoctouser/yocto_projects/meta-mini

cat > recipes-connectivity/wifi-config-mini/files/wpa_supplicant.conf << 'EOF'
ctrl_interface=/var/run/wpa_supplicant
update_config=1
country=ES

network={
    ssid="wifi"
    psk="perlitalagatita"
    key_mgmt=WPA-PSK
}
EOF
```

> **Nota:**  
> Cambia `country=ES` si necesitas otro país.

### 7.3. Crear el servicio systemd para `wpa_supplicant` en `wlan0`

Archivo: `wpa_supplicant@wlan0.service`.

```bash
cd /home/yoctouser/yocto_projects/meta-mini

cat > recipes-connectivity/wifi-config-mini/files/wpa_supplicant@wlan0.service << 'EOF'
[Unit]
Description=WPA supplicant for wlan0
After=network.target

[Service]
Type=simple
ExecStart=/sbin/wpa_supplicant -i wlan0 -c /etc/wpa_supplicant/wpa_supplicant.conf -Dwext
Restart=always

[Install]
WantedBy=multi-user.target
EOF
```

> Dependiendo de la versión del driver puede ser mejor `-Dnl80211`.  
> Si la conexión no funciona, cámbialo y reconstruye la imagen:
> `-Dwext` → `-Dnl80211`.

### 7.4. Crear la receta `wifi-config-mini.bb`

```bash
cd /home/yoctouser/yocto_projects/meta-mini

cat > recipes-connectivity/wifi-config-mini/wifi-config-mini.bb << 'EOF'
SUMMARY = "Configuración WiFi automática para Raspberry Pi Zero W"
DESCRIPTION = "Instala wpa_supplicant.conf y habilita wpa_supplicant@wlan0.service"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://wpa_supplicant.conf;md5=0e5751c026e543b2e8b5f6ad1cd9d0f5"

SRC_URI = " \
    file://wpa_supplicant.conf \
    file://wpa_supplicant@wlan0.service \
"

S = "${WORKDIR}"

inherit systemd

do_install() {
    # Configuración de wpa_supplicant
    install -d ${D}/etc/wpa_supplicant
    install -m 0600 ${WORKDIR}/wpa_supplicant.conf ${D}/etc/wpa_supplicant/wpa_supplicant.conf

    # Servicio systemd
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/wpa_supplicant@wlan0.service ${D}${systemd_system_unitdir}/wpa_supplicant@wlan0.service
}

SYSTEMD_PACKAGES = "${PN}"
SYSTEMD_SERVICE:${PN} = "wpa_supplicant@wlan0.service"

FILES:${PN} += " \
    /etc/wpa_supplicant/wpa_supplicant.conf \
    ${systemd_system_unitdir}/wpa_supplicant@wlan0.service \
"
EOF
```

> El `md5` en `LIC_FILES_CHKSUM` es inventado aquí y solo sirve como placeholder para que el ejemplo sea sintácticamente válido; en un flujo estricto deberías ajustarlo, pero para uso personal normalmente no será un problema.

### 7.5. Añadir `wifi-config-mini` a la imagen

Volvemos a `local.conf` para asegurarnos de que el paquete se incluye en la imagen.

```bash
cd /home/yoctouser/yocto_projects/poky/build

# Añadir wifi-config-mini a IMAGE_INSTALL
# (esto añade una línea al final de local.conf, sin sobrescribir)
echo 'IMAGE_INSTALL:append = " wifi-config-mini"' >> conf/local.conf
```

---

## 8. Crear una receta de imagen personalizada

Aunque podríamos usar `core-image-minimal`, es más claro tener una imagen propia.

### 8.1. Crear la receta de imagen

```bash
cd /home/yoctouser/yocto_projects/meta-mini

mkdir -p recipes-core/images
cat > recipes-core/images/rpi0w-ssh-wifi-image.bb << 'EOF'
DESCRIPTION = "Imagen mínima para Raspberry Pi Zero W con SSH y WiFi configurada"
LICENSE = "MIT"

inherit core-image

IMAGE_FEATURES += "ssh-server-dropbear"

IMAGE_INSTALL:append = " \
    wifi-config-mini \
"
EOF
```

Esta imagen:

- Incluye las características de `core-image`.
- Añade el servidor SSH.
- Asegura que `wifi-config-mini` (y por tanto la configuración WiFi) está instalada.

---

## 9. Construir la imagen

### 9.1. Volver a cargar el entorno (si abrimos una nueva shell)

Si has salido del contenedor o de la shell, vuelve a hacer:

```bash
cd /home/yoctouser/yocto_projects/poky
source oe-init-build-env
```

### 9.2. Lanzar `bitbake` de la nueva imagen

```bash
cd /home/yoctouser/yocto_projects/poky/build
bitbake rpi0w-ssh-wifi-image
```

Este paso tardará bastante (especialmente la primera vez).

Al terminar, la imagen para SD debería estar en una ruta similar a:

```text
tmp/deploy/images/raspberrypi0-wifi/rpi0w-ssh-wifi-image-raspberrypi0-wifi.rpi-sdimg
```

---

## 10. Grabar la imagen en la tarjeta SD (host macOS)

Este paso se hace **fuera del contenedor**, de vuelta en macOS.

### 10.1. Localizar el fichero `.rpi-sdimg`

Por ejemplo, cópialo desde el contenedor al host (si hace falta) o monta un volumen.  
Una opción simple (desde el host) es usar `docker cp`:

```bash
# En el host (no dentro del contenedor):
cd /Users/mini/Damian/Yocto

# Copiar el archivo desde el contenedor al host
docker cp yocto-minimal:/home/yoctouser/yocto_projects/poky/build/tmp/deploy/images/raspberrypi0-wifi/rpi0w-ssh-wifi-image-raspberrypi0-wifi.rpi-sdimg .
```

Ahora deberías tener el archivo `.rpi-sdimg` en `/Users/mini/Damian/Yocto`.

### 10.2. Identificar la tarjeta SD

Inserta la SD en el Mac y ejecuta:

```bash
diskutil list
```

Localiza el disco correspondiente, por ejemplo `/dev/disk4`.  
**Cuidado**: asegúrate de identificar correctamente la SD para no borrar el disco equivocado.

### 10.3. Desmontar la SD (sin expulsarla)

```bash
diskutil unmountDisk /dev/disk4
```

(Ajusta `disk4` según el valor real.)

### 10.4. Grabar la imagen con `dd`

```bash
cd /Users/mini/Damian/Yocto

sudo dd if=rpi0w-ssh-wifi-image-raspberrypi0-wifi.rpi-sdimg of=/dev/rdisk4 bs=4m conv=sync
sudo sync
```

- Usa `/dev/rdisk4` (nota la `r`) para mayor velocidad.
- Ajusta el número de disco (`4`) según lo que hayas visto en `diskutil list`.

---

## 11. Primer arranque y conexión por SSH

### 11.1. Arrancar la Raspberry Pi Zero W

1. Inserta la SD en la Raspberry Pi Zero W.
2. Conecta la alimentación.
3. Espera unos minutos a que:
   - El sistema arranque.
   - `wlan0` se conecte a la WiFi `wifi`.
   - El servidor SSH esté disponible.

### 11.2. Encontrar la IP de la Raspberry

Tienes varias opciones:

- Mirar en el router (lista de clientes DHCP).
- Escanear la red desde tu Mac, por ejemplo con `nmap`:

```bash
# Ejemplo para red 192.168.1.0/24 (ajusta según tu red)
nmap -sn 192.168.1.0/24
```

Identifica la IP asociada a la Raspberry (por MAC, nombre de host, etc.).

### 11.3. Conectarte por SSH

Por defecto, muchas imágenes de Yocto usan `root` sin contraseña (o con una contraseña definida en la configuración, según el `DISTRO`).

Prueba:

```bash
ssh root@IP_DE_LA_RASPBERRY
```

Sustituye `IP_DE_LA_RASPBERRY` por la IP real.  
Si se queja por clave de host, acepta la huella si es la primera vez.

---

## 12. Resumen rápido del flujo

1. **Host**: levantar contenedor (`docker compose up -d`) y entrar (`docker exec -it yocto-minimal bash`).
2. **Contenedor**:
   - Clonar `meta-raspberrypi` (y `meta-openembedded`).
   - `source oe-init-build-env` para crear `build/`.
   - Configurar `bblayers.conf` con Poky + meta-raspberrypi + meta-oe + meta-mini.
   - Configurar `local.conf` para `MACHINE = raspberrypi0-wifi`, SSH y paquetes WiFi.
   - Crear capa `meta-mini` con:
     - Receta `wifi-config-mini` (wpa_supplicant + servicio systemd).
     - Receta de imagen `rpi0w-ssh-wifi-image`.
   - `bitbake rpi0w-ssh-wifi-image`.
3. **Host**:
   - Copiar `.rpi-sdimg` desde el contenedor.
   - Grabar en SD con `dd`.
4. **Raspberry Pi Zero W**:
   - Arrancar con la SD.
   - Se conecta a WiFi `wifi` (pass `perlitalagatita`).
   - Conectarte por SSH como `root`.

---

Si quieres, en un segundo documento podemos añadir una sección para **endurecer la seguridad** (cambiar contraseña de root, claves SSH, firewall, etc.) y otra para **añadir tu aplicación Node.js** encima de esta base.
