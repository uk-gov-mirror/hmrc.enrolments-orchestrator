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

package uk.gov.hmrc.enrolmentsorchestrator.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, OFormat, OWrites}

case class AgentDeleteRequest(ARN: String, terminationDate: Long)

object AgentDeleteRequest {
  implicit val format: OFormat[AgentDeleteRequest] = Json.format[AgentDeleteRequest]
}

case class AgentDeleteResponse(ARN: String, terminationDate: Long, success: Boolean, ResponseCode: Int, failureReason: Option[String])

object AgentDeleteResponse {
  implicit val auditWrites: OWrites[AgentDeleteResponse] = (
    (JsPath \ "ARN").write[String] and
    (JsPath \ "terminationDate").write[Long] and
    (JsPath \ "success").write[Boolean] and
    (JsPath \ "ResponseCode").write[Int] and
    (JsPath \ "failureReason").writeNullable[String]) (response =>
      (response.ARN,
        response.terminationDate,
        response.success,
        response.ResponseCode,
        response.failureReason
      )
    )
}
