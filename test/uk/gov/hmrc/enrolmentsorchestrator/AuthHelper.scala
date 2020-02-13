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

package uk.gov.hmrc.enrolmentsorchestrator

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Writes
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, Upstream4xxResponse}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

trait AuthHelper extends MockFactory with ScalaFutures with GuiceOneAppPerSuite {

  val httpClient: HttpClient = mock[HttpClient]
  val config: ServicesConfig = app.injector.instanceOf[ServicesConfig]
  val mockAuthConnector: AuthConnector = new DefaultAuthConnector(httpClient, config)

  def authed = {
    (httpClient.POST(_: String, _: Any, _: Seq[(String, String)])(_: Writes[Any], _: HttpReads[Any], _: HeaderCarrier,_:ExecutionContext))
      .expects(*, *, *, *, *, *, *)
      .returning(Future.successful(HttpResponse(200)))
  }

  def notAuthed = {
    (httpClient.POST(_: String, _: Any, _: Seq[(String, String)])(_: Writes[Any], _: HttpReads[Any], _: HeaderCarrier,_:ExecutionContext))
      .expects(*, *, *, *, *, *, *) throws Upstream4xxResponse("Missing bearer token", 401, 401, Map("WWW-Authenticate" -> Seq("MDTP detail=\"MissingBearerToken\"")))

  }
}
