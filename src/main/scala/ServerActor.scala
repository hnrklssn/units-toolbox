

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.io.StdIn

import akka.actor.{Actor, Props}
import scala.collection.mutable.{HashMap, ListBuffer}
import scala.collection.immutable.HashSet
//java version of StringBuilder used to avoid unnecessary conversion when using formatter.printTo
import java.lang.StringBuilder
import org.joda.time.format.DateTimeFormat
import ScraperController._
import scala.concurrent.Future

//TODO: config page, text indicating last update of auction objects
class WebServer extends Actor { //TODO: implement persistence
  import WebServer._ //companion object
  val categoryResultsMap = new HashMap[String, Set[AuctionObject]]
  implicit val system = ActorSystem("system")
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val splitString = """<!--split-->"""
  var objectsPage: String = null
  lazy val objectsSource = scala.io.Source.fromFile("src/main/html/objects.html", "UTF-8").mkString
  lazy val objectsHeaderFooter = objectsSource.split(splitString)
  var objectChange: Boolean = true

  var configPage: String = null
  lazy val configSource = scala.io.Source.fromFile("src/main/html/config.html", "UTF-8").mkString

  lazy val menuInserts = scala.io.Source.fromFile("src/main/html/menu-snippets.html", "UTF-8").mkString
                          .split(splitString)

  lazy val cssDoc = scala.io.Source.fromFile("src/main/css/style.css", "UTF-8").mkString
  lazy val menuCssDoc = scala.io.Source.fromFile("src/main/css/menu.css", "UTF-8").mkString
  lazy val bg = scala.io.Source.fromFile("src/main/resources/background.png", "ISO8859-1").map(_.toByte).toArray //turn image file into byte array
  lazy val logo = scala.io.Source.fromFile("src/main/resources/u-tb_logo.png", "ISO8859-1").map(_.toByte).toArray
  lazy val menubg = scala.io.Source.fromFile("src/main/resources/menubg.png", "ISO8859-1").map(_.toByte).toArray

  val route = {
    get {
      //main page
      path("") {
        complete (
          HttpEntity(ContentTypes.`text/html(UTF-8)`, objectsPage) //objects page for now
        )
      } ~
      //objects page
      path("objects") {
        complete (
          HttpEntity(ContentTypes.`text/html(UTF-8)`, objectsPage)
        )
      } ~
      //main css file
      path("style.css") {
        complete {
          HttpEntity(ContentType(MediaTypes.`text/css`, HttpCharsets.`UTF-8`), cssDoc)
        }
      } ~
      //background image file
      path("background.png") {
        complete {
          HttpEntity(MediaTypes.`image/png`, bg)
        }
      } ~
      //menu css file
      path("menu.css") {
        complete {
          HttpEntity(ContentType(MediaTypes.`text/css`, HttpCharsets.`UTF-8`), menuCssDoc)
        }
      } ~
      //logo image, part of menu-bar
      path("u-tb_logo.png") {
        complete {
          HttpEntity(MediaTypes.`image/png`, logo)
        }
      } ~
      //background image of the menu-bar
      path("menubg.png") {
        complete {
          HttpEntity(MediaTypes.`image/png`, menubg)
        }
      }

    } ~
    path("config") {
      get {
        if (configPage == null) configPage = compileConfigPage()
        complete (
          HttpEntity(ContentTypes.`text/html(UTF-8)`, configPage)
        )
      } ~
      post {
        formFields("pbOnSwitch".as[Boolean], "pbKey", "pbChannel") { (pbOn, pbKey, pbChannel) =>
          //val response = updateConfig(pbKey, pbChannel)
          Preferences.activatePushbullet(pbOn)
          if (pbOn) {
            Preferences.setAPI_KEY(pbKey)
            Preferences.setPBChannel(pbChannel)
          }
          configPage = compileConfigPage()
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, configPage))
        } ~
        formFields("pbOff".as[Boolean]) { (pbOff) =>
          Preferences.activatePushbullet(false)
          configPage = compileConfigPage()
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, configPage))
        }
      }
    }

  }
  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
  //val blocking = context.actorOf(Props[ShutdownActor].withDispatcher("blocking-io-dispatcher"), name = "blocking")

  def receive = {
    case NewCategoryResults(searchTerm, newObjectsInCategory) => {
      categoryResultsMap(searchTerm) =
        newObjectsInCategory ++ categoryResultsMap.getOrElse(searchTerm, default = new HashSet[AuctionObject]())
      objectChange = true
    }
    case Recompile => if(objectChange) {objectsPage = compileObjectsPage; objectChange = false}
    case Shutdown => {
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => context.parent ! Shutdown) // and shutdown when done
    }
  }

  def compileObjectsPage(): String = {
    val categories = categoryResultsMap.keys.toVector.sorted
    val sb = new StringBuilder(objectsHeaderFooter(0))
    for (cat <- categories) {
      sb.append("""
        <div class="category"> <div class="cat_bg"></div>
          <div class="padder">
            <div class="title h1">""")
      sb.append(cat)
      sb.append("""</div>""")
      val objects = categoryResultsMap(cat).toVector.sorted(new scala.math.Ordering[AuctionObject] {
        override def compare(x: AuctionObject, y: AuctionObject) = x.endtime compareTo y.endtime
      })
      for (obj <- objects) {
        sb.append("""
            <div class="object">
              <div class="title"><a href='""")
        sb.append(obj.link)
        sb.append("'>")
        sb.append(obj.title)
        sb.append("""</a></div>
              <div class="info">
                <div class="image">
                  <a href='""")
        sb.append(obj.link)
        sb.append("'><img src='")
        sb.append(obj.imageLink)
        sb.append("""' alt='' /></a>
                </div>
                <div class="data">
                  <div class="row endtime">
                    <div class="heading">Auktionen avslutas:</div>
                    <span class="info endtime">""")
        val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm")
        formatter.printTo(sb, obj.endtime)
        sb.append("""</span>
                    </div>
                    <div class="row market-value">
                      <div class="heading">Marknadsvärde:</div>
                      <span class="info market-value">""")
        sb.append(obj.value.toString)
        sb.append(""" kr exkl moms</span>
                    </div>
                    <div class="row bid-info">
                      <div class="heading">Högsta Bud:</div>
                      <span class="info highest-bid">""")
        sb.append(obj.bid.toString)
        sb.append(""" kr exkl moms</span>
                      <br>
                      <div class="heading">Budgivare:</div>
                      <span class="info bidder">""")
        sb.append(obj.bidder)
        sb.append("""</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>""")
      }
      sb.append("""
          </div>
        </div>""")
    }
    sb.append(objectsHeaderFooter(1))
    insertMenu(sb)
    sb.toString
  }

  def compileConfigPage(): String = {
    val pbState = Preferences.isPushbulletActive
    val pbKey = Preferences.API_KEY_Option match {
      case Some(s) => s
      case None => ""
    }
    val pbChannel = Preferences.getPBChannel_Option match {
      case Some(s) => s
      case None => ""
    }

    val page = configSource.replaceAll("""\$pbState""", valueOrEmptyString(pbState, "checked"))
    .replaceAll("""\$pbInputDisabled""", valueOrEmptyString(!pbState, "disabled"))
    .replaceAll("""\$pbKey""", pbKey)
    .replaceAll("""\$channelName""", pbChannel)
    
    insertMenu(page)
  }

  def valueOrEmptyString(value: Boolean, trueString: String): String = if (value) {
    trueString
  } else {
    ""
  }
  
  def insertMenu(html: String): String = {
    val sb = new StringBuilder(html)
    insertMenu(sb)
    sb.toString
  }
  
  def insertMenu(html: StringBuilder): Unit = {
    val titleEndString = """</title>"""
    val titleEndIndex = html.indexOf(titleEndString) + titleEndString.length
    html.insert(titleEndIndex + 1, menuInserts(0))
    val bodyStartString = """<body>"""
    val bodyStartIndex = html.indexOf(bodyStartString) + bodyStartString.length
    html.insert(bodyStartIndex + 1, menuInserts(2))
    html.insert(bodyStartIndex - 1, menuInserts(1)) //needs to be after prev. line
                                                    //since it moves the index
  }


}

object WebServer {
  case object Recompile
  case object Shutdown
  case object Started
}

//will probably be replaced by a Main-class in the future
class ShutdownActor extends Actor {
  import WebServer._
  def receive = {
    case Started => {
      println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
      StdIn.readLine() // let it run until user presses return
      context.parent ! Shutdown // and shutdown when done
    }
  }
}
