package chouquette.services

import scala.concurrent._

import javax.inject._

import chouquette._
import chouquette.controllers.{ HdfsPuttable, HdfsServer }


@Singleton
class HdfsPutter @Inject()()(
  implicit ec: MyExecutionContext
) extends HdfsPuttable {

  def putHdfs(hdfsServer: HdfsServer, hdfsPath: String)
             (tileResult: TileResult): Future[String] = ???

}
