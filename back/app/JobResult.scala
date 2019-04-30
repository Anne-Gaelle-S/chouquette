package chouquette

import play.api.libs.json._


case class JobResult(metaData: MetaData, hdfsPath: String)

object JobResult {
  implicit val jobResultWrites: Writes[JobResult] = Json.writes[JobResult]
}
