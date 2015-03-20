package spray.json.streaming

import spray.json.{ deserializationError, JsonParser, ParserBase, ParserInput }

import java.lang.{ StringBuilder => JavaStringBuilder }

import scala.annotation.switch
import scala.collection.mutable

object PullParser {
  def read[T](input: ParserInput)(implicit reader: JsonStreamReader[T]): T = {
    reader.read(withInput(input))
  }

  /** @return an initialized pull parser for the given input */
  def withInput(input: ParserInput): PullParser = {
    val parser = new PullParser(input)
    parser.start()
    parser
  }
}

/** JSON parser with pull semantics. Borrows heavily from the main JsonParser by Mathias Doenitz.
  * Public methods will throw DeserializationException if the expected value isn't next in the
  * input.
  */
class PullParser(input: ParserInput) extends ParserBase(input) {

  // End-of-input sigil, forced to a compile-time constant.
  private final val EOI = '\uFFFF'

  /** The mapping of object keys to handler functions for the current object. */
  private var fieldHandlers: mutable.Map[String, ObjectValue[_]] = _

  /** Throw a DeserializationException with the given error. */
  private def fail(target: String): Nothing = {
    val cursor = input.cursor
    val ParserInput.Line(lineNr, col, text) = input.getLine(cursor)
    val summary = {
      val unexpected = if (cursorChar != EOI) {
        val c = if (Character.isISOControl(cursorChar)) {
          "\\u%04x" format cursorChar.toInt
        } else {
          cursorChar.toString
        }
        s"character '$c'"
      } else {
        "end-of-input"
      }
      s"Unexpected $unexpected at input index $cursor (line $lineNr, position $col), " +
        s"expected $target"
    }
    val detail = {
      val sanitizedText = text.map(c => if (Character.isISOControl(c)) '?' else c)
      s"\n$sanitizedText\n${" " * (col - 1)}^\n"
    }
    if (detail.isEmpty) {
      deserializationError(s"$summary")
    } else {
      deserializationError(s"$summary:$detail")
    }
  }

  /** Starts a parse. */
  private[streaming] def start(): Unit = advance()

  /** Looks for a null in the stream. If the next item in the stream is a null literal, this will
    * read it and return true; else, it leaves the stream as-is and returns false.
    */
  def maybeReadNull(): Boolean = {
    if (cursorChar == 'n') {
      `null`()
      true
    } else {
      false
    }
  }

  /** Reads a single boolean literal from the input.
    * @return the value of the literal
    * @throws DeserializationException if the next item in the stream isn't a boolean
    */
  def readBoolean(): Boolean = {
    if (cursorChar == 'f') {
      `false`()
      false
    } else if (cursorChar == 't') {
      `true`()
      true
    } else {
      fail("start of boolean literal")
    }
  }

  /** Reads a number from the stream. */
  def readNumber(): BigDecimal = {
    (cursorChar: @switch) match {
      case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' | '-' => number()
      case _ => fail("start of number literal")
    }
  }

  /** Reads a single string literal from the input.
    * @return the value of the string
    * @throws DeserializationException if the next item in the stream isn't a string
    */
  def readString(): String = {
    if (cursorChar == '"') {
      string()
    } else {
      fail("start of string literal")
    }
  }

  /** Reads a type from the stream. */
  def read[T]()(implicit handler: JsonStreamReader[T]): T = handler.read(this)

  private def startArrayInternal(): Unit = {
    if (cursorChar == '[') {
      advance()
      ws()
    } else {
      fail("start of non-null array")
    }
  }

  /** Start a JS array, and reads the first item.
    * @throws DeserializationException if there isn't a start of an array in the stream
    */
  def startArray[T]()(implicit handler: JsonStreamReader[T]): T = {
    startArrayInternal()
    handler.read(this)
  }

  /** Read an item from an array. Behavior is undefined if not inside of an array when called. */
  def readArrayItem[T]()(implicit handler: JsonStreamReader[T]): T = {
    require(',')
    ws()
    handler.read(this)
  }

  /** Ends an array that was started with startArray. */
  def endArray(): Unit = {
    require(']')
    ws()
  }

  /** Reads an array into an iterator. If additional methods are called on this parser before the
    * returned iterator is exhausted, behavior is undefined.
    */
  def readArray[T]()(implicit handler: JsonStreamReader[T]): Iterator[T] = {
    startArrayInternal()
    val self = this
    new Iterator[T]() {
      var isFirst = true
      override def hasNext(): Boolean = {
        val atEnd = ws(']')
        if (!atEnd) {
          // Require a comma if this isn't the first element.
          if (!isFirst) {
            require(',')
            ws()
          }
          isFirst = false
        }
        !atEnd
      }

      override def next(): T = handler.read(self)
    }
  }

  /** Start a JS object.
    * @throws DeserializationException if there isn't a start of an object in the stream
    */
  def startObject(): Unit = {
    if (cursorChar == '{') {
      fieldHandlers = mutable.HashMap.empty
      advance()
      ws()
    } else {
      fail("start of non-null object")
    }
  }

  /** Registers a handler for a given object key. If not called within parsing an object, behavior
    * is undefined.
    */
  def readField[T](key: String)(implicit fieldHandler: JsonStreamReader[T]): ObjectValue[T] = {
    val handler = new ObjectValue(key, fieldHandler)
    fieldHandlers(key) = handler
    handler
  }

  /** Finishes parsing an object. This will call any handlers registered for field names. */
  def endObject(): Unit = {
    // Copy the current handler map reference, in case one of them wants to parse an object.
    val handlers = fieldHandlers
    handlers.values foreach { _.setDefault() }
    if (cursorChar != '}') {
      do {
        val key = `string`()
        require(':')
        ws()
        handlers.get(key) match {
          case Some(handler) => handler.readValue(this)
          case None =>
            // Skip the next value in the input.
            // TODO: Skip value in a streaming way!!!
            val jsonParser = new JsonParser(input)
            jsonParser.cursorChar = cursorChar
            jsonParser.value()
            cursorChar = jsonParser.cursorChar
        }
      } while (ws(','))
    }
    require('}')
    ws()
  }
}