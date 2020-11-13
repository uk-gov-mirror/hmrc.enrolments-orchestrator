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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.enrolmentsorchestrator.config.AppConfig
import uk.gov.hmrc.enrolmentsorchestrator.models.PrivilegedApplicationClientLogin
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class AuthConnector @Inject() (httpClient: HttpClient, appConfig: AppConfig) {

  lazy val authBaseUrl: String = appConfig.authBaseUrl

  def createBearerToken(applicationName: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[HttpResponse] = {
    val requestBody = PrivilegedApplicationClientLogin(applicationName = Some(applicationName))
    httpClient.POST(s"$authBaseUrl/auth/sessions", requestBody)
  }

}
