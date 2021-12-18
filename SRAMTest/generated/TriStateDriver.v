
module TriStateDriver(
    output [15:0] busData,
    input [15:0] driveData,
    inout [15:0] bus,
    input drive);

    assign bus = drive ? driveData : {(16){1'bz}};
    assign busData = bus;
endmodule
