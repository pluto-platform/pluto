set_property IOSTANDARD LVCMOS33 [get_ports *]

## Clock signal
set_property PACKAGE_PIN W5 [get_ports clock]
create_clock -add -name sys_clk_pin -period 10.00 -waveform {0 5} [get_ports clock]

#set_property PACKAGE_PIN A16 [get_ports clock]

## LED and reset button
set_property PACKAGE_PIN U16 [get_ports {io_led}]
set_property PACKAGE_PIN T17 [get_ports reset]
set_property PACKAGE_PIN B18 [get_ports {io_rx}]
set_property PACKAGE_PIN A18 [get_ports {io_tx}]

set_property PACKAGE_PIN E19 [get_ports {io_pc[0]}]
set_property PACKAGE_PIN U19 [get_ports {io_pc[1]}]
set_property PACKAGE_PIN V19 [get_ports {io_pc[2]}]
set_property PACKAGE_PIN W18 [get_ports {io_pc[3]}]
set_property PACKAGE_PIN U15 [get_ports {io_pc[4]}]
set_property PACKAGE_PIN U14 [get_ports {io_pc[5]}]
set_property PACKAGE_PIN V14 [get_ports {io_pc[6]}]
set_property PACKAGE_PIN V13 [get_ports {io_pc[7]}]
set_property PACKAGE_PIN V3  [get_ports {io_pc[8]}]
set_property PACKAGE_PIN W3  [get_ports {io_pc[9]}]
