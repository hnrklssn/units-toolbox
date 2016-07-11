import org.joda.time.DateTime
import java.io.Serializable

@SerialVersionUID(314L)
case class AuctionObject(id: Int, link: String, title: String, endtime: DateTime, bid: Int, bidder: String) extends Serializable {

  override def hashCode(): Int = id
  override def equals(other: Any) =
    other match {
      case that: AuctionObject => this.hashCode == that.hashCode
      case _ => false
    }
}
