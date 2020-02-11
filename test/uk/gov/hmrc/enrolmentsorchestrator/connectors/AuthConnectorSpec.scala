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
import play.api.libs.json.{JsValue, Writes}
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.enrolmentsorchestrator.UnitSpec
import uk.gov.hmrc.enrolmentsorchestrator.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthConnectorSpec extends UnitSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite {

  val mockHttpClient: HttpClient = mock[HttpClient]
  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val connector = new AuthConnector(mockHttpClient, appConfig)

  "AuthConnector" should {

    "connect Auth to update cred's enrolments and return the update response" in {

      val testHttpResponse = HttpResponse(200)
      val credId = "credId"

      (mockHttpClient.POST[JsValue, HttpResponse](_: String, _: JsValue, _: Seq[(String, String)])(_: Writes[JsValue], _: HttpReads[HttpResponse], _: HeaderCarrier, _:ExecutionContext))
        .expects(*, *, *, *, *, *, *)
        .returning(Future.successful(testHttpResponse))

      connector.updateEnrolments(Enrolments(Set[Enrolment]()), credId).futureValue shouldBe testHttpResponse

    }

  }

}
