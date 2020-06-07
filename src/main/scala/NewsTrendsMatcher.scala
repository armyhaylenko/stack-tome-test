import zio.Task
object NewsTrendsMatcher {
  def apply(news: Task[Seq[(String, String, String, String)]], trends: Task[Seq[String]]) = {
    news.flatMap{rawNews =>
      //determines first trend for a given entry. an entry may be an article, digest, news report etc.
    def inTrend(entry: (String, String, String, String), rawTrends: Seq[String]): Option[String] = {
      val article = (entry._1 + " " + entry._3).split(" ").toSet
      rawTrends.find(trend => 
      (article intersect trend.split(" ").toSet).size == trend.split(" ").toSet.size)
    }
    for{
      rawTrends <- trends
    } yield rawNews.withFilter(entry => inTrend(entry, rawTrends) match {
      //if a trend exists for an entry, inTrend return Some(value), otherwise none
      case Some(_) => true
      case None => false
    })
    //to make things readable, we take first 100 chars of the full text
    .map(news => (news._1, news._2, news._3.substring(0, Math.min(100, news._3.length)) + "...", news._4, inTrend(news, rawTrends)))
  }
}
}  
