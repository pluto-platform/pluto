current_dir := ${CURDIR}
TARGET = basys3
TOP := Top
SOURCES := $(wildcard ${CURDIR}/../../build/*.v) 

# Include your constraint file path(s) below. Use either an XDC file
# or a PCF+SDC pair. Don't use all three file types.
XDC := ${CURDIR}/pinout.xdc

BUILDDIR := ${current_dir}/build

DEVICE := xc7a50t_test
BITSTREAM_DEVICE := artix7
PARTNAME := xc7a35tcpg236-1

SHELL := /bin/bash

# Determine the type of constraint being used
ifneq (${XDC},)
  XDC_CMD := -x ${XDC}
endif
ifneq (${SDC},)
  SDC_CMD := -s ${SDC}
endif
ifneq (${PCF},)
  PCF_CMD := -p ${PCF}
endif

.DELETE_ON_ERROR:

# Build design
all: ${BUILDDIR}/${TOP}.bit

${BUILDDIR}:
	mkdir -p ${BUILDDIR}

${BUILDDIR}/${TOP}.eblif: ${SOURCES} ${XDC} ${SDC} ${PCF} | ${BUILDDIR}
	cd ${BUILDDIR} && symbiflow_synth -t ${TOP} -v ${SOURCES} -d ${BITSTREAM_DEVICE} -p ${PARTNAME} ${XDC_CMD} 2>&1 > /dev/null

${BUILDDIR}/${TOP}.net: ${BUILDDIR}/${TOP}.eblif
	cd ${BUILDDIR} && symbiflow_pack -e ${TOP}.eblif -d ${DEVICE} ${SDC_CMD} 2>&1 > /dev/null

${BUILDDIR}/${TOP}.place: ${BUILDDIR}/${TOP}.net
	cd ${BUILDDIR} && symbiflow_place -e ${TOP}.eblif -d ${DEVICE} ${PCF_CMD} -n ${TOP}.net -P ${PARTNAME} ${SDC_CMD} 2>&1 > /dev/null

${BUILDDIR}/${TOP}.route: ${BUILDDIR}/${TOP}.place
	cd ${BUILDDIR} && symbiflow_route -e ${TOP}.eblif -d ${DEVICE} ${SDC_CMD} 2>&1 > /dev/null

${BUILDDIR}/${TOP}.fasm: ${BUILDDIR}/${TOP}.route
	cd ${BUILDDIR} && symbiflow_write_fasm -e ${TOP}.eblif -d ${DEVICE}

${BUILDDIR}/${TOP}.bit: ${BUILDDIR}/${TOP}.fasm
	cd ${BUILDDIR} && symbiflow_write_bitstream -d ${BITSTREAM_DEVICE} -f ${TOP}.fasm -p ${PARTNAME} -b ${TOP}.bit

download: ${BUILDDIR}/${TOP}.bit
	if [ $(TARGET)='arty_35' ]; then \
	  openocd -f ~/opt/symbiflow/xc7/conda/envs/xc7/share/openocd/scripts/board/digilent_arty.cfg -c "init; pld load 0 ${BOARD_BUILDDIR}/${TOP}.bit; exit"; \
	elif [ $(TARGET)='arty_100' ]; then \
	  openocd -f ~/opt/symbiflow/xc7/conda/envs/xc7/share/openocd/scripts/board/digilent_arty.cfg -c "init; pld load 0 ${BOARD_BUILDDIR}/${TOP}.bit; exit"; \
	elif [ $(TARGET)='basys3' ]; then \
	  openocd -f ~/opt/symbiflow/xc7/conda/envs/xc7/share/openocd/scripts/board/digilent_arty.cfg -c "init; pld load 0 ${BOARD_BUILDDIR}/${TOP}.bit; exit"; \
	else  \
	  echo "The commands needed to download the bitstreams to the board type specified are not currently supported by the F4PGA makefiles. \
    Please see documentation for more information."; \
	fi


clean:
	rm -rf ${BUILDDIR}