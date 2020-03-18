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

package uk.gov.hmrc.enrolmentsorchestrator.controllers

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.enrolmentsorchestrator.config.AppConfig
import uk.gov.hmrc.enrolmentsorchestrator.models.{AgentDeleteRequest, AgentDeleteResponse}
import uk.gov.hmrc.enrolmentsorchestrator.services.EnrolmentsStoreService
import uk.gov.hmrc.enrolmentsorchestrator.{AuditHelper, AuthHelper, UnitSpec}
import uk.gov.hmrc.http.{HttpResponse, Upstream4xxResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ES9DeleteControllerSpec extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite with AuthHelper with AuditHelper {

  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  val mockEnrolmentsStoreService: EnrolmentsStoreService = mock[EnrolmentsStoreService]

  val controller = new ES9DeleteController(appConfig, mockAuditService, authService, Helpers.stubControllerComponents(), mockEnrolmentsStoreService)

  val unauthedRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("DELETE", "/")
  val authedRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("DELETE", "/").withHeaders(AUTHORIZATION -> s"Basic ${encodeToBase64("AgentTermDESUser:password")}")

  val testARN = "AARN123"
  val testTerminationDate: Long = DateTime.now.toInstant.getMillis
  override val testAgentDeleteRequest = AgentDeleteRequest(testARN, testTerminationDate)

  "DELETE /enrolments-orchestrator/agents/:ARN?terminationDate=Option[Long] ?= None" should {

    "return 200, Request received and the attempt at deletion will be processed" in {
      val testHttpResponse = HttpResponse(204, responseString = Some("done"))
      val testAgentDeleteResponse = AgentDeleteResponse(testARN, testTerminationDate, success = true, 204, None)
      val extendedDataEventRequest = auditDeleteRequestEvent(testAgentDeleteRequest)
      val extendedDataEventResponse = auditAgentDeleteResponseEvent(testAgentDeleteResponse)

      when(mockEnrolmentsStoreService.terminationByEnrolmentKey(any())(any(), any())).thenReturn(Future.successful(testHttpResponse))
      auditMockSetup(testAgentDeleteResponse, extendedDataEventRequest, extendedDataEventResponse)

      val result = controller.es9Delete(testARN, Some(testTerminationDate))(authedRequest)

      status(result) shouldBe OK
      verifyAuditEvents(testAgentDeleteResponse)
    }

    "return 401, Request received but request without a valid auth token" in {
      val testAgentDeleteResponse = AgentDeleteResponse(testARN, testTerminationDate, success = false, 401, Some("BasicAuthentication failed"))
      val extendedDataEventRequest = auditDeleteRequestEvent(testAgentDeleteRequest)
      val extendedDataEventResponse = auditAgentDeleteResponseEvent(testAgentDeleteResponse)

      auditMockSetup(testAgentDeleteResponse, extendedDataEventRequest, extendedDataEventResponse)

      val result = controller.es9Delete(testARN, Some(testTerminationDate))(unauthedRequest)

      status(result) shouldBe UNAUTHORIZED
      verifyAuditEvents(testAgentDeleteResponse)
    }

    "return 401, Request received but tax-enrolment throw not unauthed response" in {
      val testAgentDeleteResponse = AgentDeleteResponse(testARN, testTerminationDate, success = false, 401, Some("notAuthed"))
      val extendedDataEventRequest = auditDeleteRequestEvent(testAgentDeleteRequest)
      val extendedDataEventResponse = auditAgentDeleteResponseEvent(testAgentDeleteResponse)

      when(mockEnrolmentsStoreService.terminationByEnrolmentKey(any())(any(), any())).thenReturn(Future.failed(Upstream4xxResponse("notAuthed", 401, 401)))
      auditMockSetup(testAgentDeleteResponse, extendedDataEventRequest, extendedDataEventResponse)

      val result = controller.es9Delete(testARN, Some(testTerminationDate))(authedRequest)

      status(result) shouldBe UNAUTHORIZED
      verifyAuditEvents(testAgentDeleteResponse)
    }

    "return 500 if down stream services return 500" in {
      val testHttpResponse = HttpResponse(500, responseString = Some("error"))
      val testAgentDeleteResponse = AgentDeleteResponse(testARN, testTerminationDate, success = false, 500, Some("error"))
      val extendedDataEventRequest = auditDeleteRequestEvent(testAgentDeleteRequest)
      val extendedDataEventResponse = auditAgentDeleteResponseEvent(testAgentDeleteResponse)

      when(mockEnrolmentsStoreService.terminationByEnrolmentKey(any())(any(), any())).thenReturn(Future.successful(testHttpResponse))
      auditMockSetup(testAgentDeleteResponse, extendedDataEventRequest, extendedDataEventResponse)

      val result = controller.es9Delete(testARN, Some(testTerminationDate))(authedRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      verifyAuditEvents(testAgentDeleteResponse)
    }

    "return 500 if there are anything wrong with the service" in {
      val testAgentDeleteResponse = AgentDeleteResponse(testARN, testTerminationDate, success = false, 500, Some("Internal service error"))
      val extendedDataEventRequest = auditDeleteRequestEvent(testAgentDeleteRequest)
      val extendedDataEventResponse = auditAgentDeleteResponseEvent(testAgentDeleteResponse)

      when(mockEnrolmentsStoreService.terminationByEnrolmentKey(any())(any(), any())).thenReturn(Future.failed(new RuntimeException))
      auditMockSetup(testAgentDeleteResponse, extendedDataEventRequest, extendedDataEventResponse)

      val result = controller.es9Delete(testARN, Some(testTerminationDate))(authedRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      verifyAuditEvents(testAgentDeleteResponse)
    }

  }

}
