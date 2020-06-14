import sttp.client._
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client.asynchttpclient.WebSocketHandler
import scala.xml.{XML, Elem}
import scala.collection.immutable.Seq
import zio.Task
import zio.console._
import zio.ZIO
import io.circe.parser._
import io.circe.Json.Null
import io.circe.Decoder
import io.circe.Json
import zio.stream.ZStream

sealed trait ResourceData[A] {
  /* 
  utility function to clean up the text from special characters
   */
  def format(str: String) = {
    str
    .replaceAll("(-|,|\\+|—|;|!|\\?|%|\"|`|'|\\$|:|\\*|\\(|\\)|=|\\/|\\||«|»|„|“|\\.|@|#|\\^|&|_|~|–|(\\s\\s)|(quot))", "")
    .toLowerCase()
  }
  /* our ultimate resource data getter */
  def data(implicit backend: SttpBackend[zio.Task, Nothing, WebSocketHandler]): ZStream[Any, Throwable, A]
}
/* parameter srces has type List[String], because we fetch RSS from multiple links (which are located 
in src/main/resources/RSSList.txt)
(e.g. "Only Analytics", "Only Sports")

type (String, String, String, String) corresponds to (title, link, full text, date of publishing)
 */
case class NewsEntry(title: String, link: String, descr: String, pubDate: String)

case class News(srces: List[String]) extends ResourceData[NewsEntry] {
  def data(implicit backend: SttpBackend[zio.Task, Nothing, WebSocketHandler]) = srces.map{ src =>
      def getNews(src: String) = {
        val newsResponse = basicRequest.get(uri"$src").send()
        val streamedResponse = ZStream.fromEffect(newsResponse)
        val news = streamedResponse.withFilter(_.body.isRight).mapM(resp => Task(XML.loadString(resp.body.right.get)))
        val item = news map (_ \ "channel" \ "item")
        val headline = item map (_ \ "title" map (n => format(n.text)))
        val link = item map (_ \ "link" map (_.text))
        val description = item map(_ \ "description" map (n =>
        //additional cleanup to remove all the tags and links
         format(n.text).replaceAll("([a-z]|\\[|\\]|(<[^>]*>))", "")
        .trim()))
        val pubDate = item map (_ \ "pubDate" map (_.text))
        val newsEntry = for{
          hd <- headline
          lnk <- link
          descr <- description
          date <- pubDate
        } yield (hd zip lnk zip descr zip date) map{
          case (((a, b), c), d) => NewsEntry(a, b, c, d) //extracting values out of nested tuples
        }
      newsEntry.flatMap(seq => ZStream.fromIterable(seq))
      }
    getNews(src)
   }.reduce((s1, s2) => s1.concat(s2)) //fold to unify all news from different sections
}

case class TrendEntry(title: String)
case class Trends(src: String) extends ResourceData[TrendEntry] {
  implicit val decoder = Decoder.decodeJson
  def data(implicit backend: SttpBackend[zio.Task, Nothing, WebSocketHandler]) = {
    val trendsResponse = basicRequest.get(uri"$src").send()
    //json comes with this weird ")]}'," start, clean up this thing
    val parsed = trendsResponse map (json => parse(json.body.right.getOrElse("{}").replaceFirst("(\\)]}',)", "")))
    val titlesEncoded = parsed map (_.right.getOrElse(Json.Null).findAllByKey("query"))
    val titlesDecoded = titlesEncoded map (_ map (json => TrendEntry(format(json.toString().toLowerCase()))))
    ZStream.fromIterableM(titlesDecoded)
  }

}

