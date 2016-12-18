package iosr.monitoring

import java.io.Closeable

import akka.actor.{Actor, ActorLogging, Props}
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientBuilder}
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory
import com.typesafe.config.Config

class DockerClientActor(config: Config) extends Actor with ActorLogging {

  import DockerClientActor._

  private val dockerHost = config.getString("docker.host")
  private val dockerNetwork = config.getString("docker.network")
  private val workerImageName = config.getString("docker.worker.image.name")
  private val workerContainerNamePrefix = config.getString("docker.worker.container.name.prefix")

  private val dockerClient: DockerClient = initializeDockerClient()

  override def receive: Receive = {
    case msg: StartNewContainer =>
      startContainer(msg.workerId)
    case msg: RemoveContainer =>
      removeContainer(msg.workerId)
      sender() ! RemoveContainerAck(msg.workerId)
  }

  private def initializeDockerClient(): DockerClient = {
    val dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
      .withDockerHost(dockerHost).build()

    val dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
      .withReadTimeout(1000)
      .withConnectTimeout(1000)
      .withMaxTotalConnections(100)
      .withMaxPerRouteConnections(10)

    val dockerClient = DockerClientBuilder.getInstance(dockerClientConfig)
      .withDockerCmdExecFactory(dockerCmdExecFactory)
      .build()

    val dockerInfo = dockerClient.infoCmd().exec()
    log.info(dockerInfo.toString)
    dockerClient
  }

  def startContainer(workerId: Int): Unit = {
    log.info(s"Starting $workerId")
    val containerName = workerContainerNamePrefix + workerId
    val container = dockerClient.createContainerCmd(workerImageName)
      .withName(containerName)
      .withNetworkMode(dockerNetwork)
      .withEnv(s"JAVA_OPTS=-Dakka.remote.netty.tcp.hostname=$containerName")
      .exec()
    dockerClient.startContainerCmd(container.getId)
  }

  def removeContainer(workerId: Int): Unit = {
    val containerName = workerContainerNamePrefix + workerId
    log.info(s"Stopping $containerName")
    dockerClient.stopContainerCmd(containerName).exec()
    log.info(s"Waiting for $containerName")
    dockerClient.waitContainerCmd(containerName).exec(null)
    log.info(s"Removing $containerName")
    dockerClient.removeContainerCmd(containerName).exec()
  }

}

object DockerClientActor {

  def props(config: Config) = Props(classOf[DockerClientActor], config)

  case object Initialize

  case class StartNewContainer(workerId: Int)

  case class RemoveContainer(workerId: Int)

  case class RemoveContainerAck(workerId: Int)

}