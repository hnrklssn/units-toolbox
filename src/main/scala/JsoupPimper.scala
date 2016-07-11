import scala.collection.JavaConverters._
import org.jsoup.select.Elements
import org.jsoup.nodes.Element

object mapper {
  implicit def createMappable[A](e: Elements) = new Object {
    def map(f: Element => A): Iterator[A] = e.iterator.asScala.map(f)
  }
}
