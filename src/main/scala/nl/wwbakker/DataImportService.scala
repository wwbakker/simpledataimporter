package nl.wwbakker

import java.net.URI
import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl.{ Compression, FileIO, Flow, Framing, Source }
import akka.util.ByteString

import scala.concurrent.ExecutionContext

trait DataImportService {

  implicit def actorSystem: ActorSystem

  implicit def executionContext: ExecutionContext = actorSystem.dispatcher

  def source(uri: URI): Source[ByteString, Any] = uri.getScheme match {
    case "file" =>
      FileIO.fromPath(Paths.get(uri))
    case "http" | "https" =>
      Source.fromFutureSource(
        Http().singleRequest(HttpRequest()).map(httpResponse =>
          if (httpResponse.status.isSuccess())
            httpResponse.entity.dataBytes
          else
            Source.failed[ByteString](
              new Exception(s"Could not retrieve data from '${uri.toString}'. " +
                s"Server responded with ${httpResponse.status.defaultMessage()}"))))
  }

  def uncompressed(gzipped: Boolean): Flow[ByteString, ByteString, NotUsed] =
    if (gzipped)
      Compression.gunzip()
    else
      Flow.fromFunction(identity)
}

// https://tpolecat.github.io/2015/04/29/f-bounds.html

object ImportStatus {
  // Typeclasses and implementations
  trait ProcessItemResult[A] {
    def friendlyMessage(a: A): String
  }
  implicit class ProcessItemResultOps[A](a: A)(implicit ev: ProcessItemResult[A]) {
    def friendlyMessage: String = ev.friendlyMessage(a)
  }

  trait ImportResult[A, B] {
    def combine(b: B, processItemResult: ProcessItemResult[A]): B
  }
  implicit class ImportResultOps[A, B](b: B)(implicit ev: ImportResult[A, B]) {
    def combine(a: ProcessItemResult[A]): B = ev.combine(b, a)
  }

  trait CsvImporter[A] extends Importer[A] {
    def maximumLineLengthInBytes: Int = 10 * 1024 * 1024 * 1024 // 10MB
    //    def csvFlow : Flow[ByteString, List[String], NotUsed] =
    //      NewlineCompatibleCsvSplitter.flow(maximumLineLengthInBytes, false).map(_.utf8String)

  }

  trait Importer[A] {
    def flow: Flow[ByteString, ProcessItemResult[A], NotUsed]
  }

}

