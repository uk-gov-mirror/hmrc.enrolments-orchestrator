import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-27"  % "3.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %%  "bootstrap-test-play-27"     % "3.0.0"               % "it",
    "uk.gov.hmrc"             %%  "service-integration-test"   % "0.12.0-play-27"       % "it",
    "com.github.tomakehurst"  %   "wiremock-jre8"              % "2.26.3"              % "it",
    "org.scalatest"           %%  "scalatest"                  % "3.0.8"               % "test",
    "com.typesafe.play"       %%  "play-test"                  % current               % "test",
    "org.mockito"             %   "mockito-core"               % "3.3.3"               % "test",
    "org.pegdown"             %   "pegdown"                    % "1.6.0"               % "test, it",
    "org.scalatestplus.play"  %%  "scalatestplus-play"         % "3.1.3"               % "test, it"
  )

}
