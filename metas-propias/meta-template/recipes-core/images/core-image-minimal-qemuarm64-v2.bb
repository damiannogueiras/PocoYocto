DESCRIPTION = "Minimal qemuarm64 image v2 with a 15s timestamp cron job"
LICENSE = "MIT"

inherit core-image

IMAGE_INSTALL += " \
    python3 \
    cronie \
    marcatemporal \
"

IMAGE_FEATURES += "ssh-server-dropbear"

COMPATIBLE_MACHINE = "qemuarm64"

# Forzar IMAGE_LINK_NAME para que el .qemuboot.conf lleve el sufijo -v2 y no pise el de v1
IMAGE_LINK_NAME = "core-image-minimal-qemuarm64-v2-${MACHINE}"

# Forzar nombre del kernel para que .qemuboot.conf no quede con la expresión BitBake sin evaluar
QB_DEFAULT_KERNEL = "Image"
