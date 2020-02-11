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
import play.api.libs.json._
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.enrolmentsorchestrator.config.AppConfig
import uk.gov.hmrc.enrolmentsorchestrator.models.AssignedUsers
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class EnrolmentsStoreConnector @Inject()(httpClient: HttpClient, appConfig: AppConfig) {

  lazy val enrolmentsStoreBaseUrl: String = appConfig.enrolmentsStoreBaseUrl

  //ES1 Query Groups who have an allocated Enrolment
  def es1GetPrincipalGroups(enrolmentKey: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[HttpResponse] = {
    val url = s"$enrolmentsStoreBaseUrl/enrolment-store/enrolments/$enrolmentKey/groups?type=principal"
    httpClient.GET(url)
  }

  //ES0 Query Users who have an assigned Enrolment
  def es0GetAssignedUsers(enrolmentKey: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Option[AssignedUsers]] = {
    val url = s"$enrolmentsStoreBaseUrl/enrolment-store/enrolments/$enrolmentKey/users"
    httpClient.GET[Option[AssignedUsers]](url)
  }

  //ES2 Query Enrolments assigned to a User/credId
  def es2GetEnrolments(credId: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Enrolments] = {

    implicit val enrolmentsReads: Reads[Enrolments] = Json.reads[Enrolments]

    val url = s"$enrolmentsStoreBaseUrl/enrolment-store/users/$credId/enrolments"
    val response: Future[HttpResponse] = httpClient.GET(url)

    response.map { r =>
      if (r.status == 204) {
        Enrolments(Set.empty)
      } else if (r.status == 200) {
        r.json.validate[Enrolments] match {
          case enr: JsSuccess[Enrolments] => enr.value
          case _ => Enrolments(Set.empty)
        }
      } else {
        Enrolments(Set.empty)
      }
    }
  }

  //ES9 De-allocate an Enrolment from a Group
  def es9DeEnrol(groupId: String, enrolmentKey: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[HttpResponse] = {
    val url = s"$enrolmentsStoreBaseUrl/enrolment-store/groups/$groupId/enrolments/$enrolmentKey"
    httpClient.DELETE(url)
  }

}
