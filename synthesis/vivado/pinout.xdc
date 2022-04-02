set_property IOSTANDARD LVCMOS33 [get_ports *]

## Clock signal
set_property PACKAGE_PIN W5 [get_ports clock]
create_clock -add -name sys_clk_pin -period 10.00 -waveform {0 5} [get_ports clock]

## LED and reset button
set_property PACKAGE_PIN U16 [get_ports {io_led}]
set_property PACKAGE_PIN T17 [get_ports reset]
set_property PACKAGE_PIN B18 [get_ports {io_rx}]
set_property PACKAGE_PIN A18 [get_ports {io_tx}]