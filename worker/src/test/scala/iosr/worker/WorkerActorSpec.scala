package iosr.worker

import java.nio.file.{Files, Paths}

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit, TestProbe}
import iosr.Messages._
import iosr.filters.ScaleParams
import iosr.worker.WorkerActor._
import org.specs2.mutable.SpecLike
import org.specs2.specification.Scope

import scala.concurrent.duration._
import scala.language.postfixOps

abstract class WithSetup(implicit system: ActorSystem) extends Scope {
  val fsm = TestFSMRef(new WorkerActor)
  val probe = TestProbe()
  val probePath = probe.ref.path
  val duration = 1 second

  def sendStartup(): Unit = {
    fsm ! Startup(probePath)
  }

  def registerWorker(): Unit = {
    sendStartup()
    probe.receiveOne(duration)
    probe.send(fsm, RegisterWorkerAck)
  }
}

class WorkerActorSpec extends TestKit(ActorSystem()) with ImplicitSender with SpecLike {
  private val testImageURI = getClass.getClassLoader.getResource("test.jpg").toURI
  private val simpleResultURI = getClass.getClassLoader.getResource("simple-result.png").toURI
  private val testImage = Files.readAllBytes(Paths.get(testImageURI))
  private val simpleResult = Files.readAllBytes(Paths.get(simpleResultURI))

  "Worker actor" should {
    "start in initial state and empty data" in new WithSetup {
      fsm.stateName mustEqual Initial
      fsm.stateData mustEqual EmptyData
    }

    "properly register itself in supervisor and transition to registering state" in new WithSetup {
      sendStartup()
      probe.receiveOne(duration) mustEqual RegisterWorker
      fsm.stateName mustEqual Registering
    }

    "transition to running state after supervisor sends registration acknowledgement" in new WithSetup {
      registerWorker()
      fsm.stateName mustEqual Running
    }

    "properly respond to simple request when in running state" in new WithSetup {
      registerWorker()
      probe.send(fsm, Request(testImage, List(
        ScaleParams(1920, 1200, preserveRatio = true)
      )))
      val responseAny = probe.receiveOne(10 seconds)
      responseAny must beAnInstanceOf[Response]
      val response = responseAny.asInstanceOf[Response]
      response.image.length mustEqual simpleResult.length
      (0 until simpleResult.length) foreach { i => response.image(i) mustEqual simpleResult(i) }
    }
  }
}
