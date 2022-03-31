package cores.pipeline

import chisel3._
import chiseltest._
import cores.nix.stages.Fetch
import org.scalatest.flatspec.AnyFlatSpec
import lib.BundleExpect._

class FetchSpec extends AnyFlatSpec with ChiselScalatestTester{

  behavior of "Fetch Stage"
/*
  def testFetch(pc: Long)(instruction: Long*)(expects: Fetch => Unit*): TestResult = {
    test(new Fetch) { dut =>
      instruction.foreach { instruction =>
        dut.upstream.data.pc.poke(pc.U)
        dut.io.instructionResponse.bits.instruction.poke(instruction.U)
        dut.io.instructionResponse.valid.poke(1.B)

        dut.downstream.data.pc.expect(pc.U)
        dut.downstream.data.nextPc.expect((pc+4).U)
        dut.downstream.data.instruction.expect(instruction.U)

        dut.io.pc.expect(pc.U)
        dut.io.nextPc.expect((pc+4).U)

        expects.foreach(_(dut))
      }
    }
  }

  it should "handle Opcode.load" in
    testFetch(4)(
      0x00218083L, // lb x1, 2(x3)
      0x00219083L, // lh x1, 2(x3)
      0x0021a083L, // lw x1, 2(x3)
      0x0021c083L, // lbu x1, 2(x3)
      0x0021d083L, // lhu x1, 2(x3)
    )(
      _.downstream.data.control.expect(
        _.isJalr -> 0.B,
        _.isBranch -> 0.B,
        _.isLoad -> 1.B,
        _.isStore -> 0.B,
        _.isLui -> 0.B,
        _.isImmediate -> 0.B,
        _.isSystem -> 0.B,
        _.isRegister -> 0.B,
        _.aluFunIsAdd -> 1.B,
        _.destinationIsNonZero -> 1.B,
        _.leftOperand -> LeftOperand.Register,
        _.rightOperand -> RightOperand.Immediate,
        _.instructionType -> InstructionType.I
      ),
      _.io.branching.expect(
        _.takeGuess -> 0.B,
        _.jump -> 0.B,
      ),
      _.io.registerSources.expect(
        _.index(0) -> 3.U,
      )
    )

  it should "handle Opcode.immediate" in
    testFetch(24)(
      0x00418113L, // addi x2, x3, 4
      0x00419113L, // slli x2, x3, 4
      0x0041a113L, // slti x2, x3, 4
      0x0041b113L, // sltiu x2, x3, 4
      0x0041c113L, // xori x2, x3, 4
      0x0041d113L, // srli x2, x3, 4
      0x4041d113L, // srai x2, x3, 4
      0x0041e113L, // ori x2, x3, 4
      0x0041f113L, // andi x2, x3, 4
    )(
      _.downstream.data.control.expect(
        _.isJalr -> 0.B,
        _.isBranch -> 0.B,
        _.isLoad -> 0.B,
        _.isStore -> 0.B,
        _.isLui -> 0.B,
        _.isImmediate -> 1.B,
        _.isSystem -> 0.B,
        _.isRegister -> 0.B,
        _.aluFunIsAdd -> 0.B,
        _.destinationIsNonZero -> 1.B,
        _.leftOperand -> LeftOperand.Register,
        _.rightOperand -> RightOperand.Immediate,
        _.instructionType -> InstructionType.I
      ),
      _.io.branching.expect(
        _.takeGuess -> 0.B,
        _.jump -> 0.B,
      ),
      _.io.registerSources.expect(
        _.index(0) -> 3.U,
      )
    )
  it should "handle Opcode.auipc" in
    testFetch(128)(
      0x0dead197L, // auipc x3, 0xdead
    )(
      _.downstream.data.control.expect(
        _.isJalr -> 0.B,
        _.isBranch -> 0.B,
        _.isLoad -> 0.B,
        _.isStore -> 0.B,
        _.isLui -> 0.B,
        _.isImmediate -> 0.B,
        _.isSystem -> 0.B,
        _.isRegister -> 0.B,
        _.aluFunIsAdd -> 1.B,
        _.destinationIsNonZero -> 1.B,
        _.leftOperand -> LeftOperand.PC,
        _.rightOperand -> RightOperand.Immediate,
        _.instructionType -> InstructionType.U
      ),
      _.io.branching.expect(
        _.takeGuess -> 0.B,
        _.jump -> 0.B,
      )
    )
  it should "handle Opcode.store" in
    testFetch(128)(
      0x004302a3L, // sb x4, 5(x6)
      0x004312a3L, // sh x4, 5(x6)
      0x004322a3L, // sw x4, 5(x6)
    )(
      _.downstream.data.control.expect(
        _.isJalr -> 0.B,
        _.isBranch -> 0.B,
        _.isLoad -> 0.B,
        _.isStore -> 1.B,
        _.isLui -> 0.B,
        _.isImmediate -> 0.B,
        _.isSystem -> 0.B,
        _.isRegister -> 0.B,
        _.aluFunIsAdd -> 1.B,
        _.leftOperand -> LeftOperand.Register,
        _.rightOperand -> RightOperand.Immediate,
        _.writeSourceRegister -> 1.U,
        _.instructionType -> InstructionType.S
      ),
      _.io.branching.expect(
        _.takeGuess -> 0.B,
        _.jump -> 0.B,
      ),
      _.io.registerSources.expect(
        _.index(0) -> 6.U,
        _.index(1) -> 4.U
      )
    )
  it should "handle Opcode.register" in
    testFetch(260)(
      0x01f50433L, // add x8, x10, x31
      0x41f50433, // sub x8, x10, x31
      0x01f51433L, // sll x8, x10, x31
      0x01f52433L, // slt x8, x10, x31
      0x01f53433, // sltu x8, x10, x31
      0x01f54433L, // xor x8, x10, x31
      0x01f55433L, // srl x8, x10, x31
      0x41f55433L, // sra x8, x10, x31
      0x01f56433L, // or x8, x10, x31
      0x01f57433L, // and x8, x10, x31
    )(
      _.downstream.data.control.expect(
        _.isJalr -> 0.B,
        _.isBranch -> 0.B,
        _.isLoad -> 0.B,
        _.isStore -> 0.B,
        _.isLui -> 0.B,
        _.isImmediate -> 0.B,
        _.isSystem -> 0.B,
        _.isRegister -> 1.B,
        _.aluFunIsAdd -> 0.B,
        _.destinationIsNonZero -> 1.B,
        _.leftOperand -> LeftOperand.Register,
        _.rightOperand -> RightOperand.Register,
        _.instructionType -> InstructionType.R
      ),
      _.io.branching.expect(
        _.takeGuess -> 0.B,
        _.jump -> 0.B,
      ),
      _.io.registerSources.expect(
        _.index(0) -> 10.U,
        _.index(1) -> 31.U
      )
    )
  it should "handle Opcode.lui" in
    testFetch(9990)(
      0xabcdeb37L, // lui x22, 0xabcde
    )(
      _.downstream.data.control.expect(
        _.isJalr -> 0.B,
        _.isBranch -> 0.B,
        _.isLoad -> 0.B,
        _.isStore -> 0.B,
        _.isLui -> 1.B,
        _.isImmediate -> 0.B,
        _.isSystem -> 0.B,
        _.isRegister -> 0.B,
        _.aluFunIsAdd -> 1.B,
        _.destinationIsNonZero -> 1.B,
        _.leftOperand -> LeftOperand.Zero,
        _.rightOperand -> RightOperand.Immediate,
        _.instructionType -> InstructionType.U
      ),
      _.io.branching.expect(
        _.takeGuess -> 0.B,
        _.jump -> 0.B,
      )
    )
  it should "handle Opcode.branch" in
    testFetch(80)(
      0xff6a04e3L, // beq x20, x22, -24
      0xff6a14e3L, // bne x20, x22, -24
      0xff6a44e3L, // blt x20, x22, -24
      0xff6a54e3L, // bge x20, x22, -24
      0xff6a64e3L, // bltu x20, x22, -24
      0xff6a74e3L, // bgeu x20, x22, -24
    )(
      _.downstream.data.control.expect(
        _.isJalr -> 0.B,
        _.isBranch -> 1.B,
        _.isLoad -> 0.B,
        _.isStore -> 0.B,
        _.isLui -> 0.B,
        _.isImmediate -> 0.B,
        _.isSystem -> 0.B,
        _.isRegister -> 0.B,
        _.instructionType -> InstructionType.B
      ),
      _.io.branching.expect(
        _.takeGuess -> 1.B,
        _.jump -> 0.B,
        _.target -> 56.U,
        _.backwards -> 1.B
      ),
      _.io.registerSources.expect(
        _.index(0) -> 20.U,
        _.index(1) -> 22.U
      )
    )
  it should "handle Opcode.jal" in
    testFetch(100)(
      0x01c000efL, // jal x1, 28
    )(
      _.downstream.data.control.expect(
        _.isJalr -> 0.B,
        _.isBranch -> 0.B,
        _.isLoad -> 0.B,
        _.isStore -> 0.B,
        _.isLui -> 0.B,
        _.isImmediate -> 0.B,
        _.isSystem -> 0.B,
        _.isRegister -> 0.B,
        _.destinationIsNonZero -> 1.B,
        _.aluFunIsAdd -> 1.B,
        _.leftOperand -> LeftOperand.PC,
        _.rightOperand -> RightOperand.Four,
        _.instructionType -> InstructionType.J
      ),
      _.io.branching.expect(
        _.takeGuess -> 0.B,
        _.jump -> 1.B,
        _.target -> 128.U
      )
    )
  it should "handle Opcode.jalr" in
    testFetch(700)(
      0xfe8480e7L, // jalr x1, -24(x9)
    )(
      _.downstream.data.control.expect(
        _.isJalr -> 1.B,
        _.isBranch -> 0.B,
        _.isLoad -> 0.B,
        _.isStore -> 0.B,
        _.isLui -> 0.B,
        _.isImmediate -> 0.B,
        _.isSystem -> 0.B,
        _.isRegister -> 0.B,
        _.aluFunIsAdd -> 1.B,
        _.destinationIsNonZero -> 1.B,
        _.leftOperand -> LeftOperand.PC,
        _.rightOperand -> RightOperand.Four,
        _.instructionType -> InstructionType.I
      ),
      _.io.branching.expect(
        _.takeGuess -> 0.B,
        _.jump -> 0.B,
      ),
      _.io.registerSources.expect(
        _.index(0) -> 9.U,
      )
    )
  it should "handle Opcode.system with csr" in
    testFetch(4)(
      0x04021df3L, // csrrw x27, 64, x4
      0x04022df3L, // csrrs x27, 64, x4
      0x04023df3L, // csrrc x27, 64, x4
      0x04025df3L, // csrrwi x27, 64, 4
      0x04026df3L, // csrrsi x27, 64, 4
      0x04027df3L, // csrrci x27, 64, 4
    )(
      _.downstream.data.control.expect(
        _.isJalr -> 0.B,
        _.isBranch -> 0.B,
        _.isLoad -> 0.B,
        _.isStore -> 0.B,
        _.isLui -> 0.B,
        _.isImmediate -> 0.B,
        _.isSystem -> 1.B,
        _.isRegister -> 0.B,
        _.destinationIsNonZero -> 1.B,
        _.writeSourceRegister -> 0.U,
        _.instructionType -> InstructionType.I
      ),
      _.io.branching.expect(
        _.takeGuess -> 0.B,
        _.jump -> 0.B,
      ),
      _.io.registerSources.expect(
        _.index(0) -> 4.U,
      )
    )
*/
}
