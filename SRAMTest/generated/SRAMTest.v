module SRAMTest(
  input         clock,
  input         reset,
  output [19:0] io_address,
  inout  [15:0] io_data,
  output        io_outputEnable,
  output        io_writeEnable,
  output        io_chipSelect,
  output        io_strobe_0,
  output        io_strobe_1,
  output        io_writing,
  output        io_reading,
  output        io_done,
  output        io_error
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
`endif // RANDOMIZE_REG_INIT
  wire [15:0] TriStateDriver_busData; // @[SRAMTest.scala 30:24]
  wire [15:0] TriStateDriver_driveData; // @[SRAMTest.scala 30:24]
  wire  TriStateDriver_drive; // @[SRAMTest.scala 30:24]
  wire  _T_1 = ~reset; // @[SRAMTest.scala 28:13]
  reg [2:0] _T_2; // @[SRAMTest.scala 32:27]
  reg [19:0] _T_3; // @[SRAMTest.scala 33:26]
  wire  _T_7 = 3'h0 == _T_2; // @[Conditional.scala 37:30]
  wire  _T_10 = 3'h1 == _T_2; // @[Conditional.scala 37:30]
  wire [19:0] _T_12 = _T_3 + 20'h1; // @[SRAMTest.scala 60:28]
  wire  _T_13 = _T_3 == 20'hfffff; // @[SRAMTest.scala 63:22]
  wire [1:0] _GEN_0 = _T_13 ? 2'h2 : 2'h1; // @[SRAMTest.scala 63:52]
  wire  _T_16 = 3'h2 == _T_2; // @[Conditional.scala 37:30]
  wire [1:0] _GEN_2 = _T_13 ? 2'h3 : 2'h2; // @[SRAMTest.scala 73:52]
  wire  _T_22 = TriStateDriver_busData[7:0] != _T_3[7:0]; // @[SRAMTest.scala 76:28]
  wire  _T_25 = 3'h3 == _T_2; // @[Conditional.scala 37:30]
  wire  _T_28 = 3'h4 == _T_2; // @[Conditional.scala 37:30]
  wire  _T_31 = 3'h5 == _T_2; // @[Conditional.scala 37:30]
  wire  _GEN_8 = _T_28 ? 1'h0 : _T_31; // @[Conditional.scala 39:67]
  wire  _GEN_10 = _T_25 | _GEN_8; // @[Conditional.scala 39:67]
  wire  _GEN_11 = _T_25 ? 1'h0 : _T_28; // @[Conditional.scala 39:67]
  wire  _GEN_14 = _T_16 ? 1'h0 : 1'h1; // @[Conditional.scala 39:67]
  wire  _GEN_16 = _T_16 ? 1'h0 : _GEN_10; // @[Conditional.scala 39:67]
  wire  _GEN_17 = _T_16 ? 1'h0 : _GEN_11; // @[Conditional.scala 39:67]
  wire  _GEN_21 = _T_10 ? 1'h0 : 1'h1; // @[Conditional.scala 39:67]
  wire  _GEN_22 = _T_10 ? 1'h0 : _T_16; // @[Conditional.scala 39:67]
  wire  _GEN_23 = _T_10 | _GEN_14; // @[Conditional.scala 39:67]
  wire  _GEN_24 = _T_10 ? 1'h0 : _GEN_16; // @[Conditional.scala 39:67]
  wire  _GEN_25 = _T_10 ? 1'h0 : _GEN_17; // @[Conditional.scala 39:67]
  TriStateDriver TriStateDriver ( // @[SRAMTest.scala 30:24]
    .busData(TriStateDriver_busData),
    .driveData(TriStateDriver_driveData),
    .bus(io_data),
    .drive(TriStateDriver_drive)
  );
  assign io_address = _T_3; // @[SRAMTest.scala 41:16]
  assign io_outputEnable = _T_7 | _GEN_23; // @[SRAMTest.scala 40:21 SRAMTest.scala 71:25]
  assign io_writeEnable = _T_7 | _GEN_21; // @[SRAMTest.scala 39:20 SRAMTest.scala 62:24]
  assign io_chipSelect = 1'h0; // @[SRAMTest.scala 38:19]
  assign io_strobe_0 = 1'h0; // @[SRAMTest.scala 37:15]
  assign io_strobe_1 = 1'h1; // @[SRAMTest.scala 37:15]
  assign io_writing = _T_7 ? 1'h0 : _T_10; // @[SRAMTest.scala 46:16 SRAMTest.scala 59:20]
  assign io_reading = _T_7 ? 1'h0 : _GEN_22; // @[SRAMTest.scala 47:16 SRAMTest.scala 69:20]
  assign io_done = _T_7 | _GEN_24; // @[SRAMTest.scala 48:13 SRAMTest.scala 53:17 SRAMTest.scala 82:17 SRAMTest.scala 90:17]
  assign io_error = _T_7 | _GEN_25; // @[SRAMTest.scala 49:14 SRAMTest.scala 54:18 SRAMTest.scala 86:18]
  assign TriStateDriver_driveData = _T_3[15:0]; // @[SRAMTest.scala 43:25]
  assign TriStateDriver_drive = _T_7 ? 1'h0 : _T_10; // @[SRAMTest.scala 42:21 SRAMTest.scala 61:25]
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  _T_2 = _RAND_0[2:0];
  _RAND_1 = {1{`RANDOM}};
  _T_3 = _RAND_1[19:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
  always @(posedge clock) begin
    if (_T_1) begin
      _T_2 <= 3'h0;
    end else if (_T_7) begin
      _T_2 <= 3'h1;
    end else if (_T_10) begin
      _T_2 <= {{1'd0}, _GEN_0};
    end else if (_T_16) begin
      if (_T_22) begin
        _T_2 <= 3'h4;
      end else begin
        _T_2 <= {{1'd0}, _GEN_2};
      end
    end else if (_T_25) begin
      _T_2 <= 3'h5;
    end else if (_T_28) begin
      _T_2 <= 3'h4;
    end else if (_T_31) begin
      _T_2 <= 3'h3;
    end
    if (_T_1) begin
      _T_3 <= 20'h0;
    end else if (!(_T_7)) begin
      if (_T_10) begin
        if (_T_13) begin
          _T_3 <= 20'h0;
        end else begin
          _T_3 <= _T_12;
        end
      end else if (_T_16) begin
        _T_3 <= _T_12;
      end
    end
  end
endmodule
