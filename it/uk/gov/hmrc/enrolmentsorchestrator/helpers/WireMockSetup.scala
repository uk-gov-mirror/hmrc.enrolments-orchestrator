package uk.gov.hmrc.enrolmentsorchestrator.helpers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.stubbing.StubMapping

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
      get(urlEqualTo("/enrolment-store-proxy/enrolment-store/enrolments/HMRC-AS-AGENT~ARN~AARN123/users"))
        .willReturn(aResponse().withStatus(204))
    )

    stubFor(
      delete(urlEqualTo("/enrolment-store-proxy/enrolment-store/groups/90ccf333-65d2-4bf2-a008-01dfca702161/enrolments/HMRC-AS-AGENT~ARN~AARN123"))
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
