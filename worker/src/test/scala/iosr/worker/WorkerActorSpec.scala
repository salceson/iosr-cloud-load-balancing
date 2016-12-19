package iosr.worker

import java.nio.file.{Files, Paths}

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit, TestProbe}
import iosr.Messages._
import iosr.filters.ScaleParams
import iosr.worker.WorkerActor._
import org.specs2.mutable.SpecLike
import org.specs2.specification.{AfterAll, Scope}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

abstract class WithSetup(implicit system: ActorSystem) extends Scope {
  val worker = TestFSMRef(new WorkerActor, s"worker-${Random.nextInt}")
  val supervisorProbe = TestProbe()
  val monitoringProbe = TestProbe()
  val supervisorProbePath = supervisorProbe.ref.path
  val monitoringProbePath = monitoringProbe.ref.path
  val duration = 2 seconds

  def sendStartup(): Unit = {
    worker ! Startup(supervisorProbePath, monitoringProbePath, 15 seconds)
  }

  def registerWorker(): Unit = {
    sendStartup()
    supervisorProbe.receiveOne(duration)
    monitoringProbe.receiveOne(duration)
    supervisorProbe.send(worker, RegisterWorkerAck)
  }
}

class WorkerActorSpec extends TestKit(ActorSystem("Worker")) with ImplicitSender with SpecLike with AfterAll {
  private val testImageURI = getClass.getClassLoader.getResource("test.jpg").toURI
  private val simpleResultURI = getClass.getClassLoader.getResource("simple-result.png").toURI
  private val testImage = Files.readAllBytes(Paths.get(testImageURI))
  private val simpleResult = Files.readAllBytes(Paths.get(simpleResultURI))

  sequential

  "Worker actor" should {
    "start in initial state and empty data" in new WithSetup {
      worker.stateName mustEqual Initial
      worker.stateData mustEqual EmptyData
    }

    "properly register itself in supervisor and transition to registering state" in new WithSetup {
      sendStartup()
      supervisorProbe.expectMsg(duration, RegisterWorker)
      monitoringProbe.expectMsg(duration, RegisterWorker)
      worker.stateName mustEqual Registering
    }

    "transition to running state after supervisor sends registration acknowledgement" in new WithSetup {
      registerWorker()
      worker.stateName mustEqual Running
    }

    "properly respond to simple request when in running state" in new WithSetup {
      registerWorker()
      supervisorProbe.send(worker, Request("test", testImage, List(
        ScaleParams(1920, 1200, preserveRatio = true)
      )))
      val responseAny = supervisorProbe.receiveOne(10 seconds)
      responseAny must beAnInstanceOf[Response]
      val response = responseAny.asInstanceOf[Response]
      response.id mustEqual "test"
      response.image.length mustEqual simpleResult.length
      (0 until simpleResult.length) foreach { i => response.image(i) mustEqual simpleResult(i) }
    }

    "properly transition to deregistering state and send supervisor message when monitoring wants worker to" +
      " terminate" in new WithSetup {
      registerWorker()
      supervisorProbe.send(worker, Deregister)
      supervisorProbe.receiveOne(duration) mustEqual DeregisterWorker
      worker.stateName mustEqual Deregistering
    }

    "properly transition to terminating state, send message to monitoring when monitoring wants worker to terminate" +
      " and supervisor sends its acknowledgement" in new WithSetup {
      registerWorker()
      supervisorProbe.send(worker, Deregister)
      supervisorProbe.receiveOne(duration) mustEqual DeregisterWorker
      supervisorProbe.send(worker, DeregisterWorkerAck)
      worker.stateName mustEqual Terminating
    }

    "process requests received until termination acknowledgement" in new WithSetup {
      registerWorker()
      supervisorProbe.send(worker, Deregister)
      supervisorProbe.receiveOne(duration) mustEqual DeregisterWorker
      supervisorProbe.send(worker, Request("test-1", testImage, List(
        ScaleParams(1920, 1200, preserveRatio = true)
      )))
      supervisorProbe.send(worker, Request("test-2", testImage, List(
        ScaleParams(1920, 1200, preserveRatio = true)
      )))
      supervisorProbe.send(worker, Request("test-3", testImage, List(
        ScaleParams(1920, 1200, preserveRatio = true)
      )))
      supervisorProbe.send(worker, Request("test-4", testImage, List(
        ScaleParams(1920, 1200, preserveRatio = true)
      )))
      supervisorProbe.send(worker, Request("test-5", testImage, List(
        ScaleParams(1920, 1200, preserveRatio = true)
      )))
      val received = supervisorProbe.receiveN(5, 30 seconds)
      received foreach { msg =>
        msg must beAnInstanceOf[Response]
      }
      supervisorProbe.send(worker, DeregisterWorkerAck)
    }
  }

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
