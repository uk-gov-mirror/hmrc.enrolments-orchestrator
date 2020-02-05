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

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json
import play.api.libs.json._
import uk.gov.hmrc.enrolmentsorchestrator.models._

class AuditModelsSpec extends WordSpec with Matchers
{
  "The AuditModels" should {
    "match the AgentDeleteRequest scala object" in {
      val agentDeleteRequest = AgentDeleteRequest("XXXX1234567", 15797056635L)
      val agentDeleteRequestJson = Json toJson agentDeleteRequest

      agentDeleteRequestJson \ "ARN" shouldBe JsDefined(JsString("XXXX1234567"))
      agentDeleteRequestJson \ "terminationDate" shouldBe JsDefined(JsNumber(15797056635L))
    }
    "match the AgentDeleteResponse scala object" in {
      val agentDeleteResponse = AgentDeleteResponse("XXXX1234567", 15797056635L, false: Boolean, 500, Some("Internal Server Error"))
      val agentDeleteResponseJson = Json toJson agentDeleteResponse

      agentDeleteResponseJson \ "ARN" shouldBe JsDefined(JsString("XXXX1234567"))
      agentDeleteResponseJson \ "terminationDate" shouldBe JsDefined(json.JsNumber(15797056635L))
      agentDeleteResponseJson \ "success" shouldBe JsDefined(json.JsBoolean(false))
      agentDeleteResponseJson \ "ResponseCode" shouldBe JsDefined(json.JsNumber(500))
      agentDeleteResponseJson \ "failureReason" shouldBe JsDefined(json.JsString("Internal Server Error"))
    }
  }
}
