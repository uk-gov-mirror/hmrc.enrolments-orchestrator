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
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.enrolmentsorchestrator.services.EnrolmentsStoreService
import uk.gov.hmrc.enrolmentsorchestrator.{AuditHelper, AuthHelper, UnitSpec}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, Upstream4xxResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ES9DeleteControllerSpec extends UnitSpec with AuthHelper with AuditHelper {

  val mockEnrolmentsStoreService: EnrolmentsStoreService = mock[EnrolmentsStoreService]

  val testARN: String = "AARN123"
  val testTerminationDate: Long = DateTime.now.toInstant.getMillis

  val testEnrolmentKey = s"HMRC-AS-AGENT~AgentReferenceNumber~$testARN"

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("DELETE", "/")
  val controller = new ES9DeleteController(Helpers.stubControllerComponents(), mockEnrolmentsStoreService, mockAuthConnector, mockAuditService)

  "DELETE /enrolments-orchestrator/agents/:ARN?terminationDate=Option[Long] ?= None" should {

    "return 204, Request received and the attempt at deletion will be processed" in {

      val testHttpResponse = HttpResponse(204, responseString = Some("done"))

      authed

      auditRequest(testARN, testTerminationDate)

      (mockEnrolmentsStoreService.terminationByEnrolmentKey(_: String)(_: HeaderCarrier,_:ExecutionContext))
        .expects(testEnrolmentKey, *, *)
        .returning(Future.successful(testHttpResponse))

      auditResponse(testARN, testTerminationDate)

      val result = controller.es9Delete(testARN, Some(testTerminationDate))(fakeRequest)

      status(result) shouldBe NO_CONTENT
    }

    "return 401, Request received but but caller not authed" in {

      notAuthed

      val response = intercept[Upstream4xxResponse] {controller.es9Delete(testARN, None)(fakeRequest)}

      response.getMessage shouldBe "Missing bearer token"
    }

    "return 4xx, Request received but down stream services return 4xx" in {

      val testHttpResponse = HttpResponse(404, responseString = Some("not found"))

      authed

      auditRequest(testARN, testTerminationDate)

      (mockEnrolmentsStoreService.terminationByEnrolmentKey(_: String)(_: HeaderCarrier,_:ExecutionContext))
        .expects(*, *, *)
        .returning(Future.successful(testHttpResponse))

      auditResponse(testARN, testTerminationDate, eventTypeSuccess = false, testHttpResponse.status, Some(testHttpResponse.body))

      val result = controller.es9Delete(testARN, Some(testTerminationDate))(fakeRequest)

      status(result) shouldBe NOT_FOUND
      bodyOf(result).futureValue shouldBe "not found"
    }

    "return 4xx, Request received but down stream services return Upstream4xxResponse" in {

      authed

      auditRequest(testARN, testTerminationDate)

      (mockEnrolmentsStoreService.terminationByEnrolmentKey(_: String)(_: HeaderCarrier,_:ExecutionContext))
        .expects(*, *, *)
        .returning(Future.failed(Upstream4xxResponse("not found", 404, 404)))

      auditResponse(testARN, testTerminationDate, eventTypeSuccess = false, 404, Some("not found"))

      val result = controller.es9Delete(testARN, Some(testTerminationDate))(fakeRequest)

      status(result) shouldBe NOT_FOUND
      bodyOf(result).futureValue shouldBe "not found"
    }

    "return 500 if down stream services return 500" in {

      val testHttpResponse = HttpResponse(500, responseString = Some("error"))

      authed

      auditRequest(testARN, testTerminationDate)

      (mockEnrolmentsStoreService.terminationByEnrolmentKey(_: String)(_: HeaderCarrier,_:ExecutionContext))
        .expects(*, *, *)
        .returning(Future.successful(testHttpResponse))

      auditResponse(testARN, testTerminationDate, eventTypeSuccess = false, 500, Some("error"))

      val result = controller.es9Delete(testARN, Some(testTerminationDate))(fakeRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 500 if there are anything wrong with the service" in {

      authed

      auditRequest(testARN, testTerminationDate)

      (mockEnrolmentsStoreService.terminationByEnrolmentKey(_: String)(_: HeaderCarrier,_:ExecutionContext))
        .expects(*, *, *)
        .returning(Future.failed(new RuntimeException))

      auditResponse(testARN, testTerminationDate, eventTypeSuccess = false, 500, Some("Internal service error"))

      val result = controller.es9Delete(testARN, Some(testTerminationDate))(fakeRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

  }
}
