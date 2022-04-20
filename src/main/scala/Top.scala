import chisel3._
import chisel3.experimental.{ChiselAnnotation, annotate}
import chisel3.util.experimental.loadMemoryFromFileInline
import firrtl.annotations.MemorySynthInit
import lib.util.BundleItemAssignment
import cores.lib.ControlTypes.{MemoryAccessResult, MemoryOperation}
import peripherals.uart.{UartReceiver, UartTransmitter}
import lib.util.SeqToTransposable

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}

class Top extends Module {
  val io = IO(new Bundle {
    val led = Output(Bool())
    val pc = Output(UInt(10.W))
    val rx = Input(Bool())
    val tx = Output(Bool())
  })
  val pipeline = Module(new cores.nix.Pipeline)

  annotate(new ChiselAnnotation {
    override def toFirrtl = MemorySynthInit
  })

  val programBinary = Files.readAllBytes(Paths.get("../pluto-rt/rust.bin"))
    .map(_.toLong & 0xFF)
    .map(BigInt(_)) ++ Seq.fill(16)(BigInt(0))
  val program = programBinary
    .grouped(4)
    .map(a => a(0) | (a(1) << 8) | (a(2) << 16) | (a(3) << 24))
    .toSeq


  def writeHexSeqToFile(seq: Seq[BigInt], fileName: String): Unit = {
    val writer = new PrintWriter(new File(fileName))
    writer.write(seq.map(_.toString(16)).mkString("\n"))
    writer.close()
  }

  val receiver = Module(new UartReceiver) // 434
  receiver.io.period := 434.U
  receiver.io.rx := io.rx
  val transmitter = Module(new UartTransmitter)
  transmitter.io.set(
    _.send.valid := 0.B,
    _.send.bits := DontCare,
    _.period := 434.U
  )
  io.tx := transmitter.io.tx

  val rom = VecInit(program.map(_.U(32.W))).apply(RegNext(pipeline.io.instructionChannel.request.bits.address(31,2), 0.U))
  /*
  SyncReadMem(2048, UInt(32.W))
  loadMemoryFromFileInline(rom, "prog.txt")
  writeHexSeqToFile(simpleBlink, "build/prog.txt")

   */
  pipeline.io.instructionChannel.set(
    _.request.ready := 1.B,
    _.response.valid := RegNext(pipeline.io.instructionChannel.request.valid, 0.B),
    _.response.bits.instruction := rom
  )


  val ram = SyncReadMem(1024, UInt(32.W))
  val ledReg = RegInit(0.B)
  io.led := ledReg
  io.pc := pipeline.io.pc

  pipeline.io.dataChannel.request.ready := 1.B
  pipeline.io.dataChannel.response.set(
    _.valid := 1.B,
    _.bits.result := MemoryAccessResult.Success,
    _.bits.readData := 0.U
  )

  val readRequest = pipeline.io.dataChannel.request.valid && pipeline.io.dataChannel.request.bits.op === MemoryOperation.Read
  val address = pipeline.io.dataChannel.request.bits.address
  val ramRead = ram.read(address(12,2))
  //val romRead = rom.read(address(31,2))
  val addrPipe = RegNext(address, 0.U)
  val romDat = RegNext(Seq.tabulate(4)(i => VecInit(programBinary.map(_.U(8.W))).apply(address + (3-i).U)).reduce(_ ## _), 0.U)
  when(RegNext(readRequest, 0.B)) {
    val rdData = WireDefault(0.U)
    when(addrPipe <= programBinary.length.U) {
      rdData := romDat
    }.elsewhen(0x80000000L.U <= addrPipe && addrPipe < (0x80000000L+1024).U) {
      rdData := ramRead
    }.elsewhen(addrPipe === 0x10000.U) {
      rdData := ledReg.asUInt
    }.elsewhen(addrPipe === 0x20000.U) {
      rdData := transmitter.io.send.ready.asUInt
    }
    pipeline.io.dataChannel.response.bits.readData := rdData
  }

  val writeRequest = pipeline.io.dataChannel.request.valid && pipeline.io.dataChannel.request.bits.op === MemoryOperation.Write
  when(writeRequest) {

    val address = pipeline.io.dataChannel.request.bits.address
    val wrData = pipeline.io.dataChannel.request.bits.writeData
    when(0x80000000L.U <= address && address < (0x80000000L+1024).U) {
      ram.write(address(31,2), wrData)
    }.elsewhen(address === 0x10000.U) {
      ledReg := wrData(0)
    }.elsewhen(address === 0x20000.U) {
      transmitter.io.send.set(
        _.valid := 1.B,
        _.bits := wrData(7,0)
      )
    }

  }

}

object TopEmitter extends App {
  emitVerilog(new Top, Array("--target-dir","build"))
}
