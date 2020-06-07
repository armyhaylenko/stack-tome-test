import zio.Task
object NewsTrendsMatcher {
  def apply(news: Task[Seq[(String, String, String)]], trends: Task[Seq[String]]) = {
    news.flatMap{rawNews =>
    def inTrend(entry: (String, String, String), rawTrends: Seq[String]): Boolean = {
      val article = (entry._1 + " " + entry._3).split(" ").toSet
      rawTrends.exists(trend => 
      (article intersect trend.split(" ").toSet).size == trend.split(" ").toSet.size)
    }
    for{
      rawTrends <- trends
    } yield rawNews.filter(entry => inTrend(entry, rawTrends))
  }
}
}  
