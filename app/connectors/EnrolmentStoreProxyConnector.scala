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
import connectors.EnrolmentStoreProxyConnector.getEnrolmentKey
import connectors.httpparsers.UpsertEnrolmentResponseHttpParser
import connectors.httpparsers.UpsertEnrolmentResponseHttpParser.UpsertEnrolmentResponse
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentStoreProxyConnector @Inject()(
  http: HttpClientV2,
  appConfig: AppConfig
)(implicit ec: ExecutionContext) {

  def upsertEnrolment(
    mtdbsa: String,
    nino: String)
  (implicit hc: HeaderCarrier): Future[UpsertEnrolmentResponse] = {
    val enrolmentKey = getEnrolmentKey(mtdbsa)
    val requestBody = Json.obj(
      "verifiers" -> Json.arr(
        Json.obj(
          "key" -> "NINO",
          "value" -> nino
        )
      )
    )
    http.put(appConfig.upsertEnrolmentEnrolmentStoreUrl(enrolmentKey)).withBody(
      body = requestBody
    ).execute.map(
      UpsertEnrolmentResponseHttpParser.read
    )
  }
}
 object EnrolmentStoreProxyConnector {
   def getEnrolmentKey(mtdbsa: String): String =
     s"HMRC-MTD-IT~MTDITID~$mtdbsa"
 }