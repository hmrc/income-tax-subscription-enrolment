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
import config.featureswitch.FeatureSwitch.CompositeEnrolmentKey
import config.featureswitch.FeatureSwitching
import connectors.EnrolmentStoreProxyConnector.{AllocateEnrolmentResponse, AssignEnrolmentToUserResponse, EnrolmentResponse}
import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentStoreProxyConnector @Inject()(
  http: HttpClientV2,
  val appConfig: AppConfig
)(implicit ec: ExecutionContext) extends FeatureSwitching {

  def upsertEnrolment(
    mtdbsa: String,
    nino: String
  )(implicit hc: HeaderCarrier): Future[EnrolmentResponse] = {
    val requestBody = EnrolmentStoreProxyRequest(Seq(EnrolmentStoreProxyVerifier(
      key = "NINO",
      value = nino
    )))
    import connectors.EnrolmentStoreParsers.UpsertResponseParser
    val url = url"${appConfig.enrolmentEnrolmentStoreUrl}/${enrolmentKey(mtdbsa).asString}"
    http.put(url).withBody(
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

  def getUserIds(
    utr: String
  )(implicit hc: HeaderCarrier): Future[EnrolmentResponse] = {
    import connectors.EnrolmentStoreParsers.UserIdsResponseParser
    val enrolmentKey = EnrolmentKey(
      serviceName = "IR-SA",
      identifiers = "UTR" -> utr
    )
    val url = url"${appConfig.enrolmentEnrolmentStoreUrl}/${enrolmentKey.asString}/users"
    http.get(url).execute
  }

  def allocateEnrolmentWithoutKnownFacts(
    groupId: String,
    userId: String,
    mtdbsa: String,
    utr: String
  )(implicit hc: HeaderCarrier): Future[AllocateEnrolmentResponse] = {
    val requestBody = Json.obj(
      "userId" -> userId,
      "type" -> "principal",
      "action" -> "enrolAndActivate"
    )
    val url = url"${appConfig.allocateEnrolmentEnrolmentStoreUrl(groupId)}/${enrolmentKey(mtdbsa, Some(utr)).asString}"
    import connectors.EnrolmentStoreParsers.AllocateEnrolmentResponseHttpReads
    http.post(url).withBody(
      body = requestBody
    ).execute
  }

  def assignEnrolment(
    userId: String,
    mtdbsa: String
  )(implicit hc: HeaderCarrier): Future[AssignEnrolmentToUserResponse] = {
    val enrolmentKey = EnrolmentKey(
      serviceName = "HMRC-MTD-IT",
      identifiers = "MTDITID" -> mtdbsa
    )
    val url = url"${appConfig.assignEnrolmentUrl(userId)}/${enrolmentKey.asString}"
    import connectors.EnrolmentStoreParsers.AssignEnrolmentToUserHttpReads
    http.post(url).execute
  }

  def enrolmentKey(mtdbsa: String, utr: Option[String] = None): EnrolmentKey = {
    val utrId = utr match {
      case Some(utr) if isEnabled(CompositeEnrolmentKey) => Seq("UTR" -> utr)
      case _ => Seq.empty
    }
    EnrolmentKey(
      serviceName = "HMRC-MTD-IT",
      identifiers = Seq("MTDITID" -> mtdbsa) ++ utrId:_*
    )
  }
}

object EnrolmentStoreProxyConnector {

  type EnrolmentResponse = Either[EnrolmentFailure, EnrolmentSuccess]

  type AllocateEnrolmentResponse = Either[EnrolFailure, EnrolSuccess.type ]

  type AssignEnrolmentToUserResponse = Either[EnrolmentAssignmentFailure, EnrolmentAssigned.type]

  sealed trait EnrolmentSuccess

  case object EnrolmentSuccess extends EnrolmentSuccess

  case class EnrolmentAllocated(groupID: String) extends EnrolmentSuccess

  case class UsersFound(users: Seq[String]) extends EnrolmentSuccess

  case class EnrolmentFailure(status: Int, message: String)

  case object EnrolSuccess

  case class EnrolFailure(message: String)

  case object EnrolmentAssigned

  case class EnrolmentAssignmentFailure(status: Int, body: String)
}

case class EnrolmentStoreProxyVerifier(
  key: String,
  value: String
)

object EnrolmentStoreProxyVerifier {
  implicit val writes: OWrites[EnrolmentStoreProxyVerifier] = Json.writes[EnrolmentStoreProxyVerifier]
}

case class EnrolmentStoreProxyRequest(
  verifiers: Seq[EnrolmentStoreProxyVerifier]
)

object EnrolmentStoreProxyRequest {
  implicit val writes: OWrites[EnrolmentStoreProxyRequest] = Json.writes[EnrolmentStoreProxyRequest]
}

case class EnrolmentKey(serviceName: String, identifiers: (String, String)*) {
  def asString: String = {
    val formattedIdentifiers = identifiers map { case (key, value) => s"$key~$value" }

    serviceName +: formattedIdentifiers mkString "~"
  }
}
