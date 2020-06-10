import zio.App
import zio.{ExitCode, ZIO}
import zio.Task
import zio.Schedule
import zio.duration._
import zio.console._
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import scala.io.Source._
import scala.collection.immutable.Seq
import java.io.File
import java.time.LocalDate

object Main extends zio.App {
  //sbt did not want to include the file with links to the assembly task, had to put the links here
  val newsRssLinks = List("https://112.ua/rss/analytics/index.rss",
  "https://112.ua/rss/statji/index.rss",
  "https://112.ua/rss/mnenie/index.rss",
  "https://112.ua/rss/interview/index.rss",
  "https://112.ua/rss/politika/index.rss",
  "https://112.ua/rss/ato/index.rss",
  "https://112.ua/rss/ekonomika/index.rss",
  "https://112.ua/rss/novosti-kanala/channel.rss?type=index",
  "https://112.ua/rss/sport/index.rss",
  "https://112.ua/rss/kiev/index.rss",
  "https://112.ua/rss/mir/index.rss",
  "https://112.ua/rss/avarii-chp/index.rss",
  "https://112.ua/rss/kriminal/index.rss")

  def print[A](zio: Task[A]) = zio.flatMap(x => putStrLn(x.toString()))

  override def run(args: List[String]): ZIO[zio.ZEnv,Nothing, ExitCode] = {
    AsyncHttpClientZioBackend().flatMap { implicit backend =>
      val date = LocalDate.now().minusDays(7).toString().replaceAll("-", "")
      val trendsLink = 
      s"https://trends.google.com/trends/api/dailytrends?hl=en-US&tz=-180&ed=$date&geo=UA&ns=15"
      val allNews = News(newsRssLinks).data
      val trends = Trends(trendsLink).data
      print(allNews) *> backend.close()
    } repeat (Schedule.spaced(300 seconds)) exitCode
  }
  
}