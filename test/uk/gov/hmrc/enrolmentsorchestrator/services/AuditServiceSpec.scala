/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.enrolmentsorchestrator.services

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.enrolmentsorchestrator.LogCapturing
import uk.gov.hmrc.enrolmentsorchestrator.models._
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuditServiceSpec extends WordSpec with Matchers with ScalaFutures with MockFactory with LogCapturing {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val AUDIT_SOURCE = "enrolments-orchestrator"
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val auditService = new AuditService(mockAuditConnector)

  def auditEventAssert(auditEvent: ExtendedDataEvent, auditType: String, agentDeleteResponseJson: JsValue): Unit ={
    auditEvent.auditSource shouldBe AUDIT_SOURCE
    auditEvent.auditType shouldBe auditType
    auditEvent.detail shouldBe agentDeleteResponseJson
  }

  "The AuditHelper" should {

    "create an AgentDeleteRequest when a request is received by the service" in {
      val auditType: String = "AgentDeleteRequest"
      val testAgentDeleteRequest: AgentDeleteRequest = AgentDeleteRequest("XXXX1234567", 15797056635L)
      val agentDeleteResponseJson = Json toJson testAgentDeleteRequest
      val auditEventRequest = auditService.auditDeleteRequestEvent(testAgentDeleteRequest)

      auditEventAssert(auditEventRequest, auditType, agentDeleteResponseJson)
    }

    "create an AgentDeleteResponse when a failed response is received" in {
      val auditType: String = "AgentDeleteResponse"
      val testAgentDeleteResponse: AgentDeleteResponse = AgentDeleteResponse("XXXX1234567", 15797056635L, success = false, 500, Some("Internal Server Error"))
      val agentDeleteResponseJson = Json toJson testAgentDeleteResponse
      val auditEventResponse = auditService.auditAgentDeleteResponseEvent(testAgentDeleteResponse)

      auditEventAssert(auditEventResponse, auditType, agentDeleteResponseJson)
    }

    "create an AgentDeleteResponse when a successful response is received" in {
      val auditType: String = "AgentDeleteResponse"
      val testAgentDeleteResponse: AgentDeleteResponse = AgentDeleteResponse("XXXX1234567", 15797056635L, success = false, 200, None)
      val agentDeleteResponseJson = Json toJson testAgentDeleteResponse
      val auditEventResponse = auditService.auditAgentDeleteResponseEvent(testAgentDeleteResponse)

      auditEventAssert(auditEventResponse, auditType, agentDeleteResponseJson)
    }

    "able to recover from auditConnector failure and log the failure" in {
      withCaptureOfLoggingFrom(Logger) { logEvents =>
        val testAgentDeleteResponse: AgentDeleteResponse = AgentDeleteResponse("XXXX1234567", 15797056635L, success = false, 200, None)
        val auditEventResponse = auditService.auditAgentDeleteResponseEvent(testAgentDeleteResponse)

        (mockAuditConnector.sendExtendedEvent(_: ExtendedDataEvent)(_: HeaderCarrier, _: ExecutionContext))
          .expects(auditEventResponse, *, *)
          .returning(Future.failed(Upstream4xxResponse("", 404, 404)))

        auditService.audit(auditEventResponse).futureValue.asInstanceOf[AuditResult.Failure].msg shouldBe AuditResult.Failure("Failed sending audit message").msg

        logEvents.length shouldBe 1
        logEvents.collectFirst { case logEvent =>
          logEvent.getMessage shouldBe s"Failed sending audit message"
        }
      }
    }

  }

}

