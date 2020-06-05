import sttp.client._
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client.asynchttpclient.WebSocketHandler
import scala.xml.XML
import zio.Task
import zio.console._
import scala.xml.Elem

final case class Trends(src: String){
  private def getTrends(implicit backend: SttpBackend[zio.Task, Nothing, WebSocketHandler]) = {
   val trendsRequest = basicRequest.get(uri"$src")
   val trendsResponse = trendsRequest.send()
   trendsResponse
  }
  val trendsData = AsyncHttpClientZioBackend().flatMap { implicit backend => 
    val rss = getTrends map (_.body.right.get) map(XML.loadString(_))
    val allTrendsItems = rss map (_ \ "channel" \ "item")
    val itemsTitle = allTrendsItems map (_ \ "title")
    // val keyWordsPerItem = ((itemsTitle map (t => t.map(_.text).toSet)) <*> 
    // (allTrendsItems.map(_ \ "ht:news_item" \ "ht:news_item_title")
    // .map(ns => ns.flatMap(_.text.split(" ")).toSet)))
    val keyWordsPerItem = allTrendsItems map (_ \\ "news_item_title" map (_.text))

    //TODO select keywords and not just titles!!!!!!


    keyWordsPerItem
  }

}