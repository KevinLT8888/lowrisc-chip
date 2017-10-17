// See LICENSE.SiFive for license details.

module SimDTM(
  input clk,
  input reset,

  output        debug_req_valid,
  input         debug_req_ready,
  output [ 6:0] debug_req_bits_addr,
  output [ 1:0] debug_req_bits_op,
  output [31:0] debug_req_bits_data,

  input         debug_resp_valid,
  output        debug_resp_ready,
  input  [ 1:0] debug_resp_bits_resp,
  input  [31:0] debug_resp_bits_data,

  output [31:0] exit
);

  reg r_reset;

  wire #0.1 __debug_req_ready = debug_req_ready;
  wire #0.1 __debug_resp_valid = debug_resp_valid;
  wire [31:0] #0.1 __debug_resp_bits_resp = {30'b0, debug_resp_bits_resp};
  wire [31:0] #0.1 __debug_resp_bits_data = debug_resp_bits_data;

  reg __debug_req_valid;
  reg [31:0]  __debug_req_bits_addr;
  reg [31:0]  __debug_req_bits_op;
  reg [31:0]  __debug_req_bits_data;
  reg __debug_resp_ready;
  reg [31:0]  __exit;

  assign #0.1 debug_req_valid = __debug_req_valid;
  assign #0.1 debug_req_bits_addr = __debug_req_bits_addr[6:0];
  assign #0.1 debug_req_bits_op = __debug_req_bits_op[1:0];
  assign #0.1 debug_req_bits_data = __debug_req_bits_data[31:0];
  assign #0.1 debug_resp_ready = __debug_resp_ready;
  assign #0.1 exit = __exit;

  always @(posedge clk)
  begin
    r_reset <= reset;
    if (reset || r_reset)
    begin
      __debug_req_valid = 0;
      __debug_resp_ready = 0;
      __exit = 0;
    end
  end
endmodule
