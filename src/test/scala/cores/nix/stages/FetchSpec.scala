package cores.nix.stages

import chisel3._
import chiseltest._
import cores.lib.riscv.InstructionType
import cores.nix.ControlTypes.{LeftOperand, RightOperand}
import org.scalatest.flatspec.AnyFlatSpec
import lib.riscv.Assemble
import lib.BundleExpect._
import lib.riscv.Assemble.assembleToWords

/*
//"beq x0, x1, .+0x234"
class FetchSpec extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Fetch Stage"

  def testFetch(pc: Long)(instructions: String*)(expects: Fetch => Unit*): TestResult = {
    test(new Fetch) { dut =>
      assembleToWords(instructions).foreach { instruction =>
        dut.upstream.reg.pc.poke(pc.U)
        dut.io.instructionResponse.bits.instruction.poke(instruction.U)
        dut.io.instructionResponse.valid.poke(1.B)

        dut.downstream.reg.pc.expect(pc.U)
        dut.downstream.reg.instruction.expect(instruction.U)

        expects.foreach(_(dut))
      }
    }
  }

  it should "handle Opcode.load" in
    testFetch(4)(
      "lb x1, 2(x3)",
      "lh x1, 2(x3)",
      "lw x1, 2(x3)",
      "lbu x1, 2(x3)",
      "lhu x1, 2(x3)",
    )(
      _.downstream.reg.expect(
        _.isLui -> 0.B,
        _.isImmediate -> 0.B,
        _.isRegister -> 0.B,
        _.destinationIsNonZero -> 1.B,
        _.instructionType -> InstructionType.I,
      ),
      _.downstream.reg.withSideEffects.expect(
        _.isJal -> 0.B,
        _.isJalr -> 0.B,
        _.isBranch -> 0.B,
        _.isLoad -> 1.B,
        _.isStore -> 0.B,
        _.isSystem -> 0.B,
        _.hasRegisterWriteBack -> 1.B,
      ),
      _.io.registerSources.expect(
        _.index(0) -> 3.U,
      )
    )

  it should "handle Opcode.immediate" in
    testFetch(24)(
      "addi x2, x3, 4",
      "slli x2, x3, 4",
      "slti x2, x3, 4",
      "sltiu x2, x3, 4",
      "xori x2, x3, 4",
      "srli x2, x3, 4",
      "srai x2, x3, 4",
      "ori x2, x3, 4",
      "andi x2, x3, 4",
    )(
      _.downstream.reg.control.expect(
        _.isJal -> 0.B,
        _.isJalr -> 0.B,
        _.isBranch -> 0.B,
        _.isLoad -> 0.B,
        _.isStore -> 0.B,
        _.isLui -> 0.B,
        _.isImmediate -> 1.B,
        _.isSystem -> 0.B,
        _.isRegister -> 0.B,
        _.destinationIsNonZero -> 1.B,
        _.hasRegisterWriteBack -> 1.B,
        _.instructionType -> InstructionType.I
      ),
      _.io.registerSources.expect(
        _.index(0) -> 3.U,
      )
    )
  it should "handle Opcode.auipc" in
    testFetch(128)(
      "auipc x3, 0xdead"
    )(
      _.downstream.reg.control.expect(
        _.isJal -> 0.B,
        _.isJalr -> 0.B,
        _.isBranch -> 0.B,
        _.isLoad -> 0.B,
        _.isStore -> 0.B,
        _.isLui -> 0.B,
        _.isImmediate -> 0.B,
        _.isSystem -> 0.B,
        _.isRegister -> 0.B,
        _.destinationIsNonZero -> 1.B,
        _.hasRegisterWriteBack -> 1.B,
        _.instructionType -> InstructionType.U
      ),
    )

  it should "handle Opcode.store" in
    testFetch(128)(
      "sb x4, 5(x6)",
      "sh x4, 5(x6)",
      "sw x4, 5(x6)",
    )(
      _.downstream.reg.control.expect(
        _.isJal -> 0.B,
        _.isJalr -> 0.B,
        _.isBranch -> 0.B,
        _.isLoad -> 0.B,
        _.isStore -> 1.B,
        _.isLui -> 0.B,
        _.isImmediate -> 0.B,
        _.isSystem -> 0.B,
        _.isRegister -> 0.B,
        _.hasRegisterWriteBack -> 0.B,
        _.instructionType -> InstructionType.S
      ),
      _.io.registerSources.expect(
        _.index(0) -> 6.U,
        _.index(1) -> 4.U
      )
    )
  it should "handle Opcode.register" in
    testFetch(260)(
      "add x8, x10, x31",
      "sub x8, x10, x31",
      "sll x8, x10, x31",
      "slt x8, x10, x31",
      "sltu x8, x10, x31",
      "xor x8, x10, x31",
      "srl x8, x10, x31",
      "sra x8, x10, x31",
      "or x8, x10, x31",
      "and x8, x10, x31",
    )(
      _.downstream.reg.control.expect(
        _.isJal -> 0.B,
        _.isJalr -> 0.B,
        _.isBranch -> 0.B,
        _.isLoad -> 0.B,
        _.isStore -> 0.B,
        _.isLui -> 0.B,
        _.isImmediate -> 0.B,
        _.isSystem -> 0.B,
        _.isRegister -> 1.B,
        _.destinationIsNonZero -> 1.B,
        _.hasRegisterWriteBack -> 1.B,
        _.instructionType -> InstructionType.R
      ),
      _.io.registerSources.expect(
        _.index(0) -> 10.U,
        _.index(1) -> 31.U
      )
    )
  it should "handle Opcode.lui" in
    testFetch(9990)(
      "lui x22, 0xabcde"
    )(
      _.downstream.reg.control.expect(
        _.isJal -> 0.B,
        _.isJalr -> 0.B,
        _.isBranch -> 0.B,
        _.isLoad -> 0.B,
        _.isStore -> 0.B,
        _.isLui -> 1.B,
        _.isImmediate -> 0.B,
        _.isSystem -> 0.B,
        _.isRegister -> 0.B,
        _.destinationIsNonZero -> 1.B,
        _.hasRegisterWriteBack -> 1.B,
        _.instructionType -> InstructionType.U
      ),
    )
  it should "handle Opcode.branch" in
    testFetch(80)(
      "beq x20, x22, .-24",
      "bne x20, x22, .-24",
      "blt x20, x22, .-24",
      "bge x20, x22, .-24",
      "bltu x20, x22, .-24",
      "bgeu x20, x22, .-24",
    )(
      _.downstream.reg.control.expect(
        _.isJal -> 0.B,
        _.isJalr -> 0.B,
        _.isBranch -> 1.B,
        _.isLoad -> 0.B,
        _.isStore -> 0.B,
        _.isLui -> 0.B,
        _.isImmediate -> 0.B,
        _.isSystem -> 0.B,
        _.isRegister -> 0.B,
        _.hasRegisterWriteBack -> 0.B,
        _.instructionType -> InstructionType.B,
      ),
      _.io.registerSources.expect(
        _.index(0) -> 20.U,
        _.index(1) -> 22.U
      )
    )
  it should "handle Opcode.jal" in
    testFetch(100)(
      "jal x1, 28",
    )(
      _.downstream.reg.control.expect(
        _.isJal -> 1.B,
        _.isJalr -> 0.B,
        _.isBranch -> 0.B,
        _.isLoad -> 0.B,
        _.isStore -> 0.B,
        _.isLui -> 0.B,
        _.isImmediate -> 0.B,
        _.isSystem -> 0.B,
        _.isRegister -> 0.B,
        _.destinationIsNonZero -> 1.B,
        _.hasRegisterWriteBack -> 1.B,
        _.instructionType -> InstructionType.J
      ),
    )
  it should "handle Opcode.jalr" in
    testFetch(700)(
      "jalr x1, -24(x9)"
    )(
      _.downstream.reg.control.expect(
        _.isJal -> 0.B,
        _.isJalr -> 1.B,
        _.isBranch -> 0.B,
        _.isLoad -> 0.B,
        _.isStore -> 0.B,
        _.isLui -> 0.B,
        _.isImmediate -> 0.B,
        _.isSystem -> 0.B,
        _.isRegister -> 0.B,
        _.destinationIsNonZero -> 1.B,
        _.hasRegisterWriteBack -> 1.B,
        _.instructionType -> InstructionType.I
      ),
      _.io.registerSources.expect(
        _.index(0) -> 9.U,
      )
    )
  it should "handle Opcode.system with csr" in
    testFetch(4)(
      "csrrw x27, 64, x4",
      "csrrs x27, 64, x4",
      "csrrc x27, 64, x4",
      "csrrwi x27, 64, 4",
      "csrrsi x27, 64, 4",
      "csrrci x27, 64, 4",
    )(
      _.downstream.reg.control.expect(
        _.isJal -> 0.B,
        _.isJalr -> 0.B,
        _.isBranch -> 0.B,
        _.isLoad -> 0.B,
        _.isStore -> 0.B,
        _.isLui -> 0.B,
        _.isImmediate -> 0.B,
        _.isSystem -> 1.B,
        _.isRegister -> 0.B,
        _.destinationIsNonZero -> 1.B,
        _.hasRegisterWriteBack -> 1.B,
        _.instructionType -> InstructionType.I
      ),
      _.io.registerSources.expect(
        _.index(0) -> 4.U,
      )
    )
}


 */