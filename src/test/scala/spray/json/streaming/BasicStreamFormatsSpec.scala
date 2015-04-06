/*
 * Copyright (C) 2015 by Jesse Kinkead
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package spray.json.streaming

import org.specs2.mutable._

import java.io.StringWriter

class BasicStreamFormatsSpec extends Specification {
  import DefaultStreamProtocol._

  "Int format" should {
    "read an int" in {
      PullParser.read[Int]("123") === 123
    }
    "write an int" in {
      val sw = new StringWriter
      PrettyStreamPrinter.printTo(sw, 123)
      sw.toString === "123"
    }
  }

  "String format" should {
    "read a string" in {
      PullParser.read[String](""""abc"""") === "abc"
    }
    "write a string" in {
      val sw = new StringWriter
      PrettyStreamPrinter.printTo(sw, "abc")
      sw.toString === """"abc""""
    }
  }
}
