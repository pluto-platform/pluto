package lib.riscv


import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}

object Assemble {
  def assembleToBytes(instruction: String, instructions: String*): Seq[Int] = assembleToBytes(instruction +: instructions)
  def assembleToBytes(instructions: Seq[String]): Seq[Int] = {

    import scala.sys.process._

    val writer = new PrintWriter(new File("build/temp.s"))
    writer.println(instructions.mkString("\n"))
    writer.close()
    println("riscv64-unknown-elf-as -o build/temp.o build/temp.s -march=rv32i -mabi=ilp32".!!)
    println("riscv64-unknown-elf-objcopy -O binary build/temp.o build/temp.bin".!!)

    Files.readAllBytes(Paths.get("build/temp.bin"))
      .map(_.toInt & 0xFF)
      .toSeq
  }
  def assembleToWords(instructions: Seq[String]): Seq[Long] =
    assembleToBytes(instructions).map(_.toLong).grouped(4).map(a => a(0) | (a(1) << 8) | (a(2) << 16) | (a(3) << 24)).toSeq
  def assembleToWords(instruction: String, instructions: String*): Seq[Long] = assembleToWords(instruction +: instructions)
}
