import zio.App
import zio.{ExitCode, ZIO}
import zio.Task
import zio.Schedule
import zio.duration._
import zio.console._
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import scala.io.Source._
import java.io.File

object Main extends zio.App {

  def print[A](zio: Task[A]) = zio.flatMap(x => putStrLn(x.toString()))

  override def run(args: List[String]): ZIO[zio.ZEnv,Nothing, ExitCode] = {
    AsyncHttpClientZioBackend().flatMap { implicit backend =>
      val allNews = News(fromFile(new File("src\\main\\resources\\RSSList.txt")).getLines().toList).data
      val trends = Trends("https://trends.google.com/trends/trendingsearches/daily/rss?geo=UA", strictMode = true).data
      print(NewsTrendsMatcher(allNews, trends)) *> backend.close()
    } repeat (Schedule.spaced(300 seconds)) exitCode
  }
  
}