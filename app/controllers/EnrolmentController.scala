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
import play.api.mvc._
import services.EnrolmentService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class EnrolmentController @Inject()(
  enrolmentService: EnrolmentService
)(implicit cc: ControllerComponents, implicit val ec: ExecutionContext) extends BackendController(cc) {

  def enrol(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[EnrolmentDetails] { enrolmentDetails =>
      enrolmentService.enrol(
        enrolmentDetails.utr,
        enrolmentDetails.nino,
        enrolmentDetails.mtdbsa
      ) map {
        case Right(status) =>
          Created(Json.toJson(EnrolmentResponse(status)))
        case Left(result) => result.error match {
          case None => Created(Json.toJson(EnrolmentResponse(result.outcomes)))
          case Some(error) => UnprocessableEntity(Json.toJson(error))
        }
      }
    }
  }
}
