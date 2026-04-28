DESCRIPTION = "Python timestamp cron job for qemuarm64-v2"
SECTION = "examples"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://marcatemporal.py;md5=3b27e9b2e685ffd3529e64e1f4047b99"

SRC_URI = "file://marcatemporal.py file://marcatemporal.cron"

inherit allarch

RDEPENDS_${PN} = "python3 cronie"

S = "${WORKDIR}"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/marcatemporal.py ${D}${bindir}/marcatemporal.py

    install -d ${D}${sysconfdir}/cron.d
    install -m 0644 ${WORKDIR}/marcatemporal.cron ${D}${sysconfdir}/cron.d/marcatemporal
}
