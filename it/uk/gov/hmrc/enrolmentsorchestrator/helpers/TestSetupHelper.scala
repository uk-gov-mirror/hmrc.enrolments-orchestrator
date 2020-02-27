package uk.gov.hmrc.enrolmentsorchestrator.helpers

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import play.api.test.WsTestClient
import uk.gov.hmrc.integration.ServiceSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait TestSetupHelper extends WordSpecLike
  with Matchers
  with WsTestClient
  with BeforeAndAfterEach
  with ServiceSpec
  with WireMockSetup {

  val es9DeleteBaseUrl = "/enrolments-orchestrator/agents"
  val testARN = "AARN123"

  implicit val defaultTimeout: FiniteDuration = 3.minutes

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  def basicAuth(string: String): String = Base64.getEncoder.encodeToString(string.getBytes(UTF_8))

  override def externalServices: Seq[String] = Seq.empty
}
