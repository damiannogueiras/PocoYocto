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
