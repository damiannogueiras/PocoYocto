FROM ubuntu:22.04

# Establecer la zona horaria para evitar advertencias durante la instalación de paquetes
ENV TZ=Europe/Madrid
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Evitar prompts interactivos durante la instalación
ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    software-properties-common \
    && add-apt-repository -y universe \
    && apt-get update && apt-get install -y --no-install-recommends 
RUN apt install -y gawk wget git diffstat texinfo gcc build-essential chrpath socat cpio python3 python3-pip unzip xz-utils debianutils iputils-ping xterm sudo
RUN apt install -y python3-pexpect libsdl1.2-dev locales node-fs.realpath tzdata 
RUN apt-get clean && rm -rf /var/lib/apt/lists/*
# Configurar locales
RUN locale-gen es_ES.UTF-8
ENV LANG=es_ES.UTF-8
ENV LANGUAGE=es_ES:es
ENV LC_ALL=es_ES.UTF-8

# Crear un usuario no-root para trabajar con Yocto
ARG YOCTO_PASS
RUN useradd -m -s /bin/bash yoctouser && echo "yoctouser:${YOCTO_PASS}" | chpasswd && adduser yoctouser sudo
USER yoctouser
WORKDIR /home/yoctouser/yocto_projects
