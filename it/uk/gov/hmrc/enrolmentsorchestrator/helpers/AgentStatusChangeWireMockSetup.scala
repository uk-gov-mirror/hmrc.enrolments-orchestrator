package uk.gov.hmrc.enrolmentsorchestrator.helpers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.stubbing.StubMapping

trait AgentStatusChangeWireMockSetup {

  val agentStatusChangeWiremockHost: String = "localhost"
  val agentStatusChangeWiremockPort: Int = 9424
  val wireMockAgentStatusChangeServer = new WireMockServer(wireMockConfig().port(agentStatusChangeWiremockPort))

  def agentStatusChangeReturnOK: StubMapping = {
    WireMock.configureFor(agentStatusChangeWiremockHost, agentStatusChangeWiremockPort)
    wireMockAgentStatusChangeServer.start()

    stubFor(
      delete(urlEqualTo("/agent-status-change/agent/AARN123/terminate"))
        .willReturn(aResponse().withStatus(200))
    )

  }

  def agentStatusChangeReturn401: StubMapping = {
    WireMock.configureFor(agentStatusChangeWiremockHost, agentStatusChangeWiremockPort)
    wireMockAgentStatusChangeServer.start()

    stubFor(
      delete(urlEqualTo("/agent-status-change/agent/AARN123/terminate"))
        .willReturn(aResponse().withStatus(401))
    )
  }

}
