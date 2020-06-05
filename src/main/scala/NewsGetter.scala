import sttp.client._
import zio.App
import zio.Runtime.default
import scala.concurrent.{Await, ExecutionContext}
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

object NewsGetter extends zio.App {
  val allRss = Task(fromFile(new File("src\\main\\resources\\RSSList.txt")).getLines().toList)
  val index = News("https://112.ua/rss/index.rss").subscriptionData
  val analytics = News("https://112.ua/rss/analytics/index.rss").subscriptionData

  val trends = Trends("https://trends.google.com/trends/trendingsearches/daily/rss?geo=UA").trendsData

  def print(zio: Task[String]) = zio.flatMap(putStrLn(_))

  def run(args: List[String]): ZIO[zio.ZEnv,Nothing, ExitCode] = {
    allRss.flatMap(list => putStrLn(list.toString())).repeat(Schedule.spaced(10 seconds)) exitCode
  }
  
}