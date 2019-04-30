package chouquette

import play.api.libs.json._
import play.api.libs.functional.syntax._


case class MetaData(upperLeft: Point, lowerRight: Point)

object MetaData {
  implicit val metaDataWrites: Writes[MetaData] = Json.writes[MetaData]

  def fromJson(json: JsValue): Option[MetaData] =
    for {
      Seq(upperLeftX, upperLeftY) <-
        (json \ "cornerCoordinates" \ "upperLeft").validate[Seq[Double]].asOpt
      Seq(lowerRightX, lowerRightY) <-
        (json \ "cornerCoordinates" \ "lowerRight").validate[Seq[Double]].asOpt
    } yield
      MetaData(Point(upperLeftX, upperLeftY), Point(lowerRightX, lowerRightY))
}


case class Point(x: Double, y: Double)

object Point {
  implicit val pointWrites: Writes[Point] = Json.writes[Point]
}
