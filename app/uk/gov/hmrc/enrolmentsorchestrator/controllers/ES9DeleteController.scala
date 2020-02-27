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
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request}
import uk.gov.hmrc.enrolmentsorchestrator.config.AppConfig
import uk.gov.hmrc.enrolmentsorchestrator.models.{AgentDeleteRequest, AgentDeleteResponse, BasicAuthentication}
import uk.gov.hmrc.enrolmentsorchestrator.services.{AuditService, AuthService, EnrolmentsStoreService}
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class ES9DeleteController @Inject()(appConfig: AppConfig,
                                    auditService: AuditService,
                                    authService: AuthService,
                                    cc: ControllerComponents,
                                    enrolmentsStoreService: EnrolmentsStoreService)
                                   (implicit executionContext: ExecutionContext) extends BackendController(cc) {


  def es9Delete(arn: String, terminationDate: Option[Long]): Action[AnyContent] = Action.async { request =>

    val expectedAuth: BasicAuthentication = appConfig.expectedAuth
    val basicAuth: Option[BasicAuthentication] = authService.getBasicAuth(request.headers)

    val bearerToken = setBearerToken(basicAuth)(request)
    implicit val newHeaderCarrier: HeaderCarrier = HeaderCarrier(authorization = bearerToken)

    val tDate: Long = terminationDate.getOrElse(DateTime.now.getMillis)
    val enrolmentKey = s"HMRC-AS-AGENT~AgentReferenceNumber~$arn"
    val agentDeleteRequest = AgentDeleteRequest(arn, tDate)

    auditService.audit(auditService.auditDeleteRequestEvent(agentDeleteRequest))

    if (basicAuth.contains(expectedAuth)) {
      enrolmentsStoreService.terminationByEnrolmentKey(enrolmentKey).map { res =>
        if (res.status == 204) auditService.audit(auditService.auditAgentDeleteResponseEvent(AgentDeleteResponse(arn, tDate, success = true, res.status, None)))
        else auditService.audit(auditService.auditAgentDeleteResponseEvent(AgentDeleteResponse(arn, tDate,  success = false, res.status, Some(res.body))))
        new Status(res.status)(res.body)
      }.recover {
        case e: Upstream4xxResponse =>
          auditService.audit(auditService.auditAgentDeleteResponseEvent(AgentDeleteResponse(arn, tDate, success = false, e.upstreamResponseCode, Some(e.message))))
          new Status(e.upstreamResponseCode)(s"${e.message}")
        case _ =>
          auditService.audit(auditService.auditAgentDeleteResponseEvent(AgentDeleteResponse(arn, tDate, success = false, 500, Some("Internal service error"))))
          new Status(500)("Internal service error")
      }
    } else {
      auditService.audit(auditService.auditAgentDeleteResponseEvent(AgentDeleteResponse(arn, tDate, success = false, 401, Some("BasicAuthentication failed"))))
      Future.successful( new Status(401)(s"BasicAuthentication failed") )
    }

  }

  private def setBearerToken(basicAuth: Option[BasicAuthentication])(implicit request: Request[_]): Option[Authorization] = {
    authService.createBearerToken(basicAuth)
  }

}
