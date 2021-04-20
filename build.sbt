import scoverage.ScoverageKeys._
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.ServiceManagerPlugin.Keys.itDependenciesList
import uk.gov.hmrc.ServiceManagerPlugin.serviceManagerSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.{ExternalService, SbtArtifactory}


val appName = "enrolments-orchestrator"

lazy val externalServices = List(
  ExternalService("AUTH"),
  ExternalService("DATASTREAM"),
  ExternalService("USER_DETAILS"),
  ExternalService("TAX_ENROLMENTS")
)

coverageExcludedPackages :=
  """<empty>;
    |Reverse.*;
    |conf.*;
    |.*BuildInfo.*;
    |.*Routes.*;
    |.*RoutesPrefix.*;""".stripMargin
coverageMinimum := 80
coverageFailOnMinimum := true
coverageHighlighting := true
parallelExecution in Test := false


lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(PlayKeys.playDefaultPort := 9456)
  .settings(
    majorVersion                     := 0,
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test
  )
  .settings(scalaVersion := "2.12.12")
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(ScalariformSettings())
  .settings(ScoverageSettings())
  .settings(SilencerSettings())
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(integrationTestSettings())
  .settings(serviceManagerSettings: _*)
  .settings(itDependenciesList := externalServices)
  .settings(resolvers += Resolver.jcenterRepo)
