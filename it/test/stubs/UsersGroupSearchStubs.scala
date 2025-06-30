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
import play.api.http.Status.NON_AUTHORITATIVE_INFORMATION
import play.api.libs.json.Json
import stubs.EnrolmentStoreProxyStubs.{GET, when}
import uk.gov.hmrc.http.StringContextOps

import java.net.URL

object UsersGroupSearchStubs {

  def stubUGS(fail: Boolean, appConfig: AppConfig, groupId: String, userIds: Seq[String]): Unit = {
    val json = fail match {
      case false => Json.toJson(userIds.map { id => Json.obj(
        "userId" -> id,
        "credentialRole" -> "Admin"
      )})
      case true  => Json.obj()
    }
    val url = url"${appConfig.usersForGroupUrl.replace("{}", groupId)}"
    when(method = GET, uri = url.toLocal)
      .thenReturn(NON_AUTHORITATIVE_INFORMATION, json)
  }

  implicit class StubURL(url: URL) {
    def toLocal: String = {
      val str = url.toString
      str.substring(str.indexOf("/u"))
    }
  }
}
