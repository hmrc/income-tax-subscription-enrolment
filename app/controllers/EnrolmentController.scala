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

package controllers

import models._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton()
class EnrolmentController @Inject()(cc: ControllerComponents)
    extends BackendController(cc) {

  def enrol(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val correlationId = request.headers.get("correlationId").getOrElse("No correlationId")
    withJsonBody[EnrolmentDetails] { enrolmentDetails =>
      Future.successful(Created(Json.toJson(EnrolmentResponse(Seq(Outcome("ES6","ok"))))))
    }
  }
}
