import zio.Task
import zio.stream.ZStream
object NewsTrendsMatcher {
  def apply(newsStream: ZStream[Any, Throwable, NewsEntry], trendsStream: ZStream[Any, Throwable, TrendEntry]) = {
      //determines first trend for a given entry. an entry may be an article, digest, news report etc.
    def inTrend(entry: NewsEntry, trend: TrendEntry): Option[(NewsEntry, String)] = {
      val titleAndArticle = entry.title + " " + entry.descr
      if(titleAndArticle.contains(trend.title)) Some(entry, trend.title) else None
    }
    for{
      trendItem <- trendsStream
      newsItem <- newsStream if inTrend(newsItem, trendItem).isDefined
    } yield inTrend(newsItem, trendItem)
  }
}
