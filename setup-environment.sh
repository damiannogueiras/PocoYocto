#!/bin/bash

# USO: source setup-environment.sh <directorio-build>
# Por ejemplo: source setup-environment.sh builds/rpi4

if [ -z "$1" ]; then
    echo "Error: Debes especificar el directorio de build."
    echo "Uso: source $0 <directorio-build>"
    return 1
fi

BUILD_DIR="$1"

# Verificar si el directorio de build ya existe
if [ ! -d "$BUILD_DIR" ]; then
    echo "El directorio de build no existe. Creándolo y configurando..."

    # Exportar la variable TEMPLATECONF para que oe-init-build-env la use
    export TEMPLATECONF=$(pwd)/metas-propias/meta-mini/conf

    # Inicializar el entorno de build de Poky. Esto creará el directorio
    # y copiará la configuración de plantilla desde TEMPLATECONF.
    source poky/oe-init-build-env "$BUILD_DIR"

    echo "****************************************************************"
    echo "Entorno de Yocto inicializado en: $(pwd)"
    echo ""
    echo "Asegúrate de configurar la variable MACHINE en conf/local.conf."
    echo "Por ejemplo: MACHINE ?= \"raspberrypi4-64\""
    echo ""
    echo "Luego, puedes construir tu imagen con:"
    echo "bitbake <nombre-de-imagen>"
    echo "****************************************************************"

else
    echo "Configurando entorno para el build existente en: $BUILD_DIR"
    source poky/oe-init-build-env "$BUILD_DIR"
fi