DESCRIPTION = "Imagen mínima para Raspberry Pi Zero W con SSH y WiFi configurada"
LICENSE = "MIT"

inherit core-image

IMAGE_FEATURES += "ssh-server-dropbear"

IMAGE_INSTALL:append = " \
    wifi-config-mini \
"
