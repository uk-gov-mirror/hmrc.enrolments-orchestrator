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

trait EnrolmentStoreWireMockSetup {

  val enrolmentStoreWireMockHost: String = "localhost"
  val enrolmentStoreWireMockPort: Int = 9595
  val wireMockEnrolmentStoreServer = new WireMockServer(wireMockConfig().port(enrolmentStoreWireMockPort))

  def startESProxyWireMockServerFullHappyPath: StubMapping = {
    WireMock.configureFor(enrolmentStoreWireMockHost, enrolmentStoreWireMockPort)
    wireMockEnrolmentStoreServer.start()

    stubFor(
      get(urlEqualTo("/enrolment-store-proxy/enrolment-store/enrolments/HMRC-AS-AGENT~AgentReferenceNumber~AARN123/groups?type=principal"))
        .willReturn(aResponse().withStatus(200)
          .withBody("""{"principalGroupIds":["90ccf333-65d2-4bf2-a008-01dfca702161"]}"""))
    )

    stubFor(
      get(urlEqualTo("/enrolment-store-proxy/enrolment-store/enrolments/HMRC-AS-AGENT~AgentReferenceNumber~AARN123/users"))
        .willReturn(aResponse().withStatus(204))
    )

    stubFor(
      delete(urlEqualTo("/enrolment-store-proxy/enrolment-store/groups/90ccf333-65d2-4bf2-a008-01dfca702161/enrolments/HMRC-AS-AGENT~AgentReferenceNumber~AARN123"))
        .willReturn(aResponse().withStatus(204))
    )
  }

  def startESProxyWireMockServerReturn204: StubMapping = {
    WireMock.configureFor(enrolmentStoreWireMockHost, enrolmentStoreWireMockPort)
    wireMockEnrolmentStoreServer.start()

    stubFor(
      get(urlEqualTo("/enrolment-store-proxy/enrolment-store/enrolments/HMRC-AS-AGENT~AgentReferenceNumber~AARN123/groups?type=principal"))
        .willReturn(aResponse().withStatus(204))
    )
  }

}
