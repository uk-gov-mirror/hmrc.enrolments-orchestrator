/*
 * Copyright 2021 HM Revenue & Customs
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

import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.enrolmentsorchestrator.UnitSpec
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.{ExecutionContext, Future}

class AuditServiceSpec extends UnitSpec with MockitoSugar {

  "The auditing service" should {
    "send the correct audit event for an agent delete request" in new Setup {
      val captor = ArgCaptor[ExtendedDataEvent]
      val arn = "agent ref no"
      val timestamp = 1234567890L

      when(mockAuditConnector.sendExtendedEvent(captor)(any, any)).thenReturn(Future.successful(AuditResult.Disabled))

      auditService.auditDeleteRequest(arn, timestamp)(requestWithHeaders)

      verify(mockAuditConnector).sendExtendedEvent(captor)(any, any)
      val event = captor.value

      event.auditSource shouldBe "enrolments-orchestrator"
      event.auditType shouldBe "AgentDeleteRequest"
      event.detail shouldBe Json.obj(
        "agentReferenceNumber" -> arn,
        "terminationDate" -> timestamp
      )
      event.tags shouldBe Map(
        "clientIP" -> clientIp,
        "path" -> requestWithHeaders.path,
        HeaderNames.xSessionId -> sessionId,
        HeaderNames.akamaiReputation -> clientReputation,
        HeaderNames.xRequestId -> requestId,
        HeaderNames.deviceID -> deviceId,
        "clientPort" -> clientPort,
        "transactionName" -> "HMRC Gateway - Enrolments Orchestrator - Agent Delete Request"
      )
    }

    "send the correct audit event for a successful agent delete response" in new Setup {
      val captor = ArgCaptor[ExtendedDataEvent]
      val arn = "agent ref no"
      val timestamp = 1234567890L

      when(mockAuditConnector.sendExtendedEvent(captor)(any, any)).thenReturn(Future.successful(AuditResult.Disabled))

      auditService.auditSuccessfulAgentDeleteResponse(arn, timestamp, 200)(requestWithHeaders)

      verify(mockAuditConnector).sendExtendedEvent(captor)(any, any)
      val event = captor.value

      event.auditSource shouldBe "enrolments-orchestrator"
      event.auditType shouldBe "AgentDeleteResponse"
      event.detail shouldBe Json.obj(
        "agentReferenceNumber" -> arn,
        "terminationDate" -> timestamp,
        "statusCode" -> 200,
        "success" -> true
      )
      event.tags shouldBe Map(
        "clientIP" -> clientIp,
        "path" -> requestWithHeaders.path,
        HeaderNames.xSessionId -> sessionId,
        HeaderNames.akamaiReputation -> clientReputation,
        HeaderNames.xRequestId -> requestId,
        HeaderNames.deviceID -> deviceId,
        "clientPort" -> clientPort,
        "transactionName" -> "HMRC Gateway - Enrolments Orchestrator - Agent Delete Response"
      )
    }

    "send the correct audit event for a failed agent delete response" in new Setup {
      val captor = ArgCaptor[ExtendedDataEvent]
      val arn = "agent ref no"
      val timestamp = 1234567890L

      when(mockAuditConnector.sendExtendedEvent(captor)(any, any)).thenReturn(Future.successful(AuditResult.Disabled))

      auditService.auditFailedAgentDeleteResponse(arn, timestamp, 400, "bad stuff happened")(requestWithHeaders)

      verify(mockAuditConnector).sendExtendedEvent(captor)(any, any)
      val event = captor.value

      event.auditSource shouldBe "enrolments-orchestrator"
      event.auditType shouldBe "AgentDeleteResponse"
      event.detail shouldBe Json.obj(
        "agentReferenceNumber" -> arn,
        "terminationDate" -> timestamp,
        "statusCode" -> 400,
        "success" -> false,
        "failureReason" -> "bad stuff happened"
      )
      event.tags shouldBe Map(
        "clientIP" -> clientIp,
        "path" -> requestWithHeaders.path,
        HeaderNames.xSessionId -> sessionId,
        HeaderNames.akamaiReputation -> clientReputation,
        HeaderNames.xRequestId -> requestId,
        HeaderNames.deviceID -> deviceId,
        "clientPort" -> clientPort,
        "transactionName" -> "HMRC Gateway - Enrolments Orchestrator - Agent Delete Response"
      )
    }
  }

  trait Setup {
    val userIdentifier = "somebody"
    val clientIp = "192.168.0.1"
    val clientPort = "443"
    val clientReputation = "totally reputable"
    val requestId = "requestId"
    val deviceId = "deviceId"
    val sessionId = "sessionId"
    val trustId = "someTrustId"

    val requestWithHeaders = FakeRequest()
      .withHeaders(HeaderNames.trueClientIp -> clientIp)
      .withHeaders(HeaderNames.trueClientPort -> clientPort)
      .withHeaders(HeaderNames.akamaiReputation -> clientReputation)
      .withHeaders(HeaderNames.xRequestId -> requestId)
      .withHeaders(HeaderNames.deviceID -> deviceId)
      .withHeaders(HeaderNames.xSessionId -> sessionId)

    val mockAuditConnector = mock[AuditConnector]

    val auditService = new AuditService(mockAuditConnector)(ExecutionContext.global)
  }
}
