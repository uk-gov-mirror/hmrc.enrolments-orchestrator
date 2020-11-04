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
import play.api.libs.json.Json
import uk.gov.hmrc.enrolmentsorchestrator.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class EnrolmentsStoreConnector @Inject() (httpClient: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  lazy val enrolmentsStoreBaseUrl: String = appConfig.enrolmentsStoreBaseUrl

  //Query Groups who have an allocated Enrolment
  def es1GetPrincipalGroups(enrolmentKey: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val url = s"$enrolmentsStoreBaseUrl/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/groups?type=principal"
    httpClient.GET(url)
  }

  //see https://github.com/hmrc/enrolment-store-proxy#es8-allocate-enrolment-to-group
  def es8EnrolAndActivateEnrolmentOnGroup(groupId: String, credId: String, enrolmentKey: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = s"/enrolment-store-proxy/enrolment-store/groups/$groupId/enrolments/$enrolmentKey"
    httpClient.POSTString[HttpResponse](url, Json.stringify(Json.obj(
      "userId" -> credId,
      "type" -> "principal",
      "action" -> "enrolAndActivate"
    ))).map(_ => ())
  }

}
