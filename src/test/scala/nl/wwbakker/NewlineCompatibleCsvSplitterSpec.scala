package nl.wwbakker

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }
import akka.testkit.TestKit
import akka.util.ByteString
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, FlatSpecLike, Matchers }

import scala.collection.immutable
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

class NewlineCompatibleCsvSplitterSpec extends TestKit(ActorSystem("NewlineCompatibleCsvSplitterSpec"))
  with FlatSpecLike with Matchers with BeforeAndAfterAll {

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)
  implicit val mat: Materializer = ActorMaterializer()

  behavior of "NewLineCompatibleCsvSplitter"

  def s2bs(s: String): immutable.Iterable[ByteString] = s.split('|').map(ByteString.apply).to[scala.collection.immutable.Iterable]
  def await[A](f: Future[A]): A = Await.result(f, 10.seconds)

  val flowUnderTest: Flow[ByteString, ByteString, NotUsed] = NewlineCompatibleCsvSplitter.flow(1024 * 1024, false)

  it should "split a line without any special characters correctly" in {
    val inputText = "Lorem| ipsum dolor si|t amet,\n consectetur adipiscing elit. Morbi a placerat sem. Pra|esent\n eleifend sem et urna auctor sagittis. Donec nec neque quis arcu gravida \nvestib|ulum. Cras tincidunt, eros id |semper viverra, \nnisl quam effici|tur erat, sit a|met dignissim elit diam|\n"
    val output: List[ByteString] = inputText.replaceAll("\\|", "").split("\n").map(ByteString.apply).toList
    val source = Source[ByteString](s2bs(inputText))
    await(source.via(flowUnderTest).runWith(Sink.seq)) shouldBe output
  }

  it should "not split a line when the newline is within double quotes" in {
    val inputText = "Lor|em"
  }

  it should "not split a line when the newline is within single quotes" in {

  }

  it should "not match single quotes to double quotes" in {

  }

  it should "not match quotes when they are not at the beginning" in {

  }
}
