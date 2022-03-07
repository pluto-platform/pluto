read_verilog [ glob ../../build/Top.v ]
read_xdc ./pinout.xdc
synth_design -top Top -part xc7a35tcpg236-1
opt_design
place_design
route_design
report_timing_summary -file sta.rpt
report_utilization -file util.rpt
write_bitstream Ferrum.bit
