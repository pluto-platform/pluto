import chisel3._
import chisel3.experimental.{ChiselAnnotation, annotate}
import chisel3.util.experimental.loadMemoryFromFileInline
import firrtl.annotations.MemorySynthInit
import lib.util.BundleItemAssignment
import cores.lib.ControlTypes.{MemoryAccessResult, MemoryOperation}
import peripherals.uart.{UartReceiver, UartTransmitter}

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}

class Top extends Module {
  val io = IO(new Bundle {
    val led = Output(Bool())
    val rx = Input(Bool())
    val tx = Output(Bool())
  })
  val pipeline = Module(new cores.nix.Pipeline)

  annotate(new ChiselAnnotation {
    override def toFirrtl = MemorySynthInit
  })

  val simpleBlink = Files.readAllBytes(Paths.get("asm/blink.bin"))
    .map(_.toLong & 0xFF)
    .grouped(4)
    .map(a => a(0) | (a(1) << 8) | (a(2) << 16) | (a(3) << 24))
    .toArray.map(BigInt(_))

  def writeHexSeqToFile(seq: Seq[BigInt], fileName: String): Unit = {
    val writer = new PrintWriter(new File(fileName))
    writer.write(seq.map(_.toString(16)).mkString("\n"))
    writer.close()
  }

  val advancedBlink = Seq(
    0x100002b7L,
    0x00028293L,
    0x00000213L,
    0x00c000efL,
    0x01c000efL,
    0xff9ff06fL,
    0x00400193L,
    0x00000113L,
    0x00110113L,
    0xfe314ee3L,
    0x00008067L,
    0x0042a023L,
    0xfff24213L,
    0x00008067L,
  )

  val receiver = Module(new UartReceiver(10417))
  receiver.io.rx := io.rx
  val transmitter = Module(new UartTransmitter(10417))
  transmitter.io.set(
    _.send.valid := 0.B,
    _.send.bits := 0.U,
  )
  io.tx := transmitter.io.tx

  val rom = SyncReadMem(256, UInt(32.W))//SyncROM(simpleBlink.map(_.U(32.W)), simulation = true)
  loadMemoryFromFileInline(rom, "prog.txt")
  writeHexSeqToFile(simpleBlink, "build/prog.txt")

  //rom.io.address := pipeline.io.instructionChannel.request.bits.address(31,2)
  pipeline.io.instructionChannel.set(
    _.request.ready := 1.B,
    _.response.valid := RegNext(1.B, 0.B),
    _.response.bits.instruction := rom.read(pipeline.io.instructionChannel.request.bits.address(31,2))
  )
  val ram = SyncReadMem(1024, UInt(32.W))
  pipeline.io.dataChannel.set(
    _.request.ready := 1.B,
    _.response.set(
      _.valid := 1.B,
      _.bits.set(
        _.readData := ram.read(pipeline.io.dataChannel.request.bits.address),
        _.result := MemoryAccessResult.Success
      )
    )
  )
  when(pipeline.io.dataChannel.request.valid && pipeline.io.dataChannel.request.bits.op === MemoryOperation.Write) {
    when(pipeline.io.dataChannel.request.bits.address(31)) {
      ram.write(pipeline.io.dataChannel.request.bits.address(11,0), pipeline.io.dataChannel.request.bits.writeData)
    } otherwise {
      rom.write(pipeline.io.dataChannel.request.bits.address(11,0), pipeline.io.dataChannel.request.bits.writeData)
    }

  }

  val ledReg = RegInit(0.B)

  when(pipeline.io.dataChannel.request.valid
    && pipeline.io.dataChannel.request.bits.op === MemoryOperation.Write
    && pipeline.io.dataChannel.request.bits.address === 0x10000000L.U) {
    ledReg := pipeline.io.dataChannel.request.bits.writeData(0)
  }
  when(pipeline.io.dataChannel.request.valid
    && pipeline.io.dataChannel.request.bits.op === MemoryOperation.Write
    && pipeline.io.dataChannel.request.bits.address === 0x20000000L.U) {
    transmitter.io.send.valid := 1.B
    transmitter.io.send.bits := pipeline.io.dataChannel.request.bits.writeData(7,0)
  }

  io.led := ledReg

}

object TopEmitter extends App {
  emitVerilog(new Top, Array("--target-dir","build"))
}
