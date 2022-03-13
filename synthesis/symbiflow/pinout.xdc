
## Clock signal
set_property PACKAGE_PIN W5 [get_ports clock]
set_property IOSTANDARD LVCMOS33 [get_ports clock]
create_clock -period 10.00 [get_ports clock]

## LED and reset button
set_property PACKAGE_PIN U16 [get_ports {io_led}]
set_property IOSTANDARD LVCMOS33 [get_ports io_led]
set_property PACKAGE_PIN T17 [get_ports reset]
set_property IOSTANDARD LVCMOS33 [get_ports reset]