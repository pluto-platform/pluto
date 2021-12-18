project_new SRAMTest -overwrite

set_global_assignment -name FAMILY "Cyclone IV E"
set_global_assignment -name DEVICE EP4CE115F29C7

set_global_assignment -name VERILOG_FILE ../generated/SRAMTest.v
set_global_assignment -name VERILOG_FILE ../generated/TriStateDriver.v
set_global_assignment -name TOP_LEVEL_ENTITY SRAMTest

set_location_assignment PIN_AB7 -to io_address[0]
set_location_assignment PIN_AD7 -to io_address[1]
set_location_assignment PIN_AE7 -to io_address[2]
set_location_assignment PIN_AC7 -to io_address[3]
set_location_assignment PIN_AB6 -to io_address[4]
set_location_assignment PIN_AE6 -to io_address[5]
set_location_assignment PIN_AB5 -to io_address[6]
set_location_assignment PIN_AC5 -to io_address[7]
set_location_assignment PIN_AF5 -to io_address[8]
set_location_assignment PIN_T7 -to io_address[9]
set_location_assignment PIN_AF2 -to io_address[10]
set_location_assignment PIN_AD3 -to io_address[11]
set_location_assignment PIN_AB4 -to io_address[12]
set_location_assignment PIN_AC3 -to io_address[13]
set_location_assignment PIN_AA4 -to io_address[14]
set_location_assignment PIN_AB11 -to io_address[15]
set_location_assignment PIN_AC11 -to io_address[16]
set_location_assignment PIN_AB9 -to io_address[17]
set_location_assignment PIN_AB8 -to io_address[18]
set_location_assignment PIN_T8 -to io_address[19]
set_location_assignment PIN_AH3 -to io_data[0]
set_location_assignment PIN_AF4 -to io_data[1]
set_location_assignment PIN_AG4 -to io_data[2]
set_location_assignment PIN_AH4 -to io_data[3]
set_location_assignment PIN_AF6 -to io_data[4]
set_location_assignment PIN_AG6 -to io_data[5]
set_location_assignment PIN_AH6 -to io_data[6]
set_location_assignment PIN_AF7 -to io_data[7]
set_location_assignment PIN_AD1 -to io_data[8]
set_location_assignment PIN_AD2 -to io_data[9]
set_location_assignment PIN_AE2 -to io_data[10]
set_location_assignment PIN_AE1 -to io_data[11]
set_location_assignment PIN_AE3 -to io_data[12]
set_location_assignment PIN_AE4 -to io_data[13]
set_location_assignment PIN_AF3 -to io_data[14]
set_location_assignment PIN_AG3 -to io_data[15]
set_location_assignment PIN_AF8 -to io_chipSelect
set_location_assignment PIN_AD5 -to io_outputEnable
set_location_assignment PIN_AD4 -to io_strobe_0
set_location_assignment PIN_AC4 -to io_strobe_1
set_location_assignment PIN_AE8 -to io_writeEnable
set_location_assignment PIN_Y2 -to clock
set_location_assignment PIN_M23 -to reset
set_location_assignment PIN_H15 -to io_error
set_location_assignment PIN_E25 -to io_done
set_location_assignment PIN_E21 -to io_reading
set_location_assignment PIN_E22 -to io_writing

project_close