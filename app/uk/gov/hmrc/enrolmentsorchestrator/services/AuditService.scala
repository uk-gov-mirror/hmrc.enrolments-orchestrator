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
import play.api.mvc.Request
import uk.gov.hmrc.enrolmentsorchestrator.models.{AgentDeleteRequest, AgentDeleteResponse}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class AuditService @Inject() (auditConnector: AuditConnector) {
  val AUDIT_SOURCE = "enrolments-orchestrator"

  def audit(event: ExtendedDataEvent)(implicit request: Request[_], ec: ExecutionContext): Future[AuditResult] = {
    auditConnector.sendExtendedEvent(event) recover {
      case t: Throwable ⇒
        Logger error (s"Failed sending audit message", t)
        AuditResult.Failure(s"Failed sending audit message", Some(t))
    }
  }

  def auditDeleteRequestEvent(agentDeleteRequest: AgentDeleteRequest): ExtendedDataEvent = {
    val auditType: String = "AgentDeleteRequest"
    ExtendedDataEvent(
      AUDIT_SOURCE,
      auditType,
      detail = Json toJson agentDeleteRequest
    )
  }

  def auditAgentDeleteResponseEvent(agentDeleteResponse: AgentDeleteResponse): ExtendedDataEvent = {
    val auditType: String = "AgentDeleteResponse"
    ExtendedDataEvent(
      AUDIT_SOURCE,
      auditType,
      detail = Json toJson agentDeleteResponse
    )
  }

}
