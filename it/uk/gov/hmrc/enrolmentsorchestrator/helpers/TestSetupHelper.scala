package uk.gov.hmrc.enrolmentsorchestrator.helpers

import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import play.api.libs.json.{Json, OFormat}
import play.api.test.WsTestClient
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.integration.ServiceSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait TestSetupHelper extends WordSpecLike
  with Matchers
  with WsTestClient
  with ExternalServiceProvider
  with BeforeAndAfterEach
  with ServiceSpec
  with WireMockSetup {

  val es9DeleteBaseUrl = "/enrolments-orchestrator/agents"
  val testARN = "AARN123"
  val testGroupId = "90ccf333-65d2-4bf2-a008-01dfca702161"

  override def externalResource(serviceName: String, path: String): String = {
    val externalServicePort = services.getOrElse(serviceName, throw new IllegalArgumentException(s"$serviceName service not configured in ExternalServiceProvider"))
    s"http://localhost:$externalServicePort$path"
  }

  def authLoginApiResource(resource: String): String = externalResource("auth-login-api", resource)

  implicit val defaultTimeout: FiniteDuration = 3.minutes

  def await[A](future: Future[A])(implicit timeout: Duration): A = Await.result(future, timeout)

  def authToken: String = {
    implicit val idFormat: OFormat[EnrolmentIdentifier] = Json.format[EnrolmentIdentifier]
    implicit val enrolmentsFormat: OFormat[Enrolment] = Json.format[Enrolment]

    val arnEnrolment = Set(Enrolment("HMRC-AS-AGENT", List(EnrolmentIdentifier("ARN", "AARN123")), "Activated"))
    val request = Json.obj(
      "credId" -> "anyCredId",
      "groupIdentifier" -> testGroupId,
      "affinityGroup" -> "Agent",
      "credentialStrength" -> "strong",
      "enrolments" -> Json.toJson(arnEnrolment)
    )
    val exchangeResult = withClient { ws => await(ws.url(authLoginApiResource("/government-gateway/session/login")).post(request)) }

    exchangeResult.status shouldBe 201
    exchangeResult.header(HeaderNames.authorisation).get
  }

  override def externalServices: Seq[String] = Seq.empty
}
