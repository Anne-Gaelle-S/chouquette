package service67.services

import scala.concurrent._

import javax.inject._

import service67.MyExecutionContext
import service67.controllers.HdfsPuttable


@Singleton
class HdfsPutter @Inject()()(
  implicit ec: MyExecutionContext
) extends HdfsPuttable {

  def putHdfs(hdfsPath: String)
             (tilesPath: String): Future[String] = ???

}
