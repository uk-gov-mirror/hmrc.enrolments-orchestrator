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
import uk.gov.hmrc.enrolmentsorchestrator.connectors.{AuthConnector, EnrolmentsStoreConnector}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class AuthService @Inject()(enrolmentsStoreConnector: EnrolmentsStoreConnector, authConnector: AuthConnector)  {

  def updatingAuthEnrolments(enrolmentKey: String)(esUpdate: => Future[HttpResponse])
                            (implicit hc: HeaderCarrier, ex: ExecutionContext): Future[HttpResponse] = {
    for {
      assignedBefore <- assignedUsers(enrolmentKey)
      response       <- esUpdate
      assignedAfter  <- if (statusOk(response)) assignedUsers(enrolmentKey)
                        else Future.successful(assignedBefore)
      changedUsers   =  {
        (assignedAfter union assignedBefore) diff (assignedBefore intersect assignedAfter)
      }
      _              <- if (statusOk(response)) updateAllAuthWithESEnrolments(changedUsers.toList) else Future.successful(())
    } yield {
      response
    }

  }

  private def assignedUsers(enrolmentKey: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Set[String]] = {
    enrolmentsStoreConnector.es0GetAssignedUsers(enrolmentKey).map(_.map(_.allUsers).getOrElse(Set.empty))
  }

  private def statusOk(response: HttpResponse): Boolean = response.status match {
    case 200 | 201 | 204  => true
    case _                => false
  }


  /** This function will update the the enrolments for potentially many credIds.
    * It does that both efficiently and without overloading the auth and enrolment store.
    * To do so it batches the credIds in groups.
    * The credIds in the same group will be updated in parallel while the groups are processed sequentially by using
    * a fold left
    * Errors in an individual update will be just logged and then ignored
    * */
  def updateAllAuthWithESEnrolments(credIdsToUpdate: List[String])(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Unit] = {
    Logger.debug(s"Updating enrolments for ${credIdsToUpdate.size} credIds")

    if (credIdsToUpdate.size > 1000)
      Logger.warn(s"Updating enrolments for $credIdsToUpdate")

    val maxParallelism = 8

    credIdsToUpdate.grouped(maxParallelism).foldLeft(Future.successful(())) {
      case (f, credIds) ⇒ f.flatMap { _ =>
        val updates: List[Future[Unit]] = credIds.map { credId =>
          updateAuthWithESEnrolments(credId).map(_ => ())
        }
        Future.sequence(updates).map(_  => ())
      }
    }

  }

  private def updateAuthWithESEnrolments(credId: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Unit] =
    for {
      enrolments  <- enrolmentsStoreConnector.es2GetEnrolments(credId)
      _           <- authConnector.updateEnrolments(enrolments, credId).recover {
                        case _: NotFoundException ⇒
                          Logger.error(s"Could not update credentials for $credId: authority record not found")
                        case e: Exception => throw e
                     }
    } yield ()

}
