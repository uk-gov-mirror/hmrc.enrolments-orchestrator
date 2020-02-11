import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-play-26"           % "1.3.0",
    "uk.gov.hmrc"             %% "auth-client"                 % "2.32.0-play-26"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %%  "bootstrap-play-26"          % "1.3.0"               % Test classifier "tests",
    "uk.gov.hmrc"             %%  "service-integration-test"   % "0.9.0-play-26"       % "it",
    "com.github.tomakehurst"  %   "wiremock-jre8"              % "2.22.0"              % "it",
    "org.scalamock"           %%  "scalamock"                  % "4.4.0"               % "test",
    "org.scalatest"           %%  "scalatest"                  % "3.0.8"               % "test",
    "com.typesafe.play"       %%  "play-test"                  % current               % "test",
    "org.pegdown"             %   "pegdown"                    % "1.6.0"               % "test, it",
    "org.scalatestplus.play"  %%  "scalatestplus-play"         % "3.1.2"               % "test, it"
  )

}
