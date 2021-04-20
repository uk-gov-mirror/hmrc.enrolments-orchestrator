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

import play.api.Logger
import uk.gov.hmrc.enrolmentsorchestrator.helpers.{LogCapturing, TestSetupHelper}
import uk.gov.hmrc.enrolmentsorchestrator.services.EnrolmentsStoreService
import uk.gov.hmrc.http.HeaderNames


class AgentControllerISpec extends TestSetupHelper with LogCapturing {

  override def afterEach {
    wireMockEnrolmentStoreServer.stop()
    wireMockAgentStatusChangeServer.stop()
  }

  "DELETE      /enrolments-orchestrator/agents/:arn" should {

    "return 200" when {
      "Request received and the attempt at deletion will be processed" in {

        agentStatusChangeReturnOK
        startESProxyWireMockServerFullHappyPath

        withClient { wsClient =>
          withCaptureOfLoggingFrom(Logger) { logEvents =>
            await(
              wsClient.url(resource(s"$es9DeleteBaseUrl/$testARN"))
                .withHttpHeaders(HeaderNames.authorisation -> s"Basic ${basicAuth("AgentTermDESUser:password")}")
                .delete()
            ).status shouldBe 200
            logEvents.length shouldBe 1
            logEvents.head.toString.contains("DELETE /enrolments-orchestrator/agents/AARN123 200") shouldBe true
          }
        }
      }

      """Request received but no groupId found by the arn. A logger.info about "may not actually exist" will fired""" in {

        agentStatusChangeReturnOK
        startESProxyWireMockServerReturn204

        withClient { wsClient =>
          withCaptureOfLoggingFrom(Logger(classOf[EnrolmentsStoreService])) { logEvents =>
            await(wsClient.url(resource(s"$es9DeleteBaseUrl/$testARN"))
              .withHttpHeaders(HeaderNames.authorisation -> s"Basic ${basicAuth("AgentTermDESUser:password")}")
              .delete()).status shouldBe 200
            logEvents.length shouldBe 1
            logEvents.head.toString.contains("For enrolmentKey: HMRC-AS-AGENT~AgentReferenceNumber~AARN123 200 was not returned by Enrolments-Store, " +
              "ie no groupId found there are no allocated groups (the enrolment itself may or may not actually exist) " +
              "or there is nothing to return, the response is 204 with body ") shouldBe true
          }
        }
      }
    }


    "return 401" when {

      """Request received but basic auth token not supplied. A logger.info about "response is 401" will fired""" in {
        withClient { wsClient =>
          withCaptureOfLoggingFrom(Logger) { logEvents =>
            val response = await(wsClient.url(resource(s"$es9DeleteBaseUrl/$testARN")).delete())
            response.status shouldBe 401
            response.body shouldBe "BasicAuthentication failed"
            logEvents.length shouldBe 1
            logEvents.head.toString.contains("401") shouldBe true
          }
        }
      }

      """Request received but AgentStatusChange service return 401. A logger.info about "response is 401" will fired""" in {

        agentStatusChangeReturn401

        withClient { wsClient =>
          withCaptureOfLoggingFrom(Logger) { logEvents =>
            val response = await(wsClient.url(resource(s"$es9DeleteBaseUrl/$testARN"))
              .withHttpHeaders(HeaderNames.authorisation -> s"Basic ${basicAuth("AgentTermDESUser:password")}")
              .delete())
            response.status shouldBe 401
            logEvents.length shouldBe 1
            logEvents.head.toString.contains("401") shouldBe true
          }
        }
      }
    }


    "return 500" when {
      "An exception occurred by external services such as Connection refused" in {
        val es9DeleteResponse = withClient {
          wsClient =>
            await(
              wsClient.url(resource(s"$es9DeleteBaseUrl/$testARN"))
                .withHttpHeaders(HeaderNames.authorisation -> s"Basic ${basicAuth("AgentTermDESUser:password")}")
                .delete()
            )
        }
        es9DeleteResponse.status shouldBe 500
      }
    }

  }

}
