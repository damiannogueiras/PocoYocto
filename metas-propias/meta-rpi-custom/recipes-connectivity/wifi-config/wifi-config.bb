SUMMARY = "Configuración WiFi para Raspberry Pi Zero W"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://wpa_supplicant.conf \
           file://wpa-supplicant-wlan0.service"
S = "${WORKDIR}"

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