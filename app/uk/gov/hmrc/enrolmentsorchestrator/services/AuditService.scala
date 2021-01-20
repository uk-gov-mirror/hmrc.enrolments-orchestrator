/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Request
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import uk.gov.hmrc.play.audit.AuditExtensions._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class AuditService @Inject() (auditConnector: AuditConnector)(implicit ec: ExecutionContext) extends Logging with BackendHeaderCarrierProvider {
  def auditDeleteRequest(agentReferenceNumber: String, terminationDate: Long)(implicit request: Request[_]): Unit = {
    val event = ExtendedDataEvent(
      "enrolments-orchestrator",
      "AgentDeleteRequest",
      detail = Json.obj(
        "agentReferenceNumber" -> agentReferenceNumber,
        "terminationDate" -> terminationDate
      ),
      tags   = hc.toAuditTags("HMRC Gateway - Enrolments Orchestrator - Agent Delete Request", request.path)
    )

    audit(event)
  }

  def auditSuccessfulAgentDeleteResponse(agentReferenceNumber: String, terminationDate: Long, statusCode: Int)(implicit request: Request[_]): Unit = {
    val event = ExtendedDataEvent(
      "enrolments-orchestrator",
      "AgentDeleteResponse",
      detail = Json.obj(
        "agentReferenceNumber" -> agentReferenceNumber,
        "terminationDate" -> terminationDate,
        "statusCode" -> statusCode,
        "success" -> true
      ),
      tags   = hc.toAuditTags("HMRC Gateway - Enrolments Orchestrator - Agent Delete Response", request.path)
    )

    audit(event)
  }

  def auditFailedAgentDeleteResponse(agentReferenceNumber: String, terminationDate: Long, statusCode: Int, failureReason: String)(implicit request: Request[_]): Unit = {
    val event = ExtendedDataEvent(
      "enrolments-orchestrator",
      "AgentDeleteResponse",
      detail = Json.obj(
        "agentReferenceNumber" -> agentReferenceNumber,
        "terminationDate" -> terminationDate,
        "statusCode" -> statusCode,
        "failureReason" -> failureReason,
        "success" -> false
      ),
      tags   = hc.toAuditTags("HMRC Gateway - Enrolments Orchestrator - Agent Delete Response", request.path)
    )

    audit(event)
  }

  private def audit(event: ExtendedDataEvent): Future[Unit] = {
    auditConnector.sendExtendedEvent(event).map(_ => ()).recover {
      case t =>
        logger.error(s"Failed sending audit message", t)
    }
  }

}
