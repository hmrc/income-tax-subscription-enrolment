/*
 * Copyright 2025 HM Revenue & Customs
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
import connectors.EnrolmentStoreProxyConnector.EnrolmentResponse
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

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
  )(implicit hc: HeaderCarrier): Future[EnrolmentResponse] = {
    val enrolmentKey = EnrolmentKey(
      serviceName = "HMRC-MTD-IT",
      identifiers = "MTDITID" -> mtdbsa
    )
    val requestBody = EnrolmentStoreProxyRequest(Seq(EnrolmentStoreProxyVerifier(
      key = "NINO",
      value = nino
    )))
    import connectors.EnrolmentStoreParsers.UpsertResponseParser
    http.put(url"${appConfig.enrolmentEnrolmentStoreUrl}/${enrolmentKey.asString}").withBody(
      body = Json.toJson(requestBody)
    ).execute
  }

  def getAllocatedEnrolments(
    utr: String
  )(implicit hc: HeaderCarrier): Future[EnrolmentResponse] = {
    import connectors.EnrolmentStoreParsers.GroupIdResponseParser
    val enrolmentKey = EnrolmentKey(
      serviceName = "IR-SA",
      identifiers = "UTR" -> utr
    )
    val url = url"${appConfig.enrolmentEnrolmentStoreUrl}/${enrolmentKey.asString}/groups?type=principal"
    http.get(url).execute
  }
}

object EnrolmentStoreProxyConnector {

  type EnrolmentResponse = Either[EnrolmentFailure, EnrolmentSuccess]

  sealed trait EnrolmentSuccess

  case object EnrolmentSuccess extends EnrolmentSuccess

  case class EnrolmentAllocated(groupID: String) extends EnrolmentSuccess

  case class EnrolmentFailure(status: Int, message: String)
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

case class EnrolmentKey(serviceName: String, identifiers: (String, String)*) {
  def asString: String = {
    val formattedIdentifiers = identifiers map { case (key, value) => s"$key~$value" }

    serviceName +: formattedIdentifiers mkString "~"
  }
}
