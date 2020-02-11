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

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.enrolmentsorchestrator.connectors.EnrolmentsStoreConnector
import uk.gov.hmrc.enrolmentsorchestrator.models.PrincipalGroupIds
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, Upstream4xxResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class EnrolmentsStoreService @Inject()(enrolmentsStoreConnector: EnrolmentsStoreConnector, authService: AuthService) {

  def terminationByEnrolmentKey(enrolmentKey: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[HttpResponse] = {

    enrolmentsStoreConnector.es1GetPrincipalGroups(enrolmentKey).flatMap { response =>

      response.status match {
        case 200 =>
          val groupId = Json.parse(response.body).validate[PrincipalGroupIds].get.principalGroupIds.head

          authService.updatingAuthEnrolments(enrolmentKey) {
            enrolmentsStoreConnector.es9DeEnrol(groupId, enrolmentKey)
          }.map { response =>

            response.status match {
              case 204 =>
              case _ =>
                Logger.warn(s"For enrolmentKey: $enrolmentKey and groupId: $groupId 204 was not returned by Tax-Enrolments, " +
                  s"the response is ${response.status} with body ${response.body}")
            }
            response
          }.recover {
            case e: Upstream4xxResponse =>
              Logger.warn(s"For enrolmentKey: $enrolmentKey and groupId: $groupId 204 was not returned by Tax-Enrolments," +
                s" the response is ${e.upstreamResponseCode} with body ${e.message}")
              throw e
            case ex: Exception => throw ex
          }
        case _ =>
          Logger.warn(s"For enrolmentKey: $enrolmentKey 200 was not returned by Enrolments-Store es1," +
            s" ie no groupId found there are no allocated groups (the enrolment itself may or may not actually exist) " +
            s"or there is nothing to return, the response is ${response.status} with body ${response.body}")
          Future.successful(response)
      }
    }
  }
}
