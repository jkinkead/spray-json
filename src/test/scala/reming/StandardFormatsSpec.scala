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
package reming

import org.scalatest.FlatSpec

import java.io.StringWriter

class StandardFormatsSpec extends FlatSpec {
  import DefaultProtocol._

  "Option format" should "read None" in {
    JsonParser.read[Option[String]]("null") === None
  }
  it should "write None" in {
    val sw = new StringWriter
    PrettyPrinter.printTo[Option[String]](sw, None)
    sw.toString === "null"
  }
  it should "read Some" in {
    JsonParser.read[Option[String]](""""Exists"""") === Some("Exists")
  }
  it should "write Some" in {
    val sw = new StringWriter
    PrettyPrinter.printTo[Option[String]](sw, Some("foo"))
    sw.toString === """"foo""""
  }

  "Either format" should "read Right" in {
    JsonParser.read[Either[String, Int]]("[1, 123]") === Right(123)
  }
  it should "read Left" in {
    JsonParser.read[Either[String, Int]]("""[0, "str"]""") === Left("str")
  }

  "Tuple1 format" should "read values" in {
    JsonParser.read[Tuple1[Int]]("22") === Tuple1(22)
  }

  "Tuple2 format" should "read values" in {
    JsonParser.read[(Int, Double)]("[22, 1.0]") === (22, 1.0)
  }

  "Tuple3 format" should "read values" in {
    JsonParser.read[(Int, Double, String)]("""[22, 1.0, "str"]""") === (22, 1.0, "str")
  }
  it should "write values" in {
    val sw = new StringWriter
    PrettyPrinter.printTo(sw, (22, 1.0, "str"))
    sw.toString === """[22, 1.0, "str"]"""
  }

  "Tuple4 format" should "read values" in {
    JsonParser.read[(Int, Double, String, Int)]("""[22, 1.0, "str", 42]""") === (22, 1.0, "str", 42)
  }

  "Tuple5 format" should "read values" in {
    JsonParser.read[(Int, Double, String, Int, Int)]("""[22, 1.0, "str", 42, 41]""") ===
      (22, 1.0, "str", 42, 41)
  }

  "Tuple6 format" should "read values" in {
    JsonParser.read[(Int, Double, String, Int, Int, Int)](
      """[22, 1.0, "str", 42, 41, 40]"""
    ) === (22, 1.0, "str", 42, 41, 40)
  }

  "Tuple7 format" should "read values" in {
    JsonParser.read[(Int, Double, String, Int, Int, Int, String)](
      """[22, 1.0, "str", 42, 41, 40, "i"]"""
    ) === (22, 1.0, "str", 42, 41, 40, "i")
  }
}
