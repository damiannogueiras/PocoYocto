FROM ubuntu:22.04

# Establecer la zona horaria para evitar advertencias durante la instalación de paquetes
ENV TZ=Europe/Madrid
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Evitar prompts interactivos durante la instalación
ARG DEBIAN_FRONTEND=noninteractive

RUN add-apt-repository -y universe \
&& apt update \
&& apt install -y --no-install-recommends software-properties-common ca-certificates \
&& apt clean

# Crear un usuario no-root para trabajar con Yocto
ARG YOCTO_PASS
RUN useradd -m -s /bin/bash yoctouser && echo "yoctouser:${YOCTO_PASS}" | chpasswd && adduser yoctouser sudo
USER yoctouser
WORKDIR /home/yoctouser/yocto_projects
