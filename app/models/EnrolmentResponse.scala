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

package models

import play.api.libs.json._

case class Outcome (api: String, status: String)

object Outcome {
  implicit val format: OFormat[Outcome] = Json.format[Outcome]

  def success(api: String): Outcome =
    Outcome(api, "Ok")

  def failure(api: String, message: String): Outcome =
    Outcome(api, s"Failure: $message")
}

case class EnrolmentResponse (results: Seq[Outcome])

object EnrolmentResponse {
  implicit val format: OFormat[EnrolmentResponse] = Json.format[EnrolmentResponse]
}
