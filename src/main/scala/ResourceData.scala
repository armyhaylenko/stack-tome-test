import sttp.client._
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client.asynchttpclient.WebSocketHandler
import scala.xml.{XML, Elem}
import scala.io.Source
import scala.collection.immutable.Seq
import zio.Task
import zio.console._
import java.io.File
import zio.ZIO

sealed trait ResourceData {
  def format(str: String) = {
    str.replaceAll("(-|,|\\+|—|;|!|\\?|%|\"|`|'|\\$|:|\\*|\\(|\\)|=|\\/|\\||«|»|„|“|\\.|@|#|\\^|&|_|~|–)", "")
    .replaceAllLiterally("quot","").toLowerCase()
  }
}

case class News(srces: List[String]) extends ResourceData {
  def info(implicit backend: SttpBackend[zio.Task, Nothing, WebSocketHandler]) = srces map { src =>
      def getNews(src: String) = {
        val newsRequest = basicRequest.get(uri"$src")
        val newsResponse = newsRequest.send()
        val news = newsResponse.map(resp => XML.loadString(resp.body.right.get)).catchSome {
          case _: NoSuchElementException => newsResponse.map(resp => XML.loadString(resp.body.right.get))
        }
        val headlines = news map (_ \ "channel" \ "item" \ "title" map (n => format(n.text)))
        val links = news map (_ \ "channel" \ "item" \ "link" map (_.text))
        val fullText = news map(_ \ "channel" \ "item" \ "fulltext" map (n =>
         format(n.text).replaceAll("([a-z]|\\[|\\]|(<[^>]*>))", "")
        .replaceAllLiterally("  ", " ")
        .trim()))
        val result = for{
          hd <- headlines
          lnk <- links
          txt <- fullText
        } yield (hd zip lnk zip txt) map{
          case ((a, b), c) => (a, b, c)
        }
      result
      }
    getNews(src)
   }
}

case class Trends(src: String) extends ResourceData {
  def trendsData(implicit backend: SttpBackend[zio.Task, Nothing, WebSocketHandler]) = {
    val trendsRequest = basicRequest.get(uri"$src")
    val trendsResponse = trendsRequest.send()
    val rss = trendsResponse map (_.body.right.get) map(XML.loadString(_))
    val allTrendsItems = rss map (_ \ "channel" \ "item")
    val itemsTitle = allTrendsItems map (_ \ "title" map (n => format(n.text)))
    val keyWordsPerItem = allTrendsItems map (ns => (ns \ "news_item" \ "news_item_title").map(n => format(n.text)))
    itemsTitle
  }

}

