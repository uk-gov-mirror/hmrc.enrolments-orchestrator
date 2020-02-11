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

import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.enrolmentsorchestrator.services.EnrolmentsStoreService
import uk.gov.hmrc.http.Upstream4xxResponse
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton()
class ES9DeleteController @Inject()(cc: ControllerComponents, enrolmentsStoreService: EnrolmentsStoreService, val authConnector: AuthConnector)
                                   (implicit executionContext: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  def es9Delete(arn: String, terminationDate: Option[Long]): Action[AnyContent] = Action.async { implicit request =>

    authorised() {
      //todo: the tDate is for Audit events to use
      val tDate: Long = terminationDate.getOrElse(DateTime.now.getMillis)
      val enrolmentKey = s"HMRC-AS-AGENT~ARN~$arn"

      enrolmentsStoreService.terminationByEnrolmentKey(enrolmentKey).map { res =>
        new Status(res.status)(res.body)
      }.recover {
        case e: Upstream4xxResponse => new Status(e.upstreamResponseCode)(s"${e.message}")
        case _ => new Status(500)("Internal service error")
      }
    }

  }

}
