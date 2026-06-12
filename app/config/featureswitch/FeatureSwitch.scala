/*
 * Copyright 2026 HM Revenue & Customs
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

package config.featureswitch

sealed trait FeatureSwitch {
  val name: String
  val displayText: String
}

object FeatureSwitch {
  private val prefix = "feature-switch"

  case object CompositeEnrolmentKey extends FeatureSwitch {
    override val name: String = s"$prefix.composite-enrolment-key"
    override val displayText: String = "CompositeEnrolmentKey"
  }

  case object DistributedKnownFactsPattern extends FeatureSwitch {
    override val name: String = s"$prefix.distributed-known-facts-pattern"
    override val displayText: String = "DistributedKnownFactsPattern"
  }

}
