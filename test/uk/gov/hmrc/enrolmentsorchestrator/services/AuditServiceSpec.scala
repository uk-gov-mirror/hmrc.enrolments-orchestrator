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

import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.enrolmentsorchestrator.models._
import uk.gov.hmrc.enrolmentsorchestrator.services.AuditService
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

class AuditServiceSpec extends WordSpec with Matchers with MockitoSugar {
  val AUDIT_SOURCE = "enrolments-orchestrator"
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val auditService = new AuditService(mockAuditConnector)
  val success = true
  val failure = false

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
      val testAgentDeleteResponse: AgentDeleteResponse = AgentDeleteResponse("XXXX1234567", 15797056635L, failure, 500, Some("Internal Server Error"))
      val agentDeleteResponseJson = Json toJson testAgentDeleteResponse
      val auditEventResponse = auditService.auditAgentDeleteResponseEvent(testAgentDeleteResponse)

      auditEventAssert(auditEventResponse, auditType, agentDeleteResponseJson)

    }

    "create an AgentDeleteResponse when a successful response is received" in {
      val auditType: String = "AgentDeleteResponse"
      val testAgentDeleteResponse: AgentDeleteResponse = AgentDeleteResponse("XXXX1234567", 15797056635L, success, 200, None)
      val agentDeleteResponseJson = Json toJson testAgentDeleteResponse
      val auditEventResponse = auditService.auditAgentDeleteResponseEvent(testAgentDeleteResponse)

      auditEventAssert(auditEventResponse, auditType, agentDeleteResponseJson)

    }
  }
}

