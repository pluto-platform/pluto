
PROJECT = Pluto
TOP_LEVEL = Top
PROJECT_FILES = synth.tcl config.tcl

PART = xc7a35tcpg236-1
CONFIG_PART = xc7a35t_0
CLOCK_PERIOD = 10

SRCS = $(wildcard ../../build/*.v)

all: synth
synth: $(PROJECT).bit

clean:
	rm -rf .Xil *.bit *.rpt usage_statistics_webtalk.html usage_statistics_webtalk.xml *.jou *.log $(PROJECT_FILES)

$(PROJECT).bit: $(SRCS) pinout.xdc $(PROJECT_FILES)
	vivado -mode batch -source synth.tcl

download: $(PROJECT).bit
	vivado -mode batch -source config.tcl
#add_files [ glob ../../build/prog.txt]\n\

synth.tcl:
	echo "\
read_verilog [ glob $(SRCS) ]\n\
read_xdc ./pinout.xdc\nsynth_design -top $(TOP_LEVEL) -part $(PART)\n\
opt_design\n\
place_design\n\
route_design\n\
report_timing_summary -file sta.rpt\n\
report_utilization -file util.rpt\n\
write_bitstream $(PROJECT).bit" > synth.tcl
config.tcl:
	echo "\
open_hw_manager \n\
connect_hw_server -allow_non_jtag \n\
open_hw_target \n\
current_hw_device [get_hw_devices $(CONFIG_PART)] \n\
refresh_hw_device -update_hw_probes false [lindex [get_hw_devices $(CONFIG_PART)] 0] \n\
set_property PROBES.FILE {} [get_hw_devices $(CONFIG_PART)] \n\
set_property FULL_PROBES.FILE {} [get_hw_devices $(CONFIG_PART)] \n\
set_property PROGRAM.FILE {./$(PROJECT).bit} [get_hw_devices $(CONFIG_PART)] \n\
program_hw_devices [get_hw_devices $(CONFIG_PART)] \n\
refresh_hw_device [lindex [get_hw_devices $(CONFIG_PART)] 0] \n\
close_hw_manager" > config.tcl
