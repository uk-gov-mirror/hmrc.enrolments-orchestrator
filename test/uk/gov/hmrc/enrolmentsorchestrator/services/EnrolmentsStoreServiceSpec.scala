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
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.enrolmentsorchestrator.connectors.EnrolmentsStoreConnector
import uk.gov.hmrc.enrolmentsorchestrator.models.PrincipalGroupIds
import uk.gov.hmrc.enrolmentsorchestrator.{LogCapturing, UnitSpec}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentsStoreServiceSpec extends UnitSpec with LogCapturing with MockFactory with ScalaFutures {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val mockEnrolmentsStoreConnector: EnrolmentsStoreConnector = mock[EnrolmentsStoreConnector]
  val mockAuthService: AuthService = mock[AuthService]

  val enrolmentsStoreService = new EnrolmentsStoreService(mockEnrolmentsStoreConnector, mockAuthService)

  val enrolmentKey = "enrolmentKey"
  val groupId = "groupId"

  "EnrolmentsStoreService" should {

    "return 204 HttpResponse if es1 returns 200 and AuthConnector returns 204" in {

      withCaptureOfLoggingFrom(Logger) { logEvents =>

        val enrolmentsStoreHttpResponseBody = Json.toJson(PrincipalGroupIds(List(groupId)))

        val enrolmentsStoreHttpResponse = HttpResponse(200, Some(enrolmentsStoreHttpResponseBody))
        val authHttpResponse = HttpResponse(204)

        (mockEnrolmentsStoreConnector.es1GetPrincipalGroups(_: String)(_: HeaderCarrier,_:ExecutionContext))
          .expects(enrolmentKey, *, *)
          .returning(Future.successful(enrolmentsStoreHttpResponse))

        (mockAuthService.updatingAuthEnrolments(_: String)(_: Future[HttpResponse])(_: HeaderCarrier,_:ExecutionContext))
          .expects(enrolmentKey, *, *, *)
          .returning(Future.successful(authHttpResponse))

        await(enrolmentsStoreService.terminationByEnrolmentKey(enrolmentKey)) shouldBe authHttpResponse

        logEvents.length shouldBe 0

      }

    }

    "return HttpResponse from es1 if es1 returns not ok(200), also log the response" in {
      withCaptureOfLoggingFrom(Logger) { logEvents =>

        val testHttpResponse = HttpResponse(204)
        val enrolmentKey = "enrolmentKey"

        (mockEnrolmentsStoreConnector.es1GetPrincipalGroups(_: String)(_: HeaderCarrier,_:ExecutionContext))
          .expects(enrolmentKey, *, *)
          .returning(Future.successful(testHttpResponse))

        await(enrolmentsStoreService.terminationByEnrolmentKey(enrolmentKey)) shouldBe testHttpResponse

        logEvents.length shouldBe 1
        logEvents.collectFirst { case logEvent =>
          logEvent.getMessage shouldBe "For enrolmentKey: enrolmentKey 200 was not returned by Enrolments-Store es1, " +
            "ie no groupId found there are no allocated groups (the enrolment itself may or may not actually exist) " +
            "or there is nothing to return, the response is 204 with body null"
        }

      }
    }

    "return HttpResponse from AuthConnector if EnrolmentsStoreConnector returns 200 but AuthConnector not returns 204, also log the response" in {
      withCaptureOfLoggingFrom(Logger) { logEvents =>

        val enrolmentsStoreHttpResponseBody = Json.toJson(PrincipalGroupIds(List(groupId)))

        val enrolmentsStoreHttpResponse = HttpResponse(200, Some(enrolmentsStoreHttpResponseBody))
        val authHttpResponse = HttpResponse(400)

        (mockEnrolmentsStoreConnector.es1GetPrincipalGroups(_: String)(_: HeaderCarrier,_:ExecutionContext))
          .expects(enrolmentKey, *, *)
          .returning(Future.successful(enrolmentsStoreHttpResponse))

        (mockAuthService.updatingAuthEnrolments(_: String)(_: Future[HttpResponse])(_: HeaderCarrier,_:ExecutionContext))
          .expects(enrolmentKey, *, *, *)
          .returning(Future.successful(authHttpResponse))

        await(enrolmentsStoreService.terminationByEnrolmentKey(enrolmentKey)) shouldBe authHttpResponse

        logEvents.length shouldBe 1
        logEvents.collectFirst { case logEvent =>
          logEvent.getMessage shouldBe s"For enrolmentKey: $enrolmentKey and groupId: $groupId 204 was not returned by Tax-Enrolments, the response is 400 with body null"
        }

      }
    }

  }

}
