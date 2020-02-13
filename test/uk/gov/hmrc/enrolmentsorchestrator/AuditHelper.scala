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
import play.api.libs.json.Json
import uk.gov.hmrc.enrolmentsorchestrator.models.{AgentDeleteRequest, AgentDeleteResponse}
import uk.gov.hmrc.enrolmentsorchestrator.services.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.{ExecutionContext, Future}

trait AuditHelper extends MockFactory with ScalaFutures {

  val mockAuditService: AuditService = mock[AuditService]

  def auditRequest(arn: String, terminationDate: Long) = {
    val testAgentDeleteRequest = AgentDeleteRequest(arn, terminationDate)
    val extendedDataEventRequest = ExtendedDataEvent("enrolments-orchestrator", "AgentDeleteRequest", detail = Json.toJson(testAgentDeleteRequest))

    (mockAuditService.auditDeleteRequestEvent(_: AgentDeleteRequest))
      .expects(testAgentDeleteRequest)
      .returning(extendedDataEventRequest)

    (mockAuditService.audit(_: ExtendedDataEvent)(_: HeaderCarrier,_:ExecutionContext))
      .expects(extendedDataEventRequest, *, *)
      .returning(Future.successful(AuditResult.Success))
  }

  def auditResponse(arn: String, terminationDate: Long, eventTypeSuccess: Boolean = true, httpCode: Int = 204, message: Option[String] = None) = {
    val testAgentDeleteResponse = AgentDeleteResponse(arn, terminationDate, eventTypeSuccess, httpCode, message)
    val extendedDataEventResponse = ExtendedDataEvent("enrolments-orchestrator", "AgentDeleteResponse", detail = Json.toJson(testAgentDeleteResponse))

    (mockAuditService.auditAgentDeleteResponseEvent(_: AgentDeleteResponse))
      .expects(testAgentDeleteResponse)
      .returning(extendedDataEventResponse)

    (mockAuditService.audit(_: ExtendedDataEvent)(_: HeaderCarrier,_:ExecutionContext))
      .expects(extendedDataEventResponse, *, *)
      .returning(Future.successful(AuditResult.Success))
  }

}
