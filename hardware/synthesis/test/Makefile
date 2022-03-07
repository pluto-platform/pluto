
PROJECT = Ferrum
TOP_LEVEL = Top

PROJECT_FILE = $(PROJECT).qpf
ASSIGNMENT_FILES = $(PROJECT).qsf $(PROJECT).sdc

###################################################################
# Part, Family, Boardfile DE1 or DE2
FAMILY = "Cyclone IV E"
PART = EP4CE115F29C7
BOARDFILE = DE2Pins

SRCS = $(wildcard ../../build/*.v)


all: $(PROJECT).asm.rpt $(PROJECT).sta.rpt

clean:
	rm -rf *.sld *.rpt *.chg *.htm *.eqn *.pin *.sof *.pof db incremental_db *.summary *.smsg *.jdi $(PROJECT_FILE)


setup: $(PROJECT_FILE)
map: $(PROJECT).map.rpt
fit: $(PROJECT).fit.rpt
asm: $(PROJECT).asm.rpt
sta: $(PROJECT).sta.rpt

STAMP = echo $(shell date --iso=seconds) >

$(PROJECT).map.rpt: map.chg $(SOURCE_FILES) $(ASSIGNMENT_FILES)
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

$(PROJECT_FILE):
	quartus_sh --64bit --prepare -f $(FAMILY) -t $(TOP_LEVEL) $(PROJECT)
    #-cat $(BOARDFILE) >> $(PROJECT).qsf

map.chg:
	$(STAMP) map.chg
fit.chg:
	$(STAMP) fit.chg
sta.chg:
	$(STAMP) sta.chg
asm.chg:
	$(STAMP) asm.chg

program: $(PROJECT).sof
	killall jtagd
	jtagd
	jtagconfig
	quartus_pgm --no_banner --mode=jtag -o "P;$(PROJECT).sof"