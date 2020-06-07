import sttp.client._
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client.asynchttpclient.WebSocketHandler
import scala.xml.{XML, Elem}
import scala.collection.immutable.Seq
import zio.Task
import zio.console._
import zio.ZIO

sealed trait ResourceData[A] {
  /* 
  utility function to clean up the text from special characters
   */
  def format(str: String) = {
    str.replaceAll("(-|,|\\+|—|;|!|\\?|%|\"|`|'|\\$|:|\\*|\\(|\\)|=|\\/|\\||«|»|„|“|\\.|@|#|\\^|&|_|~|–)", "")
    .replaceAllLiterally("quot","").toLowerCase()
  }
  /* our ultimate resource data getter */
  def data(implicit backend: SttpBackend[zio.Task, Nothing, WebSocketHandler]): Task[Seq[A]]
}
/* parameter srces has type List[String], because we fetch RSS from multiple links (which are located 
in src/main/resources/RSSList.txt)
(e.g. "Only Analytics", "Only Sports")

type (String, String, String, String) corresponds to (title, link, full text, date of publishing)
 */
case class News(srces: List[String]) extends ResourceData[(String, String, String, String)] {
  def data(implicit backend: SttpBackend[zio.Task, Nothing, WebSocketHandler]) = srces.map{ src =>
      def getNews(src: String) = {
        val newsResponse = basicRequest.get(uri"$src").send()
        val news = newsResponse.map(resp => XML.loadString(resp.body.right.get)).catchSome {
          case _: NoSuchElementException => newsResponse.map(resp => XML.loadString(resp.body.right.get))
        }
        val item = news map (_ \ "channel" \ "item")
        val headline = item map (_ \ "title" map (n => format(n.text)))
        val link = item map (_ \ "link" map (_.text))
        val fulltext = item map(_ \ "fulltext" map (n =>
        //additional cleanup to remove all the tags and links
         format(n.text).replaceAll("([a-z]|\\[|\\]|(<[^>]*>))", "")
        .replaceAllLiterally("  ", " ")
        .trim()))
        val pubDate = item map (_ \ "pubDate" map (_.text))
        val newsEntry = for{
          hd <- headline
          lnk <- link
          txt <- fulltext
          date <- pubDate
        } yield (hd zip lnk zip txt zip date) map{
          case (((a, b), c), d) => (a, b, c, d) //extracting values out of nested tuples
        }
      newsEntry
      }
    getNews(src)
   }.fold(Task(Seq()))((z1, z2) => z1.zipWithPar(z2)(_ ++ _)) //fold to unify all news from different sections

}

case class Trends(src: String) extends ResourceData[String] {
  def data(implicit backend: SttpBackend[zio.Task, Nothing, WebSocketHandler]) = {
    val trendsResponse = basicRequest.get(uri"$src").send()
    val rss = trendsResponse map (resp => XML.loadString(resp.body.right.get))
    val item = rss map (_ \ "channel" \ "item")
    /* 
    using title is not a strict approach. may result in matches that contain words from the title 
    not in order and in different parts of the text, thus making the selection contain news that 
    aren't actually in trend.
     */
    val title = item map (_ \ "title" map (n => format(n.text)))
    /* 
    using keywords is a stricter approach. may result in not getting match at all
     */
    val kw = item map (ns => (ns \ "news_item" \ "news_item_title").map(n => format(n.text)))
    title
  }

}

