package uk.gov.hmrc.enrolmentsorchestrator.services
import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.enrolmentsorchestrator.models.{AgentDeleteRequest, AgentDeleteResponse}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.{ExecutionContext, Future}

class AuditService@Inject()(auditConnector: AuditConnector){
  val AUDIT_SOURCE = "enrolments-orchestrator"

  def audit(event: ExtendedDataEvent)(implicit hc: HeaderCarrier, ec:ExecutionContext): Future[AuditResult] = {
    auditConnector.sendExtendedEvent(event) recover {
      case t: Throwable ⇒
        Logger error(s"Failed sending audit message", t)
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