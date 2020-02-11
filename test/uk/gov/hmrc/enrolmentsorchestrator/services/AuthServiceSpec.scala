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
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.enrolmentsorchestrator.connectors.{AuthConnector, EnrolmentsStoreConnector}
import uk.gov.hmrc.enrolmentsorchestrator.models.AssignedUsers
import uk.gov.hmrc.enrolmentsorchestrator.{LogCapturing, UnitSpec}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthServiceSpec extends UnitSpec with LogCapturing with MockFactory with ScalaFutures {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val mockEnrolmentsStoreConnector: EnrolmentsStoreConnector = mock[EnrolmentsStoreConnector]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  val authService = new AuthService(mockEnrolmentsStoreConnector, mockAuthConnector)

  val enrolmentKey = "enrolmentKey"

  "AuthService" should {

    "updating enrolment information for related users, and return ES9(De-allocate an Enrolment from a Group) response anyway" when {

      "there are nothing to update" in {
        val es0Response = Some(AssignedUsers(None, None))
        val es9Response = HttpResponse(204)

        (mockEnrolmentsStoreConnector.es0GetAssignedUsers(_: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(enrolmentKey, *, *)
          .returning(Future.successful(es0Response)).repeat(2)

        await(authService.updatingAuthEnrolments(enrolmentKey)(Future.successful(es9Response))) shouldBe es9Response
      }

      "update auth if es9 returns 204" in {
        val es0ResponseAssignedBefore = Some(AssignedUsers(Some(Set("1", "2", "3")), Some(Set("a", "b", "c"))))
        val es0ResponseAssignedAfter = Some(AssignedUsers(Some(Set("2", "3", "4")), Some(Set("b", "c", "d"))))
        val es2Response = Enrolments(Set[Enrolment]())
        val es9Response = HttpResponse(204)
        val authResponse = HttpResponse(204)

        (mockEnrolmentsStoreConnector.es0GetAssignedUsers(_: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(enrolmentKey, *, *)
          .returning(Future.successful(es0ResponseAssignedBefore))

        (mockEnrolmentsStoreConnector.es0GetAssignedUsers(_: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(enrolmentKey, *, *)
          .returning(Future.successful(es0ResponseAssignedAfter))

        (mockEnrolmentsStoreConnector.es2GetEnrolments(_: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returning(Future.successful(es2Response)).repeat(4)

        (mockAuthConnector.updateEnrolments(_: Enrolments, _: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returning(Future.successful(authResponse)).repeat(4)

        await(authService.updatingAuthEnrolments(enrolmentKey)(Future.successful(es9Response))) shouldBe es9Response
      }

      "don't update auth if es9 fails" in {
        val es0ResponseAssignedBefore = Some(AssignedUsers(Some(Set("1", "2", "3")), Some(Set("a", "b", "c"))))
        val es9Response = HttpResponse(401)

        (mockEnrolmentsStoreConnector.es0GetAssignedUsers(_: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(enrolmentKey, *, *)
          .returning(Future.successful(es0ResponseAssignedBefore))

        (mockAuthConnector.updateEnrolments(_: Enrolments, _: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *).never()

        await(authService.updatingAuthEnrolments(enrolmentKey)(Future.successful(es9Response))) shouldBe es9Response
      }

    }


    "log event" when {
      "number of credIds need to be updated is greater than 1000" in {
        withCaptureOfLoggingFrom(Logger) { logEvents =>
          val testCredIds = List.fill(1001)("")
          authService.updateAllAuthWithESEnrolments(testCredIds)
          logEvents.length shouldBe 2
          logEvents.collectFirst { case logEvent =>
            logEvent.getMessage shouldBe s"Updating enrolments for 1001 credIds"
          }
        }
      }
    }

  }

}
