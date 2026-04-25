# Plan — raspberrypi4-64-v1

## Hecho

- [x] Mover etiqueta `init` al último commit de `main` y pushear al remoto
- [x] Crear rama `bsp/raspberrypi4-64-v1` desde `init`
- [x] Generar `README-machine.md` con la guía de fases para la RPi4
- [x] Generar `docker-compose.yaml` a partir del template (context apuntando a `./PocoYocto-env`)
- [x] Fix: `chown` no recursivo sobre bind mounts de `metas/` (causaba restart loop)
- [x] Contenedor `pocoyocto-raspberrypi4-64` corriendo y healthy
- [x] Configurar `local.conf`: `MACHINE=raspberrypi4-64`, SSH-only (dropbear), usuario `admin/pinux`, `rm_work`
- [x] Configurar `bblayers.conf`: capas base + meta-oe + meta-networking + meta-python + meta-raspberrypi + meta-template
- [x] Agregar submódulo `metas/meta-raspberrypi` (rama scarthgap)

## Pendiente

- [x] Fix `meta-template/conf/layer.conf`: agregar `scarthgap` a `LAYERSERIES_COMPAT`
- [x] Lanzar `bitbake core-image-minimal` — **HECHO** (log en `/home/pocoyoctouser/output/bitbake.log`)
- [ ] Copiar imagen `.wic` al host y grabar en SD
  - Imagen generada: `/home/pocoyoctouser/build/tmp/deploy/images/raspberrypi4-64/core-image-minimal-raspberrypi4-64.rootfs.wic.bz2`
- [ ] Verificar SSH con `ssh admin@<ip>`
