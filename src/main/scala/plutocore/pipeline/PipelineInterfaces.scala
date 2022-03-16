package plutocore.pipeline

import chisel3._
import ControlTypes._

object PipelineInterfaces {

  class ToFetch extends Bundle {
    val pc = UInt(32.W)
  }

  class FetchToDecode extends Bundle {
    val pc = UInt(32.W)
    val nextPc = UInt(32.W)

    val instruction = UInt(32.W)
    val validOpcode = Bool()

    val recoveryTarget = UInt(32.W)

    val compareSelect = Vec(6, Bool())

    val control = new Bundle {
      val guess = Bool()
      val isJalr = Bool()
      val isBranch = Bool()
      val isLoad = Bool()
      val isStore = Bool()
      val isLui = Bool()
      val isImmediate = Bool()
      val isSystem = Bool()
      val isRegister = Bool()
      val aluFunIsAdd = Bool()
      val destinationIsNonZero = Bool()
      val hasRegisterWriteBack = Bool()
      val writeSourceRegister = UInt(1.W)
      val leftOperand = LeftOperand()
      val rightOperand = RightOperand()
      val instructionType = InstructionType()
    }
  }

  class DecodeToExecute extends Bundle {
    val pc = UInt(32.W)
    val source = Vec(2, UInt(5.W))
    val destination = UInt(5.W)

    val operand = Vec(2, UInt(32.W))
    val writeValue = UInt(32.W)
    val csrIndex = UInt(12.W)

    val funct3 = UInt(3.W)

    val control = new Bundle {
      val isLoad = Bool()

      val acceptsForwarding = Vec(2, Bool())

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

    val control = new Bundle {
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
      val isLoad = Bool()
      val withSideEffects = new Bundle {
        val writeRegisterFile = Bool()
        val writeCsrFile = Bool()
      }
    }

  }

}