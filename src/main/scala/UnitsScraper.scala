import org.jsoup.Jsoup
import com.github.nscala_time.time.Imports._
import scala.collection.mutable.HashSet
import scala.collection.immutable.Set
import scala.collection.concurrent.TrieMap
import scala.collection.JavaConversions._
import scala.util.{Try, Success, Failure}
import java.io._
import org.nlogo.util.ClassLoaderObjectInputStream
import akka.actor.Actor
import akka.actor.Props


object UnitsScraper {
  private val oldObjectsPath = "oldResults"

  //contains all mappings for search parameter characters in URL
  private val substituteMap = new TrieMap[Char, String]
  substituteMap += (
    ' ' -> "+"
    //TODO: complete map
  )

  def formatAsURL(s: String): String = {
    val q = new scala.collection.mutable.Queue[Char]
    for (c <- s) q ++= substituteMap.getOrElse[String](c, c.toString)
    q.mkString
  }

  case object Scan //tells a UnitsScraper to scan the search results and return the results
  case class SearchResults(results: Set[AuctionObject]) //container to return the results in
}


class UnitsScraper() extends Actor {
  import UnitsScraper._ //companion object

  val searchBaseURL: String = "http://www.units.se/auction/search/?search="

  val searchParams = self.path.name

  def receive = {
    case Scan => sender ! SearchResults(search())
  }

  //scrapes search result page for this query
  def search(): Set[AuctionObject] = {
    val soup = Jsoup.connect(searchBaseURL + formatAsURL(searchParams)).get
    val items = soup.getElementsByAttributeValueStarting("id", "object")  //all tags with ID:s starting with "object"

    val objects = new HashSet[AuctionObject]

    for (item <- items.iterator) {
      objects += new AuctionObject(
        link = item
          .getElementsByTag("a")
          .first
          .attr("abs:href") ,
        endtime = DateTime.parse(
          item
            .getElementsByAttributeValueStarting("id", "endtime")
            .first
            .text ,
          DateTimeFormat.forPattern("yyyy-MM-dd HH:mm")
        ) ,
        title = item
          .getElementsByClass("title")
          .first
          .text ,
        id = item
          .id
          .replaceAll(raw"\D" , "") //removes all non-digits, i.e. the "object_" part, from the id
          .toInt ,
        bid = item
          .getElementsByAttributeValueStarting("id", "highbid")
          .text
          .replaceAll(raw"\D" , "") //removes non-digits from price string
          .toInt ,
        bidder = item
          .getElementsByAttributeValueStarting("id", "bidder")
          .text ,
        imageLink = item
          .getElementsByTag("img")
          .first
          .attr("abs:src") ,
        value = item
          .getElementsByClass("small-6")
          .last
          .ownText
          .replaceAll(raw"\D", "") //keep only the integer value
          .toInt
      )
    }
    //make immutable since the set will be sent to other actors
    objects.toSet
  }
}
