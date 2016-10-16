import akka.actor.{Actor, Cancellable, ActorRef, Props}
import UnitsScraper.{Scan, SearchResults}
import scala.collection.mutable.{HashMap, HashSet}
import scala.concurrent.duration._

//TODO: schedule checkup/removal at endtime. Future: bid-bot
class ScraperController extends Actor { //TODO: implement persistence in case of crash
  import ScraperController._ //case classes defined in companion object
  val searchCancelActorMap = new HashMap[SearchTerm, (ActorRef, Cancellable)]
  val oldCategorizedResults = new HashMap[SearchTerm, Set[AuctionObject]]
  val oldObjects = new HashSet[AuctionObject]
  val serverActor = context.actorOf(Props[WebServer], name = "Server")
  val pushbullet = context.actorOf(Props[PushAPI], name = "Pushbullet")
  val system = akka.actor.ActorSystem("system")

  def receive = {
    case SearchResults(result) => {
      val searchTerm = sender.path.name
      val newObjectsInCategory = result -- oldCategorizedResults.getOrElse(SearchTerm(searchTerm), new HashSet[AuctionObject])
      serverActor ! NewCategoryResults(searchTerm, newObjectsInCategory)
      if (Preferences.isPushbulletActive && !Preferences.NeedtoConfigure) {
        for (obj <- newObjectsInCategory) {
          pushbullet ! pushLink(s"""New auction object in search "$searchTerm": ${obj.title}""", obj.link)
        }
      }
      oldCategorizedResults(SearchTerm(searchTerm)) = result
    }
    case NewSearchTerm(s) => {
      val sT = SearchTerm(s)
      //creates an actor in this context, i.e. a child actor
      val actor = context.actorOf(Props[UnitsScraper], name = s)
      //sets the ExecutionContext for the message sending task
      import system.dispatcher
      //sets a schedule to scan the search page every 5 minutes, starting in 200 milliseconds
      val cancel: Cancellable = system.scheduler.schedule(200 milliseconds, 5 minutes, actor, Scan)(sender = context.self, executor = system.dispatcher)
      searchCancelActorMap += (sT -> ((actor, cancel)))
    }
    case DeleteSearchTerm(s) => {
      val sT = SearchTerm(s)
      val actorCancelTuple = searchCancelActorMap(sT)
      actorCancelTuple._2.cancel
      context stop actorCancelTuple._1
      searchCancelActorMap -= sT
    }
    case WebServer.Shutdown => context.system.terminate
  }
}

object ScraperController {
  case class SearchTerm(s: String)
  case class NewSearchTerm(s: String)
  case class DeleteSearchTerm(s: String)
  case class NewCategoryResults(searchTerm: String, objects: Set[AuctionObject])
}
