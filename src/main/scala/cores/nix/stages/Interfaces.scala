package cores.nix.stages

import chisel3._
import cores.lib.ControlTypes._
import cores.lib.riscv.InstructionType
import cores.modules.ALU.AluFunction
import cores.nix.ControlTypes.{LeftOperand, RightOperand}
import cores.lib.Exception

object Interfaces {

  class ToFetch extends Bundle {
    val pc = UInt(32.W)
  }

  class FetchToDecode extends Bundle {
    val pc = UInt(32.W)

    val instruction = UInt(32.W)
    val validOpcode = Bool()

    val isLui = Bool()
    val isAuipc = Bool()
    val isImmediate = Bool()
    val isRegister = Bool()
    val destinationIsNonZero = Bool()
    val instructionType = InstructionType()

    val withSideEffects = new Bundle {
      val isJal = Bool()
      val isJalr = Bool()
      val isBranch = Bool()
      val isLoad = Bool()
      val isStore = Bool()
      val isSystem = Bool()
      val hasRegisterWriteBack = Bool()
      val isBubble = Bool()
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

    val leftOperand = LeftOperand()
    val rightOperand = RightOperand()
    val aluFunction = AluFunction()
    val memoryOperation = MemoryOperation()

    val cause = Exception.Cause()

    val withSideEffects = new Bundle {
      val exception = Bool()
      val isLoad = Bool()
      val isBranch = Bool()
      val isJal = Bool()
      val isJalr = Bool()
      val isEcall = Bool()
      val isMret = Bool()
      val isBubble = Bool()
      val hasMemoryAccess = Bool()
      val isCsrRead = Bool()
      val isCsrWrite = Bool()
      val hasRegisterWriteBack = Bool()
    }
  }

  class ExecuteToMemory extends Bundle {
    val pc = UInt(32.W)
    val destination = UInt(5.W)

    val aluResult = UInt(32.W)
    val writeValue = UInt(32.W)
    val csrIndex = UInt(12.W)
    val funct3 = UInt(3.W)

    val target = UInt(32.W)

    val memoryOperation = MemoryOperation()

    val cause = Exception.Cause()

    val withSideEffects = new Bundle {
      val exception = Bool()
      val isLoad = Bool()
      val isEcall = Bool()
      val isMret = Bool()
      val isBubble = Bool()
      val jump = Bool()
      val hasMemoryAccess = Bool()
      val isCsrWrite = Bool()
      val hasRegisterWriteBack = Bool()
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

    val accessWidth = MemoryAccessWidth()
    val signed = Bool()

    val cause = Exception.Cause()

    val withSideEffects = new Bundle {
      val exception = Bool()
      val isLoad = Bool()
      val hasMemoryAccess = Bool()
      val jumped = Bool()
      val isEcall = Bool()
      val isBubble = Bool()
      val isMret = Bool()
      val writeRegisterFile = Bool()
      val writeCsrFile = Bool()
      val hasMemoryAccess = Bool()
    }

  }

}
