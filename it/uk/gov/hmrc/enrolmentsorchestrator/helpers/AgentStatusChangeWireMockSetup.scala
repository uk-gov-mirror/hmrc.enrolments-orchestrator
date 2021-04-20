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
