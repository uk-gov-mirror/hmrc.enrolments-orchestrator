import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-play-26"           % "1.3.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %%  "bootstrap-play-26"          % "1.3.0"               % Test classifier "tests",
    "uk.gov.hmrc"             %%  "service-integration-test"   % "0.9.0-play-26"       % "it",
    "com.github.tomakehurst"  %   "wiremock-jre8"              % "2.22.0"              % "it",
    "org.scalatest"           %%  "scalatest"                  % "3.0.8"               % "test",
    "com.typesafe.play"       %%  "play-test"                  % current               % "test",
    "org.mockito"             %   "mockito-core"               % "3.1.0"               % "test",
    "org.pegdown"             %   "pegdown"                    % "1.6.0"               % "test, it",
    "org.scalatestplus.play"  %%  "scalatestplus-play"         % "3.1.2"               % "test, it"
  )

}
