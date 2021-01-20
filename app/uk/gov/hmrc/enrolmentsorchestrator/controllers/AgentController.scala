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

package uk.gov.hmrc.enrolmentsorchestrator.controllers

import org.joda.time.DateTime
import play.api.mvc._
import uk.gov.hmrc.enrolmentsorchestrator.config.AppConfig
import uk.gov.hmrc.enrolmentsorchestrator.connectors.AgentStatusChangeConnector
import uk.gov.hmrc.enrolmentsorchestrator.models.BasicAuthentication
import uk.gov.hmrc.enrolmentsorchestrator.services.{AuditService, AuthService, EnrolmentsStoreService}
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class AgentController @Inject() (
    appConfig:                  AppConfig,
    auditService:               AuditService,
    authService:                AuthService,
    enrolmentsStoreService:     EnrolmentsStoreService,
    agentStatusChangeConnector: AgentStatusChangeConnector,
    cc:                         ControllerComponents
)(implicit executionContext: ExecutionContext) extends BackendController(cc) {

  //more details about this end point: https://confluence.tools.tax.service.gov.uk/display/TM/SI+-+Enrolment+Orchestrator
  def deleteByARN(arn: String, terminationDate: Option[Long]): Action[AnyContent] = Action.async { implicit request =>
    val expectedAuth = appConfig.expectedAuth
    val basicAuth = authService.getBasicAuth(request.headers)

    val tDate = terminationDate.getOrElse(DateTime.now.getMillis)
    val enrolmentKey = s"HMRC-AS-AGENT~AgentReferenceNumber~$arn"

    auditService.auditDeleteRequest(arn, tDate)

    if (basicAuth.contains(expectedAuth)) {
      callAgentStatusChangeToTerminate(arn, tDate) {
        continueES9(basicAuth, arn, tDate, enrolmentKey, request)
      }
    } else {
      auditService.auditFailedAgentDeleteResponse(arn, tDate, 401, "BasicAuthentication failed")
      Future.successful(Unauthorized(s"BasicAuthentication failed"))
    }
  }

  private def callAgentStatusChangeToTerminate(arn: String, tDate: Long)(continueES9: => Future[Result])(implicit request: Request[_]): Future[Result] = {
    agentStatusChangeConnector.agentStatusChangeToTerminate(arn).flatMap { agentStatusChangeRes =>
      if (agentStatusChangeRes.status == 200) continueES9
      else {
        auditService.auditFailedAgentDeleteResponse(
          arn, tDate, agentStatusChangeRes.status, agentStatusChangeRes.body
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
          auditService.auditSuccessfulAgentDeleteResponse(arn, tDate, res.status)(request)
          Ok(res.body)
        } else {
          auditService.auditFailedAgentDeleteResponse(arn, tDate, res.status, res.body)(request)
          new Status(res.status)(res.body)
        }
      }.recover { case ex => handleRecover(ex, arn, tDate, request) }
    }
  }

  private def handleRecover(exception: Throwable, arn: String, tDate: Long, request: Request[_]): Result = {
    exception match {
      case e: Upstream4xxResponse =>
        auditService.auditFailedAgentDeleteResponse(arn, tDate, e.upstreamResponseCode, e.message)(request)
        new Status(e.upstreamResponseCode)(s"${e.message}")
      case _ =>
        auditService.auditFailedAgentDeleteResponse(arn, tDate, 500, "Internal service error")(request)
        InternalServerError("Internal service error")
    }
  }
}
