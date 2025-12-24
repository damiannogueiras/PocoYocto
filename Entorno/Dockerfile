FROM ubuntu:22.04

# Establecer la zona horaria para evitar advertencias durante la instalación de paquetes
ENV TZ=Europe/Madrid
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Evitar prompts interactivos durante la instalación
ARG DEBIAN_FRONTEND=noninteractive

RUN apt update \
&& apt install -y --no-install-recommends software-properties-common ca-certificates \
&& add-apt-repository -y universe \
&& apt update \
&& apt install -y --no-install-recommends \
    gawk \
    wget \
    git \
    diffstat \
    unzip \
    texinfo \
    gcc \
    build-essential \
    python3 \
    python3-pip \
    iputils-ping \
    libsdl1.2-dev \
    xterm \
    locales \
    realpath \
    sudo \
    && apt clean && rm -rf /var/lib/apt/lists/*

# Configurar locales
RUN locale-gen es_ES.UTF-8
ENV LANG es_ES.UTF-8
ENV LANGUAGE es_ES:es
ENV LC_ALL es_ES.UTF-8

# Crear un usuario no-root para trabajar con Yocto
ARG YOCTO_PASS
RUN useradd -m -s /bin/bash yoctouser && echo "yoctouser:${YOCTO_PASS}" | chpasswd && adduser yoctouser sudo
USER yoctouser
WORKDIR /home/yoctouser/yocto_projects
