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

package services

import connectors.EnrolmentStoreProxyConnector
import models.{EnrolmentError, Outcome}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentService @Inject()(
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector
)(implicit ec: ExecutionContext) extends Logging {

  def enrol(
    utr: String,
    nino: String,
    mtdbsa: String
  )(implicit hc: HeaderCarrier): Future[Either[ServiceFailure, Seq[Outcome]]] = {
    val resultEmpty = Right(ServiceSuccess(Seq.empty))
    for {
      resultES6 <- upsertEnrolmentAllocation(resultEmpty, mtdbsa, nino)
      resultAll <- someOtherAction(resultES6, mtdbsa, nino, utr)
    } yield {
      resultAll match {
        case Right(value) => Right(value.outcomes)
        case Left(error) => Left(error)
      }
    }
  }

  private def logError(location: String, nino: String, detail: String): Unit = {
    logger.error(s"[EnrolmentService][$location] - Auto enrolment failed for nino: $nino - $detail")
  }

  private def getOutcomesFromResult(
    result: Either[ServiceFailure, ServiceSuccess]
  ): Option[Seq[Outcome]] = {
    result match {
      case Right(value) => Some(value.outcomes)
      case Left(_) => None
    }
  }

  private def upsertEnrolmentAllocation(
    result: Either[ServiceFailure, ServiceSuccess],
    mtdbsa: String,
    nino: String
  )(implicit hc: HeaderCarrier): Future[Either[ServiceFailure, ServiceSuccess]] = {
    getOutcomesFromResult(result) match {
      case Some(outcomes) =>
        enrolmentStoreProxyConnector.upsertEnrolment(mtdbsa, nino).map {
          case Right(_) =>
            Right(ServiceSuccess(
              outcomes = outcomes :+ Outcome.success("ES6")
            ))
          case Left(EnrolmentStoreProxyConnector.UpsertEnrolmentFailure(status, message)) =>
            logError("upsertEnrolmentAllocation", nino, s"Failed to upsert enrolment with status: $status, message: $message")
            Left(ServiceFailure(
              error = Some(EnrolmentError(status.toString, message)))
            )
        }
      case None => Future.successful(result)
    }
  }

  private def someOtherAction(
    result: Either[ServiceFailure, ServiceSuccess],
    mtdbsa: String,
    nino: String,
    utr: String
  )(implicit hc: HeaderCarrier): Future[Either[ServiceFailure, ServiceSuccess]] = {
    getOutcomesFromResult(result) match {
      case Some(outcomes) =>
        enrolmentStoreProxyConnector.someOtherAction.map {
          case true =>
            Right(ServiceSuccess(
              outcomes = outcomes :+ Outcome.success("other"),
              data = Some("")
            ))
          case false =>
            logError("someOtherAction", "", "")
            Left(ServiceFailure(
              outcomes = outcomes :+ Outcome("other", "fail")
            ))
        }
      case None => Future.successful(result)
    }
  }
}

case class ServiceSuccess(
  outcomes: Seq[Outcome],
  data: Option[String] = None
)

case class ServiceFailure(
  outcomes: Seq[Outcome] = Seq.empty,
  error: Option[EnrolmentError] = None
)
