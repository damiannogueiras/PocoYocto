SUMMARY = "Mi imagen mínima para i.MX95"
LICENSE = "MIT"

inherit core-image

# Solo lo indispensable para tener un prompt en la terminal
IMAGE_INSTALL = " \
    packagegroup-core-boot \
    ${CORE_IMAGE_EXTRA_INSTALL} \
    util-linux \
"

# Hacer la imagen de solo lectura (opcional, ahorra memoria RAM)
# IMAGE_FEATURES += "read-only-rootfs"

# Formato de salida comprimido
IMAGE_FSTYPES = "wic.gz"