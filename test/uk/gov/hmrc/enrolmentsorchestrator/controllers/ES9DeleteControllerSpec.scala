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
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.enrolmentsorchestrator.UnitSpec
import uk.gov.hmrc.enrolmentsorchestrator.models.{AgentDeleteRequest, AgentDeleteResponse}
import uk.gov.hmrc.enrolmentsorchestrator.services.{AuditService, EnrolmentsStoreService}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ES9DeleteControllerSpec extends UnitSpec with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  val mockEnrolmentsStoreService: EnrolmentsStoreService = mock[EnrolmentsStoreService]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockAuditService: AuditService = mock[AuditService]
  val auditService: AuditService = new AuditService(mockAuditConnector)
  override def afterEach = reset(mockAuditService)

  private val fakeRequest = FakeRequest("DELETE", "/")

  private val controller = new ES9DeleteController(Helpers.stubControllerComponents(), mockEnrolmentsStoreService, mockAuditConnector, mockAuditService)


  val testARN = "AARN123"
  val testTerminationDate: Long = DateTime.now.toInstant.getMillis
  val testAgentDeleteRequest = AgentDeleteRequest(testARN, testTerminationDate)


  "DELETE /enrolments-orchestrator/agents/:ARN?terminationDate=Option[Long] ?= None" should {

    "return 204, Request received and the attempt at deletion will be processed" in {

      val testHttpResponse = HttpResponse(204, responseString = Some("done"))
      val testAgentDeleteResponse = AgentDeleteResponse(testARN, testTerminationDate, true, 204, None)
      val extendedDataEventRequest = auditService.auditDeleteRequestEvent(testAgentDeleteRequest)
      val extendedDataEventResponse = auditService.auditAgentDeleteResponseEvent(testAgentDeleteResponse)

      when(mockEnrolmentsStoreService.terminationByEnrolmentKey(any())(any(), any())).thenReturn(Future.successful(testHttpResponse))
      when(mockAuditService.auditDeleteRequestEvent(eqTo(testAgentDeleteRequest))).thenReturn(extendedDataEventRequest)
      when(mockAuditService.auditAgentDeleteResponseEvent(eqTo(testAgentDeleteResponse))).thenReturn(extendedDataEventResponse)

      val result = controller.es9Delete(testARN, Some(testTerminationDate))(fakeRequest)

      status(result) shouldBe NO_CONTENT
      verify(mockAuditService, times(1)).auditDeleteRequestEvent(eqTo(testAgentDeleteRequest))
      verify(mockAuditService, times(1)).auditAgentDeleteResponseEvent(eqTo(testAgentDeleteResponse))
    }

    "return 500 if down stream services return 500" in {

      val testHttpResponse = HttpResponse(500, responseString = Some("error"))
      val testAgentDeleteResponse = AgentDeleteResponse(testARN, testTerminationDate, false, 500, Some("error"))
      val extendedDataEventRequest = auditService.auditDeleteRequestEvent(testAgentDeleteRequest)
      val extendedDataEventResponse = auditService.auditAgentDeleteResponseEvent(testAgentDeleteResponse)

      when(mockEnrolmentsStoreService.terminationByEnrolmentKey(any())(any(), any())).thenReturn(Future.successful(testHttpResponse))
      when(mockAuditService.auditDeleteRequestEvent(eqTo(testAgentDeleteRequest))).thenReturn(extendedDataEventRequest)
      when(mockAuditService.auditAgentDeleteResponseEvent(eqTo(testAgentDeleteResponse))).thenReturn(extendedDataEventResponse)

      val result = controller.es9Delete(testARN, Some(testTerminationDate))(fakeRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockAuditService, times(1)).auditDeleteRequestEvent(eqTo(testAgentDeleteRequest))
      verify(mockAuditService, times(1)).auditAgentDeleteResponseEvent(eqTo(testAgentDeleteResponse))
    }

    "return 500 if there are anything wrong with the service" in {
      val testAgentDeleteResponse = AgentDeleteResponse(testARN, testTerminationDate, false, 500, Some("Internal service error"))
      val extendedDataEventRequest = auditService.auditDeleteRequestEvent(testAgentDeleteRequest)
      val extendedDataEventResponse = auditService.auditAgentDeleteResponseEvent(testAgentDeleteResponse)

      when(mockEnrolmentsStoreService.terminationByEnrolmentKey(any())(any(), any())).thenReturn(Future.failed(new RuntimeException))
      when(mockAuditService.auditDeleteRequestEvent(eqTo(testAgentDeleteRequest))).thenReturn(extendedDataEventRequest)
      when(mockAuditService.auditAgentDeleteResponseEvent(eqTo(testAgentDeleteResponse))).thenReturn(extendedDataEventResponse)

      val result = controller.es9Delete(testARN, Some(testTerminationDate))(fakeRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      verify(mockAuditService, times(1)).auditDeleteRequestEvent(eqTo(testAgentDeleteRequest))
      verify(mockAuditService, times(1)).auditAgentDeleteResponseEvent(eqTo(testAgentDeleteResponse))
    }

  }
}
