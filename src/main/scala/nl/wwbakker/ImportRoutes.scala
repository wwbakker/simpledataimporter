package nl.wwbakker

import java.net.URI

import akka.actor.ActorSystem
import akka.event.Logging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
//import akka.http.scaladsl.server.Route
//import akka.http.scaladsl.server.Directives._


//TODO: Change to ErrorAccumulatingCirceSupport
trait ImportRoutes extends FailFastCirceSupport {

  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[ImportRoutes])

//  lazy val importRoutes: Route =
//    pathPrefix("import") {
//      path(Segment) { handlerName =>
//        post (
//          entity(as[DataImportParameters]){ importParameters =>
//
//          }
//        )
//      }
//    }
}


case class DataImportParameters(url: URI,
                                deleteOldData: Boolean,
                                gzipped: Boolean,
                                bulkInsert: Boolean)