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

import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.enrolmentsorchestrator.models.{AgentDeleteRequest, AgentDeleteResponse}
import uk.gov.hmrc.enrolmentsorchestrator.services.AuditService
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

trait AuditHelper extends MockitoSugar {

  val mockAuditService: AuditService = mock[AuditService]

  val testAgentDeleteRequest: AgentDeleteRequest

  def auditDeleteRequestEvent(agentDeleteRequest: AgentDeleteRequest): ExtendedDataEvent = {
    ExtendedDataEvent(
      "enrolments-orchestrator",
      "AgentDeleteRequest",
      detail = Json toJson agentDeleteRequest
    )
  }

  def auditAgentDeleteResponseEvent(agentDeleteResponse: AgentDeleteResponse): ExtendedDataEvent = {
    ExtendedDataEvent(
      "enrolments-orchestrator",
      "AgentDeleteResponse",
      detail = Json toJson agentDeleteResponse
    )
  }

  def auditMockSetup(testAgentDeleteResponse: AgentDeleteResponse, extendedDataEventRequest: ExtendedDataEvent, extendedDataEventResponse: ExtendedDataEvent): OngoingStubbing[ExtendedDataEvent] = {
    when(mockAuditService.auditDeleteRequestEvent(eqTo(testAgentDeleteRequest))).thenReturn(extendedDataEventRequest)
    when(mockAuditService.auditAgentDeleteResponseEvent(eqTo(testAgentDeleteResponse))).thenReturn(extendedDataEventResponse)
  }

  def verifyAuditEvents(testAgentDeleteResponse: AgentDeleteResponse): Unit = {
    verify(mockAuditService, times(1)).auditDeleteRequestEvent(eqTo(testAgentDeleteRequest))
    verify(mockAuditService, times(1)).auditAgentDeleteResponseEvent(eqTo(testAgentDeleteResponse))
    reset(mockAuditService)
  }

}
