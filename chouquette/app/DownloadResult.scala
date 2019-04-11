package chouquette

import java.nio.file.Path

case class DownloadResult(
  extractedDir: String,
  tiffPath: String,
  metaData: MetaData
)

object DownloadResult {
  def apply(
      extractedDir: Path,
      tiffPath: Path,
      metaData: MetaData
  ): DownloadResult =
    DownloadResult(extractedDir.toString, tiffPath.toString, metaData)
}
