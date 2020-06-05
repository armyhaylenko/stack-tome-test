import sttp.client._
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client.asynchttpclient.WebSocketHandler
import scala.xml.{XML, Elem}
import scala.io.Source
import zio.Task
import zio.console._
import java.io.File
import zio.ZIO
case class News(src: String) {
  private def getNews(implicit backend: SttpBackend[zio.Task, Nothing, WebSocketHandler]) = {
    val newsRequest = basicRequest.get(uri"$src")
    val newsResponse = newsRequest.send()
    newsResponse
  }
  val subscriptionData = AsyncHttpClientZioBackend().flatMap{implicit backend => 
    val news = getNews.map(resp => (XML.loadString(resp.body.right.get)))
    val headlines = news.map(_ \\ "title" map (_.text.toLowerCase()))
    val links = news.map(_ \\ "link" map (_.text))
    for{
      hd <- headlines
      lnk <- links
    } yield (hd zip lnk) mkString("\n") replaceAllLiterally(")", "")
  }
}
