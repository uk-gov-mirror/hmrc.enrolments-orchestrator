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

package uk.gov.hmrc.enrolmentsorchestrator.connectors

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json, Reads, Writes}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.enrolmentsorchestrator.UnitSpec
import uk.gov.hmrc.enrolmentsorchestrator.config.AppConfig
import uk.gov.hmrc.enrolmentsorchestrator.models.AssignedUsers
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentsStoreConnectorSpec extends UnitSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite {

  val mockHttpClient: HttpClient = mock[HttpClient]
  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val connector = new EnrolmentsStoreConnector(mockHttpClient, appConfig)

  val enrolmentKey = "enrolmentKey"
  val credId = "credId"
  val groupId = "groupId"

  "EnrolmentsStoreConnector" should {

    "Connect to ES0: Query Users who have an assigned Enrolment" in {

      val testResponse = Some(AssignedUsers(None, None))

      (mockHttpClient.GET[Option[AssignedUsers]](_: String)(_: HttpReads[Option[AssignedUsers]], _: HeaderCarrier,_:ExecutionContext))
        .expects(*, *, *, *)
        .returning(Future.successful(testResponse))

      connector.es0GetAssignedUsers(enrolmentKey).futureValue shouldBe testResponse

    }


    "Connect to ES1 get HttpResponse: Query Groups who have an allocated Enrolment" in {

      val testHttpResponse = HttpResponse(200)

      (mockHttpClient.GET[HttpResponse](_: String)(_: HttpReads[HttpResponse], _: HeaderCarrier,_:ExecutionContext))
        .expects(*, *, *, *)
        .returning(Future.successful(testHttpResponse))

      connector.es1GetPrincipalGroups(enrolmentKey).futureValue shouldBe testHttpResponse

    }


    "Connect to ES2: Query Enrolments assigned to a User/credId" when {

      "if ES2 returns 204, enrolments for the credId in empty " in {
        val testHttpResponse = HttpResponse(204)
        val testResponse = Enrolments(Set[Enrolment]())

        (mockHttpClient.GET(_: String)(_: HttpReads[HttpResponse], _: HeaderCarrier,_:ExecutionContext))
          .expects(*, *, *, *)
          .returning(Future.successful(testHttpResponse))

        connector.es2GetEnrolments(credId).futureValue shouldBe testResponse
      }

      "if ES2 returns 200 and with a valid enrolments body, return enrolments for the credId" in {

        val testEnrolments = Enrolments(Set[Enrolment](Enrolment("testKey", Seq[EnrolmentIdentifier](EnrolmentIdentifier("name", "value")), "activated", None)))
        val testEnrolmentsInJson: JsValue = Json.parse("""{"enrolments":[{"key":"testKey","identifiers":[{"key":"name","value":"value"}],"state":"activated"}]}""")
        val testHttpResponse = HttpResponse(200, Some(testEnrolmentsInJson))
        val testResponse = testEnrolments

        (mockHttpClient.GET(_: String)(_: HttpReads[HttpResponse], _: HeaderCarrier,_:ExecutionContext))
          .expects(*, *, *, *)
          .returning(Future.successful(testHttpResponse))

        connector.es2GetEnrolments(credId).futureValue shouldBe testResponse
      }

      "if ES2 returns 200 but with an invalid enrolments body, return empty enrolments for the credId" in {

        val testEnrolments = Enrolments(Set[Enrolment]())
        val testEnrolmentsInJson: JsValue = Json.parse("""{"enrolments": "any"}""")
        val testHttpResponse = HttpResponse(200, Some(testEnrolmentsInJson))
        val testResponse = testEnrolments

        (mockHttpClient.GET(_: String)(_: HttpReads[HttpResponse], _: HeaderCarrier,_:ExecutionContext))
          .expects(*, *, *, *)
          .returning(Future.successful(testHttpResponse))

        connector.es2GetEnrolments(credId).futureValue shouldBe testResponse
      }

      "if ES2 returns non 200 or 204, return empty enrolments for the credId" in {

        val testEnrolments = Enrolments(Set[Enrolment]())
        val testHttpResponse = HttpResponse(400)
        val testResponse = testEnrolments

        (mockHttpClient.GET(_: String)(_: HttpReads[HttpResponse], _: HeaderCarrier,_:ExecutionContext))
          .expects(*, *, *, *)
          .returning(Future.successful(testHttpResponse))

        connector.es2GetEnrolments(credId).futureValue shouldBe testResponse
      }

    }


    "Connect to ES9 get HttpResponse: De-allocate an Enrolment from a Group" in {

      val testHttpResponse = HttpResponse(200)

      (mockHttpClient.DELETE[HttpResponse](_: String, _: Seq[(String, String)])(_: HttpReads[HttpResponse], _: HeaderCarrier,_:ExecutionContext))
        .expects(*, *, *, *, *)
        .returning(Future.successful(testHttpResponse))

      connector.es9DeEnrol(groupId, enrolmentKey).futureValue shouldBe testHttpResponse

    }

  }

}
