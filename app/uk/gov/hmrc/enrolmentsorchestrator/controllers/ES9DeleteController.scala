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
import uk.gov.hmrc.enrolmentsorchestrator.models.{AgentDeleteRequest, AgentDeleteResponse}
import uk.gov.hmrc.enrolmentsorchestrator.services.{AuditService, EnrolmentsStoreService}
import uk.gov.hmrc.http.Upstream4xxResponse
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton()
class ES9DeleteController @Inject()(cc: ControllerComponents, enrolmentsStoreService: EnrolmentsStoreService, val authConnector: AuthConnector, auditService: AuditService)
                                   (implicit executionContext: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  def es9Delete(arn: String, terminationDate: Option[Long]): Action[AnyContent] = Action.async { implicit request =>

    authorised() {

      val tDate: Long = terminationDate.getOrElse(DateTime.now.getMillis)
      val AGENT_ENROLMENT_KEY = s"HMRC-AS-AGENT~AgentReferenceNumber~$arn"
      val agentDeleteRequest = AgentDeleteRequest(arn, tDate)

      auditService.audit(auditService.auditDeleteRequestEvent(agentDeleteRequest))

      enrolmentsStoreService.terminationByEnrolmentKey(AGENT_ENROLMENT_KEY).map { res =>
        if (res.status == 204) auditService.audit(auditService.auditAgentDeleteResponseEvent(AgentDeleteResponse(arn, tDate, success = true, res.status, None)))
        else auditService.audit(auditService.auditAgentDeleteResponseEvent(AgentDeleteResponse(arn, tDate, success = false, res.status, Some(res.body))))
        new Status(res.status)(res.body)
      }.recover {
        case e: Upstream4xxResponse =>
          auditService.audit(auditService.auditAgentDeleteResponseEvent(AgentDeleteResponse(arn, tDate, success = false, e.upstreamResponseCode, Some(e.message))))
          new Status(e.upstreamResponseCode)(s"${e.message}")
        case _ =>
          auditService.audit(auditService.auditAgentDeleteResponseEvent(AgentDeleteResponse(arn, tDate, success = false, 500, Some("Internal service error"))))
          new Status(500)("Internal service error")
      }
    }
  }
}
