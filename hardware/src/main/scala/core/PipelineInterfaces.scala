package core

import chisel3._
import core.ControlTypes._
import chisel3.experimental.ChiselEnum
import core.pipeline.Instruction





object PipelineInterfaces {

  class ToFetch extends Bundle {
    val pc = UInt(32.W)
  }

  class FetchToDecode extends Bundle {
    val pc = UInt(32.W)
    val source = Vec(2, UInt(5.W))
    val destination = UInt(5.W)

    val instruction = UInt(32.W)
    val validOpcode = Bool()

    val branchTarget = UInt(32.W)

    val control = new Bundle {
      val branchWasTaken = Bool()
      val isJump = Bool()
      val isBranch = Bool()
      val destinationIsZero = Bool()
      val writeSourceRegister = WriteSourceRegister()
      val leftOperand = LeftOperand()
      val rightOperand = RightOperand()
      val instructionType = InstructionType()
    }
  }

  class DecodeToExecute extends Bundle {
    val pc = UInt(32.W)
    val source = Vec(2, UInt(5.W))
    val destination = UInt(5.W)

    val operand = Vec(2,UInt(32.W))
    val writeValue = UInt(32.W)
    val csrIndex = UInt(12.W)

    val funct3 = UInt(3.W)
    val funct7_5 = UInt(1.W)

    val control = new Bundle {
      val isLoad = Bool()
      val allowForwarding = Vec(2,Bool())

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


