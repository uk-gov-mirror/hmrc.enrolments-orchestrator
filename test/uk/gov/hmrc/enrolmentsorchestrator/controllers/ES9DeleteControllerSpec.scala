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
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.enrolmentsorchestrator.UnitSpec
import uk.gov.hmrc.enrolmentsorchestrator.services.EnrolmentsStoreService
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ES9DeleteControllerSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  val mockEnrolmentsStoreService: EnrolmentsStoreService = mock[EnrolmentsStoreService]

  private val fakeRequest = FakeRequest("DELETE", "/")

  private val controller = new ES9DeleteController(Helpers.stubControllerComponents(), mockEnrolmentsStoreService)

  val testARN = "AARN123"
  val testTerminationDate: Long = DateTime.now.toInstant.getMillis

  "DELETE /enrolments-orchestrator/agents/:ARN?terminationDate=Option[Long] ?= None" should {

    "return 204, Request received and the attempt at deletion will be processed" in {

      val testHttpResponse = HttpResponse(204, responseString = Some("done"))

      when(mockEnrolmentsStoreService.terminationByEnrolmentKey(any())(any(), any())).thenReturn(Future.successful(testHttpResponse))

      val result = controller.es9Delete(testARN, Some(testTerminationDate))(fakeRequest)

      status(result) shouldBe NO_CONTENT
    }

    "return 500 if down stream services return 500" in {

      val testHttpResponse = HttpResponse(500, responseString = Some("error"))

      when(mockEnrolmentsStoreService.terminationByEnrolmentKey(any())(any(), any())).thenReturn(Future.successful(testHttpResponse))

      val result = controller.es9Delete(testARN, Some(testTerminationDate))(fakeRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 500 if there are anything wrong with the service" in {

      when(mockEnrolmentsStoreService.terminationByEnrolmentKey(any())(any(), any())).thenReturn(Future.failed(new RuntimeException))

      val result = controller.es9Delete(testARN, Some(testTerminationDate))(fakeRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

  }
}
