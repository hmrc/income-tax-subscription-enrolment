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

import base.{TestData, TestGen}
import connectors.UserGroupSearchParsers.GetUsersForGroupsHttpReads
import connectors.UsersGroupsSearchConnector.{GroupUsersFound, InvalidJson, UsersGroupsSearchConnectionFailure}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.NON_AUTHORITATIVE_INFORMATION
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse

class UserGroupSearchParsersSpec extends AnyWordSpec with Matchers with TestData with TestGen {

  "UserGroupSearchParsers" when {
    "GetUsersForGroupsHttpReads is invoked" should {
      "return all ADMIN users" in {
        val creds = userIds.map(Cred(_, "Admin"))
        val response = HttpResponse(
          status = NON_AUTHORITATIVE_INFORMATION,
          headers = Map("content-type" -> Seq("application/json")),
          body = Json.toJson(creds).toString()
        )
        val actual = GetUsersForGroupsHttpReads.read("", "", response)
        actual mustBe Right(GroupUsersFound(userIds.toSeq))
      }

      "return empty list if no ADMIN users" in {
        val creds = userIds.map(Cred(_, "Assistant"))
        val response = HttpResponse(
          status = NON_AUTHORITATIVE_INFORMATION,
          headers = Map("content-type" -> Seq("application/json")),
          body = Json.toJson(creds).toString()
        )
        val actual = GetUsersForGroupsHttpReads.read("", "", response)
        actual mustBe Right(GroupUsersFound(Seq.empty))
      }

      "return error if invalid JSON" in {
        val creds = userIds.map(Cred(_, "User"))
        val response = HttpResponse(
          status = NON_AUTHORITATIVE_INFORMATION,
          headers = Map("content-type" -> Seq("application/json")),
          body = Json.toJson(creds).toString()
        )
        val actual = GetUsersForGroupsHttpReads.read("", "", response)
        actual mustBe Left(InvalidJson)
      }

      "return error if incorrect status code" in {
        val response = HttpResponse(
          status = statuses.filter(_ != NON_AUTHORITATIVE_INFORMATION).random,
        )
        val actual = GetUsersForGroupsHttpReads.read("", "", response)
        actual mustBe Left(UsersGroupsSearchConnectionFailure(response.status))
      }
    }
  }
}
