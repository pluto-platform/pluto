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
    val immediate = UInt(32.W)



    val control = new Bundle {
      val isLoad = Bool()
      val allowForwarding = new Bundle {
        val left = Bool()
        val right = Bool()
      }
      val aluFunction = AluFunction()

      val memoryControl = new MemoryControl

      val writeBackSource = WriteBackSource()
    }
  }

  class ExecuteToMemory extends Bundle {
    val pc = UInt(32.W)
    val AluResult = UInt(32.W)
    val writeData = UInt(32.W)
    val registerDestination = UInt(5.W)

    val memoryControl = new MemoryControl

    val writeBackSource = WriteBackSource()
  }

  class MemoryToWriteBack extends Bundle {
    val pc = UInt(32.W)
    val AluResult = UInt(32.W)
    val writeData = UInt(32.W)
    val registerDestination = UInt(5.W)

    val writeBackSource = WriteBackSource()
  }

}


