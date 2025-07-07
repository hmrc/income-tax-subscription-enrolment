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

package config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject()(
  config: ServicesConfig,
  val configuration: Configuration
) {

  val appName: String = configuration.get[String]("appName")

  private lazy val enrolmentStoreProxyUrl: String =
    config.baseUrl("enrolment-store-proxy") + "/enrolment-store-proxy/enrolment-store"

  private lazy val usersGroupsSearchUrl: String =
    config.baseUrl("users-groups-search")

  lazy val enrolmentEnrolmentStoreUrl: String =
    s"$enrolmentStoreProxyUrl/enrolments"

  def usersForGroupUrl(groupId: String): String =
    s"$usersGroupsSearchUrl/users-groups-search/groups/$groupId/users"

  def allocateEnrolmentEnrolmentStoreUrl(groupId: String): String =
    s"$enrolmentStoreProxyUrl/groups/$groupId/enrolments"

  def assignEnrolmentUrl(userId: String): String =
    s"$enrolmentStoreProxyUrl/users/$userId/enrolments"
}
