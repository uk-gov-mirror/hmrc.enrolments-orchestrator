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

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames._
import play.api.mvc.Headers
import uk.gov.hmrc.enrolmentsorchestrator.connectors.AuthConnector
import uk.gov.hmrc.enrolmentsorchestrator.models.BasicAuthentication
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization

import scala.concurrent._
import scala.util.matching.Regex

@Singleton()
class AuthService @Inject() (authConnector: AuthConnector) {

  private def decodeFromBase64(encodedString: String): String = try {
    new String(Base64.getDecoder.decode(encodedString), UTF_8)
  } catch { case _: Throwable => "" }

  def getBasicAuth(headers: Headers): Option[BasicAuthentication] = {

    val basicAuthHeader: Regex = "Basic (.+)".r
    val decodedAuth: Regex = "(.+):(.+)".r

    headers.get(AUTHORIZATION) match {
      case Some(basicAuthHeader(encodedAuthHeader)) =>
        decodeFromBase64(encodedAuthHeader) match {
          case decodedAuth(username, password) => Some(BasicAuthentication(username, password))
          case _                               => None
        }
      case _ => None
    }

  }

  def createBearerToken(basicAuthentication: Option[BasicAuthentication])(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Option[Authorization]] = {
    basicAuthentication.fold(Future successful (None: Option[Authorization])) {
      ba => authConnector.createBearerToken(ba.username).map(_.header(AUTHORIZATION).map(bearerToken => Authorization(bearerToken)))
    }
  }

}
