import sttp.client._
import zio.App
import zio.Runtime.default
import zio.{ExitCode, ZIO}
import zio.Task
import zio.Schedule
import zio.duration._
import zio.console._
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.zio.ZioWebSocketHandler
import scala.xml.XML
import scala.xml.Elem
import scala.io.Source._
import java.io.File
import scala.collection.immutable.{Nil, Seq}
import scala.annotation.tailrec

object NewsGetter extends zio.App {

  def print[A](zio: Task[A]) = zio.flatMap(x => putStrLn(x.toString()))

  override def run(args: List[String]): ZIO[zio.ZEnv,Nothing, ExitCode] = {
    AsyncHttpClientZioBackend().flatMap { implicit backend =>
      val allNews = News(fromFile(new File("src\\main\\resources\\RSSList.txt"))
        .getLines().toList).info.fold(Task(Seq()))((z1, z2) => z1.zipWithPar(z2)(_ ++ _))
      val trends = Trends("https://trends.google.com/trends/trendingsearches/daily/rss?geo=UA").trendsData
      print(NewsTrendsMatcher(allNews, trends)) *> backend.close()
    } repeat (Schedule.spaced(300 seconds)) exitCode
  }
  
}