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

package uk.gov.hmrc.enrolmentsorchestrator.controllers

import org.joda.time.DateTime
import org.mockito.scalatest.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.enrolmentsorchestrator.UnitSpec
import uk.gov.hmrc.enrolmentsorchestrator.config.AppConfig
import uk.gov.hmrc.enrolmentsorchestrator.connectors.AgentStatusChangeConnector
import uk.gov.hmrc.enrolmentsorchestrator.models.BasicAuthentication
import uk.gov.hmrc.enrolmentsorchestrator.services.{AuditService, AuthService, EnrolmentsStoreService}
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HttpResponse, Upstream4xxResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AgentControllerSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite {

  "DELETE /enrolments-orchestrator/agents/:ARN?terminationDate=Option[Long] ?= None" should {

    "return 200, Request received and the attempt at deletion will be processed" in new Setup {
      val testHttpResponse = HttpResponse(204, responseString = Some("done"))

      val testAgentStatusChangeHttpResponse = HttpResponse(200, responseString = Some("done"))
      when(mockAgentStatusChangeConnector.agentStatusChangeToTerminate(any)(any, any))
        .thenReturn(Future.successful(testAgentStatusChangeHttpResponse))

      when(mockEnrolmentsStoreService.terminationByEnrolmentKey(any)(any, any))
        .thenReturn(Future.successful(testHttpResponse))
      when(mockAuthService.createBearerToken(eqTo(Some(basicAuthHeader)))(any, any))
        .thenReturn(Future.successful(Some(Authorization("pls"))))
      doNothing.when(mockAuditService).auditDeleteRequest(any, any)(any)
      doNothing.when(mockAuditService).auditSuccessfulAgentDeleteResponse(any, any, any)(any)

      val result = controller.deleteByARN(testARN, Some(testTerminationDate))(FakeRequest().withHeaders(AUTHORIZATION -> s"Basic ${encodeToBase64("AgentTermDESUser:password")}"))
      status(result) shouldBe OK
    }

    "return 401, Request received but request without a valid BasicAuth token" in new Setup {
      when(mockAuthService.getBasicAuth(any)).thenReturn(None)
      doNothing.when(mockAuditService).auditDeleteRequest(any, any)(any)
      doNothing.when(mockAuditService).auditFailedAgentDeleteResponse(any, any, any, any)(any)

      val result = controller.deleteByARN(testARN, Some(testTerminationDate))(FakeRequest())

      status(result) shouldBe UNAUTHORIZED
    }

    "return 401, Request received but AgentStatusChange return 401 response" in new Setup {
      val testAgentStatusChangeHttpResponse = HttpResponse(401, responseString = Some("notAuthed"))
      when(mockAgentStatusChangeConnector.agentStatusChangeToTerminate(any)(any, any))
        .thenReturn(Future.successful(testAgentStatusChangeHttpResponse))
      doNothing.when(mockAuditService).auditDeleteRequest(any, any)(any)
      doNothing.when(mockAuditService).auditFailedAgentDeleteResponse(any, any, any, any)(any)

      val result = controller.deleteByARN(testARN, Some(testTerminationDate))(FakeRequest().withHeaders(AUTHORIZATION -> s"Basic ${encodeToBase64("AgentTermDESUser:password")}"))
      status(result) shouldBe UNAUTHORIZED
    }

    "return 401, Request received but AgentStatusChange throw 401 response" in new Setup {
      when(mockAgentStatusChangeConnector.agentStatusChangeToTerminate(any)(any, any))
        .thenReturn(Future.failed(Upstream4xxResponse("notAuthed", 401, 401)))
      doNothing.when(mockAuditService).auditDeleteRequest(any, any)(any)
      doNothing.when(mockAuditService).auditFailedAgentDeleteResponse(any, any, any, any)(any)

      val result = controller.deleteByARN(testARN, Some(testTerminationDate))(FakeRequest().withHeaders(AUTHORIZATION -> s"Basic ${encodeToBase64("AgentTermDESUser:password")}"))

      status(result) shouldBe UNAUTHORIZED
    }

    "return 401, Request received but tax-enrolment throw 401 response" in new Setup {
      val testAgentStatusChangeHttpResponse = HttpResponse(200, responseString = Some("done"))

      when(mockAuthService.createBearerToken(eqTo(Some(basicAuthHeader)))(any, any))
        .thenReturn(Future.successful(Some(Authorization("pls"))))
      when(mockAgentStatusChangeConnector.agentStatusChangeToTerminate(any)(any, any))
        .thenReturn(Future.successful(testAgentStatusChangeHttpResponse))
      when(mockEnrolmentsStoreService.terminationByEnrolmentKey(any)(any, any))
        .thenReturn(Future.failed(Upstream4xxResponse("notAuthed", 401, 401)))
      doNothing.when(mockAuditService).auditDeleteRequest(any, any)(any)
      doNothing.when(mockAuditService).auditFailedAgentDeleteResponse(any, any, any, any)(any)

      val result = controller.deleteByARN(testARN, Some(testTerminationDate))(FakeRequest().withHeaders(AUTHORIZATION -> s"Basic ${encodeToBase64("AgentTermDESUser:password")}"))

      status(result) shouldBe UNAUTHORIZED
    }

    "return 500 if down stream services return 500" in new Setup {
      val testHttpResponse = HttpResponse(500, responseString = Some("error"))

      val testAgentStatusChangeHttpResponse = HttpResponse(200, responseString = Some("done"))

      when(mockAuthService.createBearerToken(eqTo(Some(basicAuthHeader)))(any, any))
        .thenReturn(Future.successful(Some(Authorization("pls"))))
      when(mockAgentStatusChangeConnector.agentStatusChangeToTerminate(any)(any, any))
        .thenReturn(Future.successful(testAgentStatusChangeHttpResponse))
      when(mockEnrolmentsStoreService.terminationByEnrolmentKey(any)(any, any))
        .thenReturn(Future.successful(testHttpResponse))

      doNothing.when(mockAuditService).auditDeleteRequest(any, any)(any)
      doNothing.when(mockAuditService).auditFailedAgentDeleteResponse(any, any, any, any)(any)

      val result = controller.deleteByARN(testARN, Some(testTerminationDate))(FakeRequest().withHeaders(AUTHORIZATION -> s"Basic ${encodeToBase64("AgentTermDESUser:password")}"))

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 500 if there are anything wrong with down stream such as EnrolmentsStore" in new Setup {
      val testAgentStatusChangeHttpResponse = HttpResponse(200, responseString = Some("done"))

      when(mockAuthService.createBearerToken(eqTo(Some(basicAuthHeader)))(any, any))
        .thenReturn(Future.successful(Some(Authorization("pls"))))
      when(mockAgentStatusChangeConnector.agentStatusChangeToTerminate(any)(any, any))
        .thenReturn(Future.successful(testAgentStatusChangeHttpResponse))

      when(mockEnrolmentsStoreService.terminationByEnrolmentKey(any)(any, any))
        .thenReturn(Future.failed(new RuntimeException))
      doNothing.when(mockAuditService).auditDeleteRequest(any, any)(any)
      doNothing.when(mockAuditService).auditFailedAgentDeleteResponse(any, any, any, any)(any)

      val result = controller.deleteByARN(testARN, Some(testTerminationDate))(FakeRequest().withHeaders(AUTHORIZATION -> s"Basic ${encodeToBase64("AgentTermDESUser:password")}"))
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 500 if there are anything wrong with down stream such as AgentStatusChange" in new Setup {
      when(mockAgentStatusChangeConnector.agentStatusChangeToTerminate(any)(any, any))
        .thenReturn(Future.failed(new RuntimeException))
      doNothing.when(mockAuditService).auditDeleteRequest(any, any)(any)
      doNothing.when(mockAuditService).auditFailedAgentDeleteResponse(any, any, any, any)(any)

      val result = controller.deleteByARN(testARN, Some(testTerminationDate))(FakeRequest().withHeaders(AUTHORIZATION -> s"Basic ${encodeToBase64("AgentTermDESUser:password")}"))
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  trait Setup {
    val testARN = "AARN123"
    val testTerminationDate: Long = DateTime.now.toInstant.getMillis
    val basicAuthHeader = BasicAuthentication("AgentTermDESUser", "password")

    val appConfig = app.injector.instanceOf[AppConfig]
    val mockAgentStatusChangeConnector = mock[AgentStatusChangeConnector]
    val mockEnrolmentsStoreService = mock[EnrolmentsStoreService]
    val mockAuditService = mock[AuditService]
    val mockAuthService = mock[AuthService]

    when(mockAuthService.getBasicAuth(any)).thenReturn(Some(basicAuthHeader))

    val controller = new AgentController(
      appConfig,
      mockAuditService,
      mockAuthService,
      mockEnrolmentsStoreService,
      mockAgentStatusChangeConnector,
      Helpers.stubControllerComponents()
    )
  }

}
