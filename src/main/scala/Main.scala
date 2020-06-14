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
  val newsRssLinks = List("https://www.liga.net/news/all/rss.xml",
  "https://www.liga.net/news/top/rss.xml",
  "https://www.liga.net/news/rss.xml",
  "https://www.liga.net/news/articles/rss.xml",
  "https://www.liga.net/news/interview/rss.xml",
  "https://www.liga.net/news/photo/rss.xml",
  "https://www.liga.net/news/video/rss.xml",
  "https://www.liga.net/news/politics/rss.xml",
  "https://www.liga.net/news/economics/rss.xml",
  "https://www.liga.net/news/society/rss.xml",
  "https://www.liga.net/news/world/rss.xml",
  "https://www.liga.net/newsua/all/rss.xml",
  "https://www.liga.net/newsua/top/rss.xml",
  "https://www.liga.net/biz/all/rss.xml",
  "https://www.liga.net/fin/export/all.xml",
  "https://www.liga.net/tech/all/rss.xml",
  "https://www.liga.net/rss/blog.xml")

  def print[A](zio: Task[A]) = zio.flatMap(x => putStrLn(x.toString()))

  override def run(args: List[String]): ZIO[zio.ZEnv,Nothing, ExitCode] = {
    AsyncHttpClientZioBackend().flatMap { implicit backend =>
      val trends = (0 to 7).toList.map{day => 
      val intermediateDate = LocalDate.now().minusDays(day).toString().replaceAll("-", "")
      val link = s"https://trends.google.com/trends/api/dailytrends?hl=en-US&tz=-180&ed=$intermediateDate&geo=UA&ns=15"
      Trends(link).data
    }.reduce((z1, z2) => z1.concat(z2))
      val allNews = News(newsRssLinks).data
      NewsTrendsMatcher(allNews, trends).tap(e => putStrLn(e.toString)).runDrain *> putStrLn("DONE FETCHING") *>
      backend.close()
    } repeat(Schedule.spaced(5 minutes)) exitCode
  }
  
}