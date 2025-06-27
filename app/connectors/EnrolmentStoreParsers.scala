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

import connectors.EnrolmentStoreProxyConnector.{EnrolmentAllocated, EnrolmentFailure, EnrolmentResponse, EnrolmentSuccess, UsersFound}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.JsSuccess
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object EnrolmentStoreParsers {
  private val jsonError = Left(EnrolmentFailure(INTERNAL_SERVER_ERROR, "Unexpected JSON in response"))

  private def error(response: HttpResponse) =
    Left(EnrolmentFailure(
      response.status,
      response.body
    ))

  implicit object UpsertResponseParser extends HttpReads[EnrolmentResponse] {
    override def read(method: String, url: String, response: HttpResponse): EnrolmentResponse =
      response.status match {
        case NO_CONTENT => Right(EnrolmentSuccess)
        case _ => error(response)
      }
  }

  implicit object GroupIdResponseParser extends HttpReads[EnrolmentResponse] {
    override def read(method: String, url: String, response: HttpResponse): EnrolmentResponse =
      response.status match {
        case OK =>
          (response.json \ "principalGroupIds" \ 0).validate[String] match {
            case JsSuccess(groupId, _) => Right(EnrolmentAllocated(groupId))
            case error =>
              println(error)
              jsonError
          }
        case _ => error(response)
      }
  }

  implicit object UserIdsResponseParser extends HttpReads[EnrolmentResponse] {
    override def read(method: String, url: String, response: HttpResponse): EnrolmentResponse =
      response.status match {
        case OK =>
          (response.json \ "principalUserIds").validate[Set[String]] match {
            case JsSuccess(userIds, _) => Right(UsersFound(userIds))
            case _ => jsonError
          }
        case _ => error(response)
      }
  }
}
