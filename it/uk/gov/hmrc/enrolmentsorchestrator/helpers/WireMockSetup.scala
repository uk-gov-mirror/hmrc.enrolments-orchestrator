package uk.gov.hmrc.enrolmentsorchestrator.helpers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.stubbing.{Scenario, StubMapping}

trait WireMockSetup {

  val wiremockHost: String = "localhost"
  val wiremockPort: Int = 9595
  val wireMockServer = new WireMockServer(wireMockConfig().port(wiremockPort))
  WireMock.configureFor(wiremockHost, wiremockPort)

  def startESProxyWireMockServerFullHappyPath: StubMapping = {

    wireMockServer.start()

    stubFor(
      get(urlEqualTo("/enrolment-store/enrolments/HMRC-AS-AGENT~ARN~AARN123/groups?type=principal"))
        .willReturn(aResponse().withStatus(200)
          .withBody("""{"principalGroupIds":["90ccf333-65d2-4bf2-a008-01dfca702161"]}"""))
    )

    stubFor(
      get(urlEqualTo("/enrolment-store/enrolments/HMRC-AS-AGENT~ARN~AARN123/users"))
        .inScenario("Default")
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody( """{"principalUserIds": ["anyCredId", "2", "3"], "delegatedUserIds": ["a", "b"]}""" )
        )
        .willSetStateTo("After")
    )

    stubFor(
      get(urlEqualTo("/enrolment-store/enrolments/HMRC-AS-AGENT~ARN~AARN123/users"))
        .inScenario("Default")
        .whenScenarioStateIs("After")
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody( """{"principalUserIds": ["2","3"],"delegatedUserIds": ["a","b"]}""" )
        )
    )

    stubFor(
      get(urlEqualTo("/enrolment-store/users/anyCredId/enrolments"))
        .willReturn(aResponse().withStatus(204).withBody( """{}""" ))
    )

    stubFor(
      delete(urlEqualTo("/enrolment-store/groups/90ccf333-65d2-4bf2-a008-01dfca702161/enrolments/HMRC-AS-AGENT~ARN~AARN123"))
        .willReturn(aResponse().withStatus(204))
    )
  }

  def startESProxyWireMockServerReturn204: StubMapping = {

    wireMockServer.start()

    stubFor(
      get(urlEqualTo("/enrolment-store/enrolments/HMRC-AS-AGENT~ARN~AARN123/groups?type=principal"))
        .willReturn(aResponse().withStatus(204))
    )
  }

}
