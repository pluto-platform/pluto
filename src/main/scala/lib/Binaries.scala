package lib

import java.io.File
import java.nio.file.{Files, Paths}

object Binaries {

  def getFiles(dir: String): Seq[String] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).map(_.getName).toSeq
    } else {
      Seq()
    }
  }

  def loadWords(path: String): Seq[BigInt] = {
    Files.readAllBytes(Paths.get(path))
      .map(BigInt(_) & 0xFF)
      .grouped(4)
      .map(a => a(0) | (a(1) << 8) | (a(2) << 16) | (a(3) << 24))
      .toSeq
  }
  def loadBytes(path: String): Seq[Int] = {
    Files.readAllBytes(Paths.get(path))
      .map(_.toInt & 0xFF)
      .toSeq
  }

}
