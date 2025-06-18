/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors

import config.AppConfig
import connectors.EnrolmentStoreProxyConnector.{UpsertEnrolmentFailure, UpsertEnrolmentResponse, UpsertEnrolmentSuccess, getEnrolmentKey}
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentStoreProxyConnector @Inject()(
  http: HttpClientV2,
  appConfig: AppConfig
)(implicit ec: ExecutionContext) {

  def upsertEnrolment(
    mtdbsa: String,
    nino: String
  )(implicit hc: HeaderCarrier): Future[UpsertEnrolmentResponse] = {
    val enrolmentKey = getEnrolmentKey(mtdbsa)
    val requestBody = EnrolmentStoreProxyRequest(Seq(EnrolmentStoreProxyVerifier(
      key = "NINO",
      value = nino
    )))
    http.put(appConfig.upsertEnrolmentEnrolmentStoreUrl(enrolmentKey)).withBody(
      body = Json.toJson(requestBody)
    ).execute.map(read)
  }

  private def read(response: HttpResponse): UpsertEnrolmentResponse =
    response.status match {
      case NO_CONTENT => Right(UpsertEnrolmentSuccess)
      case status => Left(UpsertEnrolmentFailure(status, response.body))
    }

  def someOtherAction: Future[Boolean] =
    Future.successful(true)
}

object EnrolmentStoreProxyConnector {

  private type UpsertEnrolmentResponse = Either[UpsertEnrolmentFailure, UpsertEnrolmentSuccess.type]

  case object UpsertEnrolmentSuccess

  case class UpsertEnrolmentFailure(status: Int, message: String)

  def getEnrolmentKey(mtdbsa: String): String =
    s"HMRC-MTD-IT~MTDITID~$mtdbsa"
}

case class EnrolmentStoreProxyVerifier(
  key: String,
  value: String
)

object EnrolmentStoreProxyVerifier {
  implicit val format: OFormat[EnrolmentStoreProxyVerifier] = Json.format[EnrolmentStoreProxyVerifier]
}

case class EnrolmentStoreProxyRequest(
  verifiers: Seq[EnrolmentStoreProxyVerifier]
)

object EnrolmentStoreProxyRequest {
  implicit val format: OFormat[EnrolmentStoreProxyRequest] = Json.format[EnrolmentStoreProxyRequest]
}
