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
class EnrolmentService  @Inject()(
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector
)(implicit ec: ExecutionContext) extends Logging {

  private type ServiceOutcome = Either[EnrolmentError, Seq[Outcome]]

  def enrol(
    utr: String,
    nino: String,
    mtdbsa: String
  )(implicit hc: HeaderCarrier): Future[Either[ServiceOutcome, Seq[Outcome]]] = {
    for {
      outcome <- upsertEnrolmentAllocation(Seq.empty, mtdbsa, nino)
    } yield {
      outcome
    }
  }

  private def logError(location: String, nino: String, detail: String): Unit = {
    logger.error(s"[EnrolmentService][$location] - Auto enrolment failed for nino: $nino - $detail")
  }

  private def upsertEnrolmentAllocation(
    outcomes: Seq[Outcome],
    mtdbsa: String,
    nino: String
  )(implicit hc: HeaderCarrier): Future[Either[ServiceOutcome, Seq[Outcome]]] = {
    enrolmentStoreProxyConnector.upsertEnrolment(mtdbsa, nino).map {
      case Right(_) =>
        Right(outcomes :+ Outcome.success("ES6"))
      case Left(EnrolmentStoreProxyConnector.UpsertEnrolmentFailure(status, message)) =>
        logError("upsertEnrolmentAllocation", nino, s"Failed to upsert enrolment with status: $status, message: $message")
        Left(Left(EnrolmentError(status.toString, message)))
    }
  }
}
