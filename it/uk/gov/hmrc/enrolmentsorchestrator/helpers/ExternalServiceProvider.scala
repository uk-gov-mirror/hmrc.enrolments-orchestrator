package uk.gov.hmrc.enrolmentsorchestrator.helpers

trait ExternalServiceProvider {
  val services: Map[String, Int] = Map(
    "auth-login-api" -> 8585
  )
}
