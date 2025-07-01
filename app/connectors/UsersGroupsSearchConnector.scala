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

import com.google.inject.Singleton
import config.AppConfig
import connectors.UsersGroupsSearchConnector.GetUsersForGroupResponse
import uk.gov.hmrc.auth.core.CredentialRole
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UsersGroupsSearchConnector @Inject()(
  http: HttpClientV2,
  appConfig: AppConfig
)(implicit ec: ExecutionContext) {

  def getUsersForGroup(
    groupId: String
  )(implicit hc:HeaderCarrier): Future[GetUsersForGroupResponse] = {
    import UserGroupSearchParsers._
    val url = url"${appConfig.usersForGroupUrl(groupId)}"
    http.get(url).execute
  }

}

object UsersGroupsSearchConnector {

  type GetUsersForGroupResponse = Either[GetUsersForGroupFailure, GetUsersForGroupSuccess]

  sealed trait GetUsersForGroupSuccess

  case class GroupUsersFound(userIds: Seq[String]) extends GetUsersForGroupSuccess

  sealed trait GetUsersForGroupFailure

  case object InvalidJson extends GetUsersForGroupFailure

  case class UsersGroupsSearchConnectionFailure(status: Int) extends GetUsersForGroupFailure
}