package cool.graph.webhook

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import cool.graph.client.ClientInjector
import cool.graph.cuid.Cuid

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait WebhookCaller {
  def call(url: String, payload: String): Future[Boolean]
}

class WebhookCallerMock extends WebhookCaller {
  private val _calls = scala.collection.parallel.mutable.ParTrieMap[String, (String, String)]()

  def calls: List[(String, String)] = _calls.values.toList

  var nextCallShouldFail = false

  def clearCalls: Unit = _calls.clear

  override def call(url: String, payload: String): Future[Boolean] = {
    _calls.put(Cuid.createCuid(), (url, payload))

    Future.successful(!nextCallShouldFail)
  }
}

class WebhookCallerImplementation(implicit injector: ClientInjector) extends WebhookCaller {

  override def call(url: String, payload: String): Future[Boolean] = {

    implicit val system: ActorSystem             = injector.system
    implicit val materializer: ActorMaterializer = injector.materializer

    println("calling " + url)

    Http()
      .singleRequest(HttpRequest(uri = url, method = HttpMethods.POST, entity = HttpEntity(contentType = ContentTypes.`application/json`, string = payload)))
      .map(x => {
        x.status.isSuccess()
      })
  }
}
