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
import play.api.mvc._
import uk.gov.hmrc.enrolmentsorchestrator.config.AppConfig
import uk.gov.hmrc.enrolmentsorchestrator.connectors.AgentStatusChangeConnector
import uk.gov.hmrc.enrolmentsorchestrator.models.{AgentDeleteRequest, AgentDeleteResponse, BasicAuthentication}
import uk.gov.hmrc.enrolmentsorchestrator.services.{AuditService, AuthService, EnrolmentsStoreService}
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class AgentController @Inject()(appConfig: AppConfig,
                                auditService: AuditService,
                                authService: AuthService,
                                enrolmentsStoreService: EnrolmentsStoreService,
                                agentStatusChangeConnector: AgentStatusChangeConnector,
                                cc: ControllerComponents)
                               (implicit executionContext: ExecutionContext) extends BackendController(cc) {

  //more details about this end point: https://confluence.tools.tax.service.gov.uk/display/TM/SI+-+Enrolment+Orchestrator
  def deleteByARN(arn: String, terminationDate: Option[Long]): Action[AnyContent] = Action.async { implicit request =>

    val expectedAuth: BasicAuthentication = appConfig.expectedAuth
    val basicAuth: Option[BasicAuthentication] = authService.getBasicAuth(request.headers)

    val tDate: Long = terminationDate.getOrElse(DateTime.now.getMillis)
    val enrolmentKey = s"HMRC-AS-AGENT~AgentReferenceNumber~$arn"
    val agentDeleteRequest = AgentDeleteRequest(arn, tDate)

    auditService.audit(auditService.auditDeleteRequestEvent(agentDeleteRequest))

    if (basicAuth.contains(expectedAuth)) {
      callAgentStatusChangeToTerminate(arn, tDate) {
        continueES9(basicAuth, arn, tDate, enrolmentKey, request)
      }
    }
    else {
      auditService.audit(
        auditService.auditAgentDeleteResponseEvent(AgentDeleteResponse(arn, tDate, success = false, 401, Some("BasicAuthentication failed")))
      )
      Future.successful(new Status(401)(s"BasicAuthentication failed"))
    }

  }

  private def callAgentStatusChangeToTerminate(arn: String, tDate: Long)(continueES9: Future[Result])(implicit request: Request[_]): Future[Result] = {
    agentStatusChangeConnector.agentStatusChangeToTerminate(arn).flatMap { agentStatusChangeRes =>
      if (agentStatusChangeRes.status == 200) continueES9
      else {
        auditService.audit(
          auditService.auditAgentDeleteResponseEvent(
            AgentDeleteResponse(arn, tDate, success = false, agentStatusChangeRes.status, Some(agentStatusChangeRes.body))
          )
        )
        Future.successful(new Status(agentStatusChangeRes.status)(agentStatusChangeRes.body))
      }
    }.recover { case ex => handleRecover(ex, arn, tDate, request) }
  }

  private def createBearerToken(basicAuth: Option[BasicAuthentication])(implicit request: Request[_]): Future[Option[Authorization]] = {
    authService.createBearerToken(basicAuth)
  }

  private def continueES9(basicAuth: Option[BasicAuthentication], arn: String, tDate: Long, enrolmentKey: String, request: Request[_]): Future[Result] = {
    createBearerToken(basicAuth)(request).flatMap { bearerToken =>
      implicit val newHeaderCarrier: HeaderCarrier = HeaderCarrier(authorization = bearerToken)
      enrolmentsStoreService.terminationByEnrolmentKey(enrolmentKey).map { res =>
        if (res.status == 204) {
          auditService.audit(
            auditService.auditAgentDeleteResponseEvent(AgentDeleteResponse(arn, tDate, success = true, res.status, None))
          )(request, executionContext)
          new Status(200)(res.body)
        }
        else {
          auditService.audit(
            auditService.auditAgentDeleteResponseEvent(AgentDeleteResponse(arn, tDate, success = false, res.status, Some(res.body)))
          )(request, executionContext)
          new Status(res.status)(res.body)
        }
      }.recover { case ex => handleRecover(ex, arn, tDate, request) }
    }
  }

  private def handleRecover(exception: Throwable, arn: String, tDate: Long, request: Request[_]): Result = {
    exception match {
      case e: Upstream4xxResponse =>
        auditService.audit(
          auditService.auditAgentDeleteResponseEvent(AgentDeleteResponse(arn, tDate, success = false, e.upstreamResponseCode, Some(e.message)))
        )(request, executionContext)
        new Status(e.upstreamResponseCode)(s"${e.message}")
      case _ =>
        auditService.audit(
          auditService.auditAgentDeleteResponseEvent(AgentDeleteResponse(arn, tDate, success = false, 500, Some("Internal service error")))
        )(request, executionContext)
        new Status(500)("Internal service error")
    }
  }
}
