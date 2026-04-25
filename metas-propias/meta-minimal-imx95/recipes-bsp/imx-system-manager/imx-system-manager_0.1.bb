SUMMARY = "Stub provider for virtual/imx-system-manager"
LICENSE = "MIT"
PR = "r0"

PROVIDES = "virtual/imx-system-manager"

inherit allarch

# Install a tiny marker file so the package isn't empty
do_install() {
    install -d ${D}${sysconfdir}/imx-system-manager
    echo "stub" > ${D}${sysconfdir}/imx-system-manager/stub
}

FILES:${PN} += "${sysconfdir}/imx-system-manager"
