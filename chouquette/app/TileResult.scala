package chouquette

import java.io.File
import java.nio.file.Path


case class TileResult(
  tiffPath: String,
  tiles: TilesDirectory
)


case class TilesDirectory(base: String, zDirectories: ZDirectory*)

object TilesDirectory {
  def fromOutput(output: Path): TilesDirectory = {
    val zDirectories = output.toFile.listFiles
      .filter(_.isDirectory)
      .sortBy(_.getName)
      .map(ZDirectory.fromFile)
    TilesDirectory(output.toString, zDirectories: _*)
  }
}


case class ZDirectory(name: String, xDirectories: XDirectory*)

object ZDirectory {
  def fromFile(zDirectory: File): ZDirectory = {
    val xDirectories = zDirectory.listFiles
      .sortBy(_.getName)
      .map(XDirectory.fromFile)
    ZDirectory(zDirectory.getName, xDirectories: _*)
  }
}


case class XDirectory(name: String, yDirectories: String*)

object XDirectory {
  def fromFile(xDirectory: File): XDirectory = {
    val yDirectories = xDirectory.listFiles
      .sortBy(_.getName)
      .map(_.getName)
    XDirectory(xDirectory.getName, yDirectories: _*)
  }
}
