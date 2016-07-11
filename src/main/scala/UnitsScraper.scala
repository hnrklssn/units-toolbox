import org.jsoup.Jsoup
import com.github.nscala_time.time.Imports._
import scala.collection.mutable.HashSet
import scala.collection.mutable.Set
import scala.collection.concurrent.TrieMap
import scala.collection.JavaConversions._
import mapper._
import scalax.io.JavaConverters._
import scalax.file.Path
import scalax.file.defaultfs.DefaultPath
import scala.util.{Try, Success, Failure}
import java.io._

object UnitsScraper2 {
  private val oldObjectsPath = "oldResults"

  //contains all mappings for search parameter characters in URL
  private val substituteMap = new TrieMap[Char, String]
  substituteMap += (
    ' ' -> "+"
  )

  def formatAsURL(s: String): String = {
    val q = new scala.collection.mutable.Queue[Char]
    for (c <- s) q ++= substituteMap.getOrElse[String](c, c.toString)
    q.mkString
  }

  def main(args: Array[String]): Unit = {
    val ois: Try[ObjectInputStream] = Try(new ObjectInputStream(new FileInputStream(oldObjectsPath)))
    val readObjects: Option[HashSet[AuctionObject]] = ois match {
      case Success(stream) => Try(stream.readObject.asInstanceOf[HashSet[AuctionObject]]) match {
        case Success(set) => stream.close; Some(set)
        case Failure(_) => println("Error, malformed file"); System.exit(1); None //TODO: Handle error acceptably
      }
      case x: Failure[ObjectInputStream] => x.failed.get match {
        case e: FileNotFoundException => None
      }
    }
    val oldObjects: HashSet[AuctionObject] = readObjects match {
      case Some(s) => s
      case None => new HashSet[AuctionObject]
    }
    val searchTermSource = scala.io.Source.fromFile("search_terms.txt")
    val searchTerms: Iterator[String] = searchTermSource getLines
    val newResults = new HashSet[AuctionObject]
    for (sT <- searchTerms) newResults ++= new UnitsScraper().search(sT)
    for (r <- newResults) println(r.toString)
    searchTermSource.close
  }
}


class UnitsScraper(searchBaseURL: String = "http://www.units.se/auction/search/?search=") {

  def search(searchParams: String): Set[AuctionObject] = {
    val soup = Jsoup.connect(searchBaseURL + UnitsScraper2.formatAsURL(searchParams)).get
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
          .text
      )
    }

    objects
  }
}
