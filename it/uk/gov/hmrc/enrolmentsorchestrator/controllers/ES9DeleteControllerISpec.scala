package uk.gov.hmrc.enrolmentsorchestrator.controllers

import ch.qos.logback.classic.Level
import play.api.Logger
import uk.gov.hmrc.enrolmentsorchestrator.helpers.{LogCapturing, TestSetupHelper}
import uk.gov.hmrc.http.HeaderNames


class ES9DeleteControllerISpec extends TestSetupHelper with LogCapturing {

  override def afterEach {
    wireMockServer.stop()
  }

  "DELETE      /enrolments-orchestrator/agents/:arn" should {

    "return 204" when {
      "Request received and the attempt at deletion will be processed" in {

        startESProxyWireMockServerFullHappyPath

        withClient { wsClient =>
          withCaptureOfLoggingFrom(Logger) { logEvents =>
            await(
              wsClient.url(resource(s"$es9DeleteBaseUrl/$testARN"))
                .withHttpHeaders(HeaderNames.authorisation -> authToken)
                .delete()
            ).status shouldBe 204
            logEvents.length shouldBe 3
            logEvents.filter(l => l.getLevel == Level.DEBUG) match { case logEvent =>
              logEvent.toString() shouldBe """List([DEBUG] Updating enrolments for 1 credIds, [DEBUG] Updating Auth with these Enrolments: {"individualEnrolments":{},"allEnrolments":[]})"""
            }
            logEvents.filter(l => l.getLevel == Level.INFO) match { case logEvent =>
              logEvent.toString().contains("DELETE /enrolments-orchestrator/agents/AARN123 204") shouldBe true
            }
          }
        }
      }

      """Request received but no groupId found by the arn. A logger.info about "may not actually exist" will fired""" in {

        startESProxyWireMockServerReturn204

        withClient { wsClient =>
          withCaptureOfLoggingFrom(Logger) { logEvents =>
            await(
              wsClient.url(resource(s"$es9DeleteBaseUrl/$testARN"))
                .withHttpHeaders(HeaderNames.authorisation -> authToken)
                .delete()
            ).status shouldBe 204
            logEvents.length shouldBe 2
            logEvents.filter(l => l.getLevel == Level.WARN) match { case logEvent =>
              logEvent.toString().contains("For enrolmentKey: HMRC-AS-AGENT~AgentReferenceNumber~AARN123 200 was not returned by Enrolments-Store es1, " +
                "ie no groupId found there are no allocated groups (the enrolment itself may or may not actually exist) " +
                "or there is nothing to return, the response is 204 with body ") shouldBe true
            }
          }
        }
      }

    }


    "return 401" when {
      """Request received but Bearer token not supplied. A logger.info about "response is 401" will fired""" in {
        withClient { wsClient =>
          withCaptureOfLoggingFrom(Logger) { logEvents =>
            await(wsClient.url(resource(s"$es9DeleteBaseUrl/$testARN")).delete()).status shouldBe 401
            logEvents.length shouldBe 2
            logEvents.filter(l => l.getLevel == Level.INFO) match { case logEvent =>
              logEvent.toString().contains("""DELETE /enrolments-orchestrator/agents/AARN123 uk.gov.hmrc.auth.core.MissingBearerToken: Bearer token not supplied""") shouldBe true
            }
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
                .withHttpHeaders(HeaderNames.authorisation -> authToken)
                .delete()
            )
        }
        es9DeleteResponse.status shouldBe 500
      }
    }

  }

}
