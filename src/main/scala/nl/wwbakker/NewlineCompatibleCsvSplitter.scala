package nl.wwbakker

import akka.NotUsed
import akka.stream.Attributes.name
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Framing.FramingException
import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import akka.util.ByteString

import scala.annotation.tailrec

/**
 * Splits lines on newlines, only if the line is not within 2 "" characters. Alternative to Framing.delimiter("\n").
 * The flipside to this ability is that it makes the processing more brittle: If a line contains an uneven number of " it might read the rest of the stream as if it's part of a single element.
 *
 * Based on akka.stream.scaladsl.Framing.DelimiterFramingStage.
 * @param maximumLineBytes
 * @param allowTruncation
 */
class NewlineCompatibleCsvSplitterGraphStage(val maximumLineBytes: Int, val allowTruncation: Boolean) extends GraphStage[FlowShape[ByteString, ByteString]] {
  val separatorByte: ByteString = ByteString("\n")
  val in: Inlet[ByteString] = Inlet[ByteString]("CsvSplitter.in")
  val out: Outlet[ByteString] = Outlet[ByteString]("CsvSplitter.out")

  override def shape: FlowShape[ByteString, ByteString] = FlowShape(in, out)

  override def initialAttributes: Attributes = name("delimiterFraming") //DefaultAttributes.delimiterFraming
  override def toString: String = "CsvSplitterFraming"

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with InHandler with OutHandler {
    private var buffer = ByteString.empty
    private var nextPossibleMatch = 0

    override def onPush(): Unit = {
      buffer ++= grab(in)
      doParse()
    }

    override def onPull(): Unit = doParse()

    override def onUpstreamFinish(): Unit = {
      if (buffer.isEmpty) {
        completeStage()
      } else if (isAvailable(out)) {
        doParse()
      } // else swallow the termination and wait for pull
    }

    private def tryPull(): Unit = {
      if (isClosed(in)) {
        if (allowTruncation) {
          push(out, buffer)
          completeStage()
        } else
          failStage(new FramingException(
            "Stream finished but there was a truncated final frame in buffer"))
      } else pull(in)
    }

    @tailrec
    def indexOfFirstSeparatorNotInQuotes(currentIndex: Int, isInsideQuotes: Boolean = false): Option[Int] =
      if (buffer.length <= currentIndex)
        None
      else if (!isInsideQuotes && buffer(currentIndex) == separatorByte.head)
        Some(currentIndex)
      else
        indexOfFirstSeparatorNotInQuotes(currentIndex + 1, if (buffer(currentIndex) == '\"') !isInsideQuotes else isInsideQuotes)

    @tailrec
    private def doParse(): Unit = {
      indexOfFirstSeparatorNotInQuotes(nextPossibleMatch) match {
        case None =>
          if (buffer.size > maximumLineBytes)
            failStage(new FramingException(s"Read ${buffer.size} bytes which is more than $maximumLineBytes without reading a line seperator"))
          else {
            // no matching character, we need to accumulate more bytes into the buffer
            nextPossibleMatch = buffer.size
            tryPull()
          }
        case Some(possibleMatchPos) =>
          if (possibleMatchPos > maximumLineBytes)
            failStage(new FramingException(s"Read ${buffer.size} bytes which is more than $maximumLineBytes without reading a line seperator"))
          else if (possibleMatchPos + separatorByte.size > buffer.size) {
            // We have found a possible match (we found the terminator) but we don't have enough bytes yet.
            // We remember the position and try again
            nextPossibleMatch = possibleMatchPos
            tryPull()
          } else if (buffer.slice(possibleMatchPos, possibleMatchPos + separatorByte.size) == separatorByte) {
            // Found a match
            val parsedFrame = buffer.slice(0, possibleMatchPos).compact
            buffer = buffer.drop(possibleMatchPos + separatorByte.size).compact
            nextPossibleMatch = 0
            if (isClosed(in) && buffer.isEmpty) {
              push(out, parsedFrame)
              completeStage()
            } else push(out, parsedFrame)
          } else {
            // possibleMatchPos was not actually a match
            nextPossibleMatch += 1
            doParse()
          }
      }
    }
    setHandlers(in, out, this)
  }
}

object NewlineCompatibleCsvSplitter {
  def flow(maximumLineBytes: Int, allowTruncation: Boolean): Flow[ByteString, ByteString, NotUsed] = Flow[ByteString].via(new NewlineCompatibleCsvSplitterGraphStage(maximumLineBytes, allowTruncation))
}