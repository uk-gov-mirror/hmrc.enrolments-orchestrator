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
import uk.gov.hmrc.enrolmentsorchestrator.models.{AgentDeleteRequest, AgentDeleteResponse}
import uk.gov.hmrc.enrolmentsorchestrator.services.{AuditService, EnrolmentsStoreService}
import uk.gov.hmrc.http.Upstream4xxResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton()
class ES9DeleteController @Inject()(cc: ControllerComponents, enrolmentsStoreService: EnrolmentsStoreService, val auditConnector: AuditConnector, auditService: AuditService)
                                   (implicit val executionContext: ExecutionContext) extends BackendController(cc) {


  def es9Delete(arn: String, terminationDate: Option[Long]): Action[AnyContent] = Action.async { implicit request =>

    val tDate: Long = terminationDate.getOrElse(DateTime.now.getMillis)
    val enrolmentKey = s"HMRC-AS-AGENT~ARN~$arn"
    val agentDeleteRequest = AgentDeleteRequest(arn, tDate)

    auditService.audit(auditService.auditDeleteRequestEvent(agentDeleteRequest))

    enrolmentsStoreService.terminationByEnrolmentKey(enrolmentKey).map { res =>
      {
        if (res.status == 204) {
          val agentDeleteResponse = AgentDeleteResponse(arn, tDate, true, res.status, None)
          auditService.audit(auditService.auditAgentDeleteResponseEvent(agentDeleteResponse))
        }
        else {
          val agentDeleteResponse = AgentDeleteResponse(arn, tDate, false, res.status, Some(res.body))
          auditService.audit(auditService.auditAgentDeleteResponseEvent(agentDeleteResponse))
        }

        new Status(res.status)(res.body)
      }
    }.recover {
      case e: Upstream4xxResponse => {
        val agentDeleteResponse = AgentDeleteResponse(arn, tDate, false, e.upstreamResponseCode, Some(e.message))
        auditService.audit(auditService.auditAgentDeleteResponseEvent(agentDeleteResponse))
        new Status(e.upstreamResponseCode)(s"${e.message}")
      }
      case _ => {
        val agentDeleteResponse = AgentDeleteResponse(arn, tDate, false, 500, Some("Internal service error"))
        auditService.audit(auditService.auditAgentDeleteResponseEvent(agentDeleteResponse))
        new Status(500)("Internal service error")
      }
    }
  }
}
