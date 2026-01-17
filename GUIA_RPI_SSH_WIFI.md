# Guía: Imagen Yocto para Raspberry Pi 4 (64-bit) con SSH y WiFi

Objetivo: construir una imagen Yocto para **Raspberry Pi 4 (64-bit)** que:

- Arranque en **modo solo consola** (sin entorno gráfico).
- Habilite **SSH** desde el primer arranque (con usuario `root` sin contraseña).
- Se conecte automáticamente a la WiFi con:
  - SSID: `wifi`
  - Contraseña: `perlitalagatita`
- Compile los menos paquetes posibles y limpie la caché durante la construcción.

La guía está escrita para que un agente de IA pueda ejecutarla paso a paso.

---

## 0. Suposiciones

- Ya tienes este repositorio clonado en el host (macOS).
- Tienes Docker instalado y funcionando.
- El repositorio contiene:
  - `PocoYocto-env/Dockerfile` y `docker-compose.yml`
  - `yocto_projects/poky` (checkout de Poky).
- Todo el trabajo de Yocto se hace **dentro del contenedor**, no directamente en macOS.
- El directorio `yocto_projects` se corresponde con el `/home/yoctouser/yocto_projects` del contenedor
- Todos los ficheros generados por Yocto estan en un volumen Docker `yocto-output`

Si algo de esto no es cierto, ajusta la ruta o los comandos según tu entorno.

---

## 1. Arrancar el entorno de desarrollo

### 1.1. Ir al directorio del repositorio

```bash
cd ~/PocoYocto
```

### 1.2. Levantar el contenedor con Docker Compose

```bash
docker compose up -d
```

- Si falla, revisa que exista un `docker-compose.yml` en este directorio.
- El contenedor resultante se asume que se llama `pocoyocto` (ajusta el nombre si es distinto).

### 1.3. Entrar al contenedor

```bash
docker exec -it pocoyocto bash
```

Dentro del contenedor, el directorio de trabajo será:

```bash
cd /home/yoctouser/yocto_projects
```

---

## 2. Preparar Poky y las capas necesarias

### 2.1. Asegurarse de que existe `poky` (dentro contenedor)

```bash
cd /home/yoctouser/yocto_projects
ls
```

Debe aparecer un directorio `poky`. Si no existe, habría que clonarlo (no se detalla aquí porque ya debería venir preparado).

### 2.2. Clonar como submodulo la capa `meta-raspberrypi` (fuera del contenedor, directorio 'metas')

```bash
cd ./metas
git submodule add -b kirkstone https://github.com/agherzan/meta-raspberrypi.git
```

---
// TODO hacerlo en el volumen de Docker
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

---

## 4. Configurar capas en `bblayers.conf`

Archivo: `conf/bblayers.conf` dentro del `build/`.

```bash
cd /home/yoctouser/yocto_projects/poky/build

cat > conf/bblayers.conf << 'EOF'
POKY_BBLAYERS_CONF_VERSION = "2"

BBPATH = "${TOPDIR}"
BBFILES ?= ""

BBLAYERS ?= " \
  /home/yoctouser/yocto_projects/poky/meta \
  /home/yoctouser/yocto_projects/poky/meta-poky \
  /home/yoctouser/yocto_projects/meta-raspberrypi \
"
EOF
```

---

## 5. Configurar todo en `local.conf` (la magia está acá, boludo)

Archivo: `conf/local.conf`.

Este es el archivo más importante. Acá le decimos a Yocto:
- Qué máquina es (Raspberry Pi 4 64-bit).
- Que habilite SSH.
- Que instale lo necesario para WiFi.
- Dónde guardar los archivos temporales (sin quilombos de permisos).
- Que habilite systemd como sistema de init.
- Que borre el trabajo intermedio para ahorrar espacio.

```bash
cd /home/yoctouser/yocto_projects/poky/build

cat > conf/local.conf << 'EOF'
# ============ MÁQUINA ============ 
# Raspberry Pi 4 de 64 bits
MACHINE ?= "raspberrypi4-64"
DISTRO ?= "poky"

# ============ BUILD ============ 
# Optimización: compilar paquetes como .deb y limpiar el directorio de trabajo después de cada receta
PACKAGE_CLASSES ?= "package_deb"
INHERIT += "rm_work"

BB_NUMBER_THREADS ?= "6"
PARALLEL_MAKE ?= "-j 6"
TMPDIR = "/tmp/yocto-build"

# ============ IMAGEN: TIPO Y FORMATO ============ 
IMAGE_FSTYPES += "rpi-sdimg"
# Para una imagen mínima, no añadir 'debug-tweaks' a menos que sea necesario para depurar
# IMAGE_FEATURES += "debug-tweaks"

# ============ SYSTEMD ============ 
# Habilitar systemd como sistema de init
DISTRO_FEATURES:append = " systemd"
VIRTUAL-RUNTIME_init_manager = "systemd"

# ============ SSH: SERVIDOR DROPBEAR (LIGERO) ============ 
# Esto habilita SSH en la imagen de forma automática
EXTRA_IMAGE_FEATURES += "ssh-server-dropbear"

# ============ PAQUETES WIFI ============ 
# Instala todo lo necesario para conectarse a redes WiFi
IMAGE_INSTALL:append = " \
    wpa-supplicant \
    iw \
    wifi-config \
"

# ============ HARDWARE ============ 
# Habilita puerto UART (útil para depuración via consola serie)
ENABLE_UART = "1"

# ============ CREDENCIALES (para desarrollo, che) ============ 
# Root sin contraseña (cámbialo en producción)
EXTRA_IMAGE_FEATURES += "allow-empty-password"
EXTRA_IMAGE_FEATURES += "allow-root-login"

# Para que el servicio de configuración WiFi se inicie automáticamente
SYSTEMD_AUTO_ENABLE:pn-wifi-config = "enable"
EOF
```

---

## 6. Crear archivo de configuración de WiFi

Crearemos un archivo que Yocto pueda inyectar en la imagen durante el build. La forma más simple es hacerlo sin recetas complicadas.

### 6.1. Crear directorio para archivos

```bash
cd /home/yoctouser/yocto_projects/poky/build
mkdir -p files/etc/wpa_supplicant
```

### 6.2. Crear el archivo de configuración de `wpa_supplicant`

```bash
cat > files/etc/wpa_supplicant/wpa_supplicant.conf << 'EOF'
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

### 6.2. Crear el archivo `layer.conf`

Cada layer en Yocto necesita un archivo `conf/layer.conf` que le indique a BitBake cómo procesarla.

```bash
mkdir -p /home/yoctouser/yocto_projects/poky/build/meta-rpi-custom/conf

cat > /home/yoctouser/yocto_projects/poky/build/meta-rpi-custom/conf/layer.conf << 'EOF'
# We have a conf and classes directory, add to BBPATH
BBPATH .= ":${LAYERDIR}"

# We have recipes-* directories, add to BBFILES
BBFILES += "${LAYERDIR}/recipes-*/*/*.bb \
            ${LAYERDIR}/recipes-*/*/*.bbappend"

BBFILE_COLLECTIONS += "rpi-custom"
BBFILE_PATTERN_rpi-custom = "^${LAYERDIR}/"
BBFILE_PRIORITY_rpi-custom = "10"

LAYERDEPENDS_rpi-custom = "core"

LAYERSERIES_COMPAT_rpi-custom = "kirkstone"
EOF
```

Este archivo configura:
- **BBPATH**: Dónde BitBake busca recetas.
- **BBFILES**: Qué archivos `.bb` y `.bbappend` procesar.
- **BBFILE_COLLECTIONS**: Nombre interno de la layer.
- **BBFILE_PRIORITY**: Prioridad (10 es medio-baja, permite que otros overrides).
- **LAYERDEPENDS**: Dependencias con otras layers (acá, `core`).
- **LAYERSERIES_COMPAT**: Compatibilidad con la serie Yocto (en tu caso, `kirkstone`).

Verificá que el archivo se creó:

```bash
ls -la /home/yoctouser/yocto_projects/poky/build/meta-rpi-custom/conf/layer.conf
```


---

## 7. Crear una receta simple para inyectar la configuración

Ah che, acá es donde simplificamos todo. En lugar de una receta complicada, hacemos una que solo copia los archivos.

### 7.1. Crear la receta

```bash
cd /home/yoctouser/yocto_projects/poky/build
mkdir -p meta-rpi-custom/recipes-connectivity/wifi-config/files

cat > meta-rpi-custom/recipes-connectivity/wifi-config/files/wpa_supplicant.conf << 'EOF'
ctrl_interface=/var/run/wpa_supplicant
update_config=1
country=ES

network={
    ssid="wifi"
    psk="perlitalagatita"
    key_mgmt=WPA-PSK
}
EOF

cat > meta-rpi-custom/recipes-connectivity/wifi-config/wifi-config.bb << 'EOF'
SUMMARY = "Configuración WiFi para Raspberry Pi 4"
LICENSE = "MIT"

SRC_URI = "file://wpa_supplicant.conf \
           file://wpa-supplicant-wlan0.service"
S = "${WORKDIR}"

inherit systemd

do_install() {
    # Instalar configuración de wpa_supplicant
    install -d ${D}/etc/wpa_supplicant
    install -m 0600 ${WORKDIR}/wpa_supplicant.conf ${D}/etc/wpa_supplicant/wpa_supplicant.conf
    
    # Instalar servicio systemd
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/wpa-supplicant-wlan0.service ${D}${systemd_system_unitdir}/
}

FILES:${PN} = "/etc/wpa_supplicant/wpa_supplicant.conf \
               ${systemd_system_unitdir}/wpa-supplicant-wlan0.service"

RDEPENDS:${PN} = "wpa-supplicant systemd"

SYSTEMD_AUTO_ENABLE:${PN} = "enable"
SYSTEMD_SERVICE:${PN} = "wpa-supplicant-wlan0.service"
EOF
```

### 7.2. Agregar la capa `meta-rpi-custom` a `bblayers.conf`

```bash
cd /home/yoctouser/yocto_projects/poky/build

# Editar bblayers.conf para agregar la nueva capa
sed -i 's|BBLAYERS ?= " |BBLAYERS ?= " \
  /home/yoctouser/yocto_projects/poky/build/meta-rpi-custom |' conf/bblayers.conf
```

### 7.3. Agregar el paquete a la imagen

```bash
echo 'IMAGE_INSTALL:append = " wifi-config"' >> conf/local.conf
```

---

## 8. Habilitar WiFi automáticamente en el arranque con systemd

Para que `wpa_supplicant` se ejecute automáticamente en `wlan0`, vamos a crear un **servicio systemd limpio y profesional**.

### 8.1. Crear el servicio systemd

Primero, crearemos el archivo del servicio que se va a inyectar en la imagen:

```bash
cd /home/yoctouser/yocto_projects/poky/build/meta-rpi-custom/recipes-connectivity/wifi-config/files

cat > wpa-supplicant-wlan0.service << 'EOF'
[Unit]
Description=WPA Supplicant for wlan0
Before=network-online.target
Wants=network-online.target

[Service]
Type=simple
ExecStart=/sbin/wpa_supplicant -u -i wlan0 -c /etc/wpa_supplicant/wpa_supplicant.conf -Dnl80211
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
```

### 8.2. Actualizar la receta `wifi-config.bb` para incluir systemd

Reemplazá el contenido de `wifi-config.bb` con esto:

```bash
cat > /home/yoctouser/yocto_projects/poky/build/meta-rpi-custom/recipes-connectivity/wifi-config/wifi-config.bb << 'EOF'
SUMMARY = "Configuración WiFi para Raspberry Pi 4"
LICENSE = "MIT"

SRC_URI = "file://wpa_supplicant.conf \
           file://wpa-supplicant-wlan0.service"
S = "${WORKDIR}"

inherit systemd

do_install() {
    # Instalar configuración de wpa_supplicant
    install -d ${D}/etc/wpa_supplicant
    install -m 0600 ${WORKDIR}/wpa_supplicant.conf ${D}/etc/wpa_supplicant/wpa_supplicant.conf
    
    # Instalar servicio systemd
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/wpa-supplicant-wlan0.service ${D}${systemd_system_unitdir}/
}

FILES:${PN} = "/etc/wpa_supplicant/wpa_supplicant.conf \
               ${systemd_system_unitdir}/wpa-supplicant-wlan0.service"

RDEPENDS:${PN} = "wpa-supplicant systemd"

SYSTEMD_AUTO_ENABLE:${PN} = "enable"
SYSTEMD_SERVICE:${PN} = "wpa-supplicant-wlan0.service"
EOF
```

### 8.3. Verificar que todo está en su lugar

```bash
ls -la /home/yoctouser/yocto_projects/poky/build/meta-rpi-custom/recipes-connectivity/wifi-config/files/
# Debería haber:
# - wpa_supplicant.conf
# - wpa-supplicant-wlan0.service
```

---

## 9. Construir la imagen

### 9.1. Asegurarse de estar en el build

```bash
cd /home/yoctouser/yocto_projects/poky/build
source ../oe-init-build-env 2>/dev/null || true
```

### 9.2. Lanzar `bitbake` con la imagen

```bash
bitbake core-image-minimal
```

Este paso tardará bastante (especialmente la primera vez, boludo). Andá a tomar un café.

Al terminar, la imagen para SD debería estar en:

```text
tmp/deploy/images/raspberrypi4-64/core-image-minimal-raspberrypi4-64.rpi-sdimg
```

---

## 10. Grabar la imagen en la tarjeta SD (host macOS)

Este paso se hace **fuera del contenedor**, de vuelta en macOS.

### 10.1. Copiar el archivo desde el contenedor al host

```bash
# En el host (no dentro del contenedor):
cd /Users/mini/Damian/Yocto

docker cp yocto-minimal:/home/yoctouser/yocto_projects/poky/build/tmp/deploy/images/raspberrypi4-64/core-image-minimal-raspberrypi4-64.rpi-sdimg .
```

### 10.2. Identificar la tarjeta SD

```bash
diskutil list
```

Localiza el disco, por ejemplo `/dev/disk4`. **Cuidado**: asegúrate de que es la SD.

### 10.3. Desmontar (sin expulsar)

```bash
diskutil unmountDisk /dev/disk4
```

### 10.4. Grabar con `dd`

```bash
cd /Users/mini/Damian/Yocto

sudo dd if=core-image-minimal-raspberrypi4-64.rpi-sdimg of=/dev/rdisk4 bs=4m conv=sync
sudo sync
```

**Nota:** usa `/dev/rdisk4` (con la `r`) para velocidad.

---

## 11. Primer arranque y conexión por SSH

### 11.1. Arrancar la Raspberry

1. Inserta la SD en la Raspberry Pi 4.
2. Conecta la alimentación.
3. Espera 1-2 minutos a que:
   - El sistema arranque.
   - `wlan0` intente conectarse a WiFi.
   - SSH esté disponible.

### 11.2. Encontrar la IP

Opción 1: mira en el router.  
Opción 2: escanea la red:

```bash
# Para red 192.168.1.0/24 (ajusta según tu red)
nmap -sn 192.168.1.0/24 | grep -i "raspberry\|\.local"
```

### 11.3. Conectarte por SSH

```bash
ssh -v root@IP_DE_LA_RASPBERRY
```

Si todo está ok, deberías estar dentro sin contraseña.

---

## 12. Troubleshooting rápido

### SSH no funciona

- Verifica que el servidor SSH está en la imagen: `ps aux | grep ssh`
- Reconstruye con `EXTRA_IMAGE_FEATURES += "ssh-server-dropbear"` bien configurado.

### WiFi no se conecta

- Entra por UART o HDMI y verifica:
  ```bash
  ip link show wlan0
  wpa_cli status
  ```
- Cambia el driver en `wifi-config.bb` de `-Dnl80211` a `-Dwext`.

### No hay IP en wlan0

- Verifica que `dhclient` está disponible.
- O configura IP estática en `wpa_supplicant.conf`:
  ```conf
  network={
      ssid="wifi"
      psk="perlitalagatita"
      key_mgmt=WPA-PSK
      static_ip_address=192.168.1.100/24
      static_routers=192.168.1.1
  }
  ```

---

## Resumen rápido

1. Docker: levantar contenedor.
2. Clonar `meta-raspberrypi`.
3. Inicializar build con `oe-init-build-env`.
4. Configurar `bblayers.conf` (Poky + meta-raspberrypi).
5. Configurar `local.conf` (máquina, SSH, WiFi, limpieza).
6. Crear receta `wifi-config` con wpa_supplicant.conf.
7. `bitbake core-image-minimal`.
8. Grabar en SD con `dd`.
9. Arrancar y conectarse por SSH.

¡Dale, que esto funciona!