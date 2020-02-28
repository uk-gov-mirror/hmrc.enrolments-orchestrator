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

import play.api.mvc.Headers
import play.api.test.Helpers.AUTHORIZATION
import uk.gov.hmrc.enrolmentsorchestrator.models.BasicAuthentication
import uk.gov.hmrc.enrolmentsorchestrator.{AuthHelper, UnitSpec}
import uk.gov.hmrc.http.logging.Authorization

import scala.concurrent.ExecutionContext.Implicits.global

class AuthServiceSpec extends UnitSpec with AuthHelper {

  "AuthService" should {

    "able to interpret basic auth from headers" when {
      "there is a valid basic auth token, return BasicAuthentication" in {
        val testHeader: Headers = Headers(AUTHORIZATION -> s"Basic ${encodeToBase64("username:password")}")
        authService.getBasicAuth(testHeader) shouldBe Some(BasicAuthentication("username", "password"))
      }
      "there is an invalid basic auth token, return None" in {
        val testHeader: Headers = Headers(AUTHORIZATION -> s"Basic ${encodeToBase64("username")}")
        authService.getBasicAuth(testHeader) shouldBe None
      }
      "there is a basic auth token, but not in base64 return None" in {
        val testHeader: Headers = Headers(AUTHORIZATION -> s"Basic username:password")
        authService.getBasicAuth(testHeader) shouldBe None
      }
      "there is no token, return None" in {
        val testHeader: Headers = Headers()
        authService.getBasicAuth(testHeader) shouldBe None
      }
    }

    "able to create a bearer token" when {
      "there is a BasicAuthentication, return Authorization" in {
        val ba = BasicAuthentication("username", "password")
        await(authService.createBearerToken(Some(ba))) shouldBe Some(Authorization(s"BEARER AUTHORIZATION"))
      }
      "there is no BasicAuthentication, return None" in {
        await(authService.createBearerToken(None)) shouldBe None
      }
    }

  }

}
