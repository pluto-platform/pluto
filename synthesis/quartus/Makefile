# Inspired by: https://github.com/mfischer/Altera-Makefile/blob/master/quartus/Makefile


PROJECT = Pluto
TOP_LEVEL = TopCached

PROJECT_FILES = $(PROJECT).qpf $(PROJECT).qsf $(PROJECT).sdc

###################################################################
# Part, Family, Boardfile DE1 or DE2
FAMILY = "Cyclone IV E"
PART = EP4CE115F29C7
BOARDFILE = DE2Pins
CLOCK_PERIOD = 20

SRCS = $(wildcard ../../build/*.v)


all: $(PROJECT).asm.rpt $(PROJECT).sta.rpt

clean:
	rm -rf *.sld *.rpt *.chg *.htm *.eqn *.pin *.sof *.pof db incremental_db *.summary *.smsg *.jdi $(PROJECT_FILES)


setup: $(PROJECT_FILES)
map: $(PROJECT).map.rpt
fit: $(PROJECT).fit.rpt
asm: $(PROJECT).asm.rpt
sta: $(PROJECT).sta.rpt

STAMP = echo $(shell date --iso=seconds) >

$(PROJECT).map.rpt: $(SRCS) pinout.tcl
	quartus_map --64bit --read_settings_files=on $(addprefix --source=,$(SRCS)) $(PROJECT)
	$(STAMP) fit.chg

$(PROJECT).fit.rpt: fit.chg $(PROJECT).map.rpt
	quartus_fit --64bit --part=$(PART) --read_settings_files=on $(PROJECT)
	$(STAMP) asm.chg
	$(STAMP) sta.chg

$(PROJECT).asm.rpt: asm.chg $(PROJECT).fit.rpt
	quartus_asm --64bit $(PROJECT)

$(PROJECT).sta.rpt: sta.chg $(PROJECT).fit.rpt
	quartus_sta --64bit $(PROJECT)

$(PROJECT_FILES):
	quartus_sh --64bit --prepare -f $(FAMILY) -t $(TOP_LEVEL) -d $(PART) $(PROJECT)
	echo "\nset_global_assignment -name SOURCE_TCL_SCRIPT_FILE pinout.tcl" >> $(PROJECT).qsf
	echo "create_clock -period $(CLOCK_PERIOD)  [get_ports {clock}]\nderive_clock_uncertainty" > $(PROJECT).sdc
	touch pinout.tcl

fit.chg:
	$(STAMP) fit.chg
sta.chg:
	$(STAMP) sta.chg
asm.chg:
	$(STAMP) asm.chg

$(PROJECT).sof: $(PROJECT).asm.rpt

program: $(PROJECT).sof
	killall jtagd || true
	jtagd
	jtagconfig
	quartus_pgm --no_banner --mode=jtag -o "P;$(PROJECT).sof"