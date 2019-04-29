package chouquette.services

import scala.concurrent._
import scala.util.Try

import javax.inject._
import com.google.inject.ImplementedBy

import com.decodified.scalassh._
import net.schmizz.sshj.common.LoggerFactory
import net.schmizz.sshj.xfer.LoggingTransferListener

import chouquette._
import chouquette.controllers.{ HDFSPuttable, SSHServer }


@ImplementedBy(classOf[SSHService])
trait SSHServable {
  type StdOut = String
  type StdErr = String

  def runSSHCmd(sshServer: SSHServer, cmd: String): Try[(StdOut, StdErr)]

  def scpUpload(
    sshServer: SSHServer,
    localPath: String,
    remotePath: String): Try[Unit]
}

// hardly testable
@Singleton
class SSHService() extends SSHServable {
  def runSSHCmd(sshServer: SSHServer, cmd: String): Try[(String, String)] =
    SSH(
      sshServer.host,
      HostConfig(
        PasswordLogin(
          sshServer.user,
          PasswordProducer.fromString(sshServer.password))))(
      _.exec(cmd)
        .map(result => (result.stdOutAsString(), result.stdErrAsString())))

  def scpUpload(
      sshServer: SSHServer,
      localPath: String,
      remotePath: String
  ): Try[Unit] =
    SSH(
      sshServer.host,
      HostConfig(
        PasswordLogin(
          sshServer.user,
          PasswordProducer.fromString(sshServer.password))))(
      _.upload(localPath, remotePath))
}


@Singleton
class HDFSPutter @Inject()()(implicit ec: MyExecutionContext) extends HDFSPuttable {

  def putHDFS(sshServer: SSHServer, hdfsPath: String)
             (tileResult: TileResult): Future[String] = {
    val password = PasswordProducer.fromString(sshServer.password)
    val config = HostConfig(PasswordLogin(sshServer.user, password))
    Future(
      SSH(sshServer.host, config)(runCmdsForClient(hdfsPath, tileResult)).get)
  }

  def runCmdsForClient(hdfsPath: String, tileResult: TileResult)
                      (client: SshClient): String = {
    val lfsTiffPath = s"tmp/${tileResult.tiffPath}"
    val hdfsTiffPath = s"$hdfsPath/${tileResult.tiffPath}"
    val lfsTilesPath = s"tmp/${tileResult.tiles.base}"
    val hdfsTilesPath = s"$hdfsPath/${tileResult.tiles.base}"
    println(s"lfsTiffPath = ${lfsTiffPath}")
    println(s"hdfsTiffPath = ${hdfsTiffPath}")
    println(s"lfsTilesPath = ${lfsTilesPath}")
    println(s"hdfsTilesPath = ${hdfsTilesPath}")
    (for {
      _ <- client.exec("mkdir -p tmp")
      _ <- client.upload(tileResult.tiffPath, "tmp")
      _ <- client.upload(tileResult.tiles.base, "tmp")
      _ <- client.exec(s"hadoop fs -put $lfsTiffPath $hdfsTiffPath")
      _ <- client.exec(s"hadoop fs -put $lfsTilesPath $hdfsTilesPath")
      _ <- client.exec(s"rm -fr $lfsTiffPath $lfsTilesPath")
    } yield hdfsPath) // TODO: return tiles structure
      .get
  }

}
