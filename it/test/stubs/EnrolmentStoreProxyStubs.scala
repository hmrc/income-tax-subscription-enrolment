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

package stubs

import config.AppConfig
import connectors.EnrolmentKey
import play.api.http.Status
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.http.StringContextOps

import java.net.URL

object EnrolmentStoreProxyStubs extends WireMockMethods {

  def stubES6(fail: Boolean, appConfig: AppConfig, mtdbsa: String): Unit = {
    val enrolmentKey = EnrolmentKey(
      serviceName = "HMRC-MTD-IT",
      identifiers = "MTDITID" -> mtdbsa
    )
    val url = url"${appConfig.enrolmentEnrolmentStoreUrl}/${enrolmentKey.asString}"
    when(method = PUT, uri = url.toLocal)
      .thenReturn(if (fail) Status.OK else Status.NO_CONTENT)
  }

  def stubES1(fail: Boolean, appConfig: AppConfig, utr: String, groupId: String): Unit = {
    val enrolmentKey = EnrolmentKey(
      serviceName = "IR-SA",
      identifiers = "UTR" -> utr
    )
    val json = fail match {
      case false => Json.obj("principalGroupIds" -> Json.arr(groupId))
      case true  => Json.obj()
    }
    val url = url"${appConfig.enrolmentEnrolmentStoreUrl}/${enrolmentKey.asString}/groups?type=principal"
    when(method = GET, uri = url.toLocal)
      .thenReturn(OK, json)
  }

  def stubES0(fail: Boolean, appConfig: AppConfig, utr: String, userIds: Seq[String]): Unit = {
    val enrolmentKey = EnrolmentKey(
      serviceName = "IR-SA",
      identifiers = "UTR" -> utr
    )
    val json = fail match {
      case false => Json.obj("principalUserIds" -> userIds)
      case true  => Json.obj()
    }
    val url = url"${appConfig.enrolmentEnrolmentStoreUrl}/${enrolmentKey.asString}/users"
    when(method = GET, uri = url.toLocal)
      .thenReturn(OK, json)
  }

  def stubES8(fail: Boolean, appConfig: AppConfig, groupId: String, mtdbsa: String): Unit = {
    val enrolmentKey = EnrolmentKey(
      serviceName = "HMRC-MTD-IT",
      identifiers = "MTDITID" -> mtdbsa
    )
    val url = url"${appConfig.allocateEnrolmentEnrolmentStoreUrl(groupId)}/${enrolmentKey.asString}"
    when(method = POST, uri = url.toLocal)
      .thenReturn(if (fail) Status.OK else Status.CREATED)
  }

  def stubES11(fail: Boolean, appConfig: AppConfig, userIds: Seq[String], mtdbsa: String): Unit = {
    val enrolmentKey = EnrolmentKey(
      serviceName = "HMRC-MTD-IT",
      identifiers = "MTDITID" -> mtdbsa
    )
    userIds.foreach { userId =>
      val url = url"${appConfig.assignEnrolmentUrl(userId)}/${enrolmentKey.asString}"
      when(method = POST, uri = url.toLocal)
        .thenReturn(if (fail) Status.OK else Status.CREATED)
    }
  }

  implicit class StubURL(url: URL) {
    def toLocal: String = {
      val str = url.toString
      str.substring(str.indexOf("/e")).replace("?", "\\?")
    }
  }
}
