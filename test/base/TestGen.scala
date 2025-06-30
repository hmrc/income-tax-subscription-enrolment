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

package base

trait TestGen {
  private val letters = "ancdefghijklmnopqrstuvwxyz"

  implicit class Random[A](seq: Seq[A]) {
    def random: A = {
      val index = (math.random() * seq.size).toInt
      seq(index)
    }
  }

  private def addCharTo(text: String): String = {
    if (text.length == letters.length) {
      text
    } else {
      addCharTo(text + letters.toSeq.random)
    }
  }

  def randomString: String =
    addCharTo("")
}
