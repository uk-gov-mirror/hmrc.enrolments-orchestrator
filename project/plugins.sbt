resolvers += Resolver.url("HMRC Sbt Plugin Releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))( Resolver.ivyStylePatterns)

resolvers += "HMRC Releases" at "https://dl.bintray.com/hmrc/releases"

resolvers += Resolver.typesafeRepo("releases")

resolvers += Resolver.url(
  "HMRC Private Sbt Plugin Releases",
  url("https://artefacts.tax.service.gov.uk/artifactory/hmrc-sbt-plugin-releases-local"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe.play"  % "sbt-plugin"          % "2.7.6")

addSbtPlugin("uk.gov.hmrc"        % "sbt-auto-build"      % "2.13.0")

addSbtPlugin("uk.gov.hmrc"        % "sbt-git-versioning"  % "2.2.0")

addSbtPlugin("uk.gov.hmrc"        % "sbt-artifactory"     % "1.13.0")

addSbtPlugin("uk.gov.hmrc"        % "sbt-distributables"  % "2.1.0")

addSbtPlugin("uk.gov.hmrc"        % "sbt-service-manager" % "0.9.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.3")
