

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.io.StdIn

object WebServer {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val route = {
      get {
        //main page
        path("") {
          complete (
            HttpEntity(ContentTypes.`text/html(UTF-8)`, scala.io.Source.fromFile("src/main/html/index.html", "UTF-8").mkString)
          )
        } ~
        //css file
        path("style.css") {
          complete {
            HttpEntity(ContentType(MediaTypes.`text/css`, HttpCharsets.`UTF-8`), scala.io.Source.fromFile("src/main/css/style.css", "UTF-8").mkString)
          }
        } ~
        //background image file
        path("background.png") {
          complete {
            HttpEntity(MediaTypes.`image/png`, scala.io.Source.fromFile("src/main/resources/background.png", "ISO8859-1").map(_.toByte).toArray) //turn image file into byte array
          }
        }

      }

    }
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
