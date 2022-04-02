package cores.nix.stages

import chisel3._
import cores.lib.ControlTypes._
import cores.lib.riscv.InstructionType
import cores.modules.ALU.AluFunction
import cores.nix.ControlTypes.{LeftOperand, RightOperand}

object Interfaces {

  class ToFetch extends Bundle {
    val pc = UInt(32.W)
  }

  class FetchToDecode extends Bundle {
    val pc = UInt(32.W)

    val instruction = UInt(32.W)
    val validOpcode = Bool()



    val control = new Bundle {
      val isJal = Bool()
      val isJalr = Bool()
      val isBranch = Bool()
      val isLoad = Bool()
      val isStore = Bool()
      val isLui = Bool()
      val isAuipc = Bool()
      val isImmediate = Bool()
      val isSystem = Bool()
      val isRegister = Bool()
      val destinationIsNonZero = Bool()
      val hasRegisterWriteBack = Bool()
      val instructionType = InstructionType()
    }
  }

  class DecodeToExecute extends Bundle {
    val pc = UInt(32.W)
    val source = Vec(2, UInt(5.W))
    val destination = UInt(5.W)

    val registerOperand = Vec(2, UInt(32.W))
    val immediate = SInt(32.W)
    val csrIndex = UInt(12.W)

    val funct3 = UInt(3.W)

    val control = new Bundle {
      val isEcall = Bool()
      val isLoad = Bool()
      val isBranch = Bool()
      val isJal = Bool()
      val isJalr = Bool()

      val leftOperand = LeftOperand()
      val rightOperand = RightOperand()

      val aluFunction = AluFunction()

      val memoryOperation = MemoryOperation()

      val withSideEffects = new Bundle {
        val hasMemoryAccess = Bool()
        val isCsrRead = Bool()
        val isCsrWrite = Bool()
        val hasRegisterWriteBack = Bool()
      }
    }
  }

  class ExecuteToMemory extends Bundle {
    val pc = UInt(32.W)
    val destination = UInt(5.W)

    val aluResult = UInt(32.W)
    val writeValue = UInt(32.W)
    val csrIndex = UInt(12.W)
    val funct3 = UInt(3.W)

    val jump = Bool()
    val target = UInt(32.W)

    val control = new Bundle {
      val isEcall = Bool()
      val isLoad = Bool()
      val memoryOperation = MemoryOperation()
      val withSideEffects = new Bundle {
        val hasMemoryAccess = Bool()
        val isCsrWrite = Bool()
        val hasRegisterWriteBack = Bool()
      }
    }
  }

  class MemoryToWriteBack extends Bundle {
    val pc = UInt(32.W)

    val registerWriteBack = new Bundle {
      val index = UInt(5.W)
      val value = UInt(32.W)
    }
    val csrWriteBack = new Bundle {
      val index = UInt(12.W)
      val value = UInt(32.W)
    }

    val control = new Bundle {
      val isEcall = Bool()
      val isLoad = Bool()
      val withSideEffects = new Bundle {
        val writeRegisterFile = Bool()
        val writeCsrFile = Bool()
      }
    }

  }

}
