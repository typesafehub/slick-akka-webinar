/**
 * Copyright (C) 2015 Typesafe Inc. <http://www.typesafe.com>
 */

import scala.concurrent.duration._

import org.reactivestreams.Publisher

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import akka.stream.stage.{ Context, PushPullStage }
import akka.util.{ ByteString, Timeout }
import spray.json._

object HttpDemo extends App {
  implicit val sys = ActorSystem("TheDemo")
  implicit val mat = ActorFlowMaterializer()
  implicit val timeout = Timeout(3.seconds)
  import sys.dispatcher

  case class Item(id: Int, name: String)
  object Item extends DefaultJsonProtocol {
    implicit val format = jsonFormat2(apply)
  }

  class ToJsonArray[T](implicit f: RootJsonFormat[T])
      extends PushPullStage[T, ByteString] {
    private var first = true
    override def onPush(elem: T, ctx: Context[ByteString]) = {
      val leading = if (first) { first = false; "[" } else ","
      ctx.push(ByteString(leading + elem.toJson.compactPrint))
    }
    override def onPull(ctx: Context[ByteString]) =
      if (ctx.isFinishing) ctx.pushAndFinish(ByteString("]"))
      else ctx.pull()
    override def onUpstreamFinish(ctx: Context[ByteString]) =
      ctx.absorbTermination()
  }

  private def getFromDb(id: Int): Publisher[Item] =
    Source(List(Item(1, "one"), Item(2, "two"), Item(3, "three")))
      .runWith(Sink.publisher)

  Http().bindAndHandle(
    path("items" / IntNumber) { id =>
      val pub = Source(getFromDb(id)).transform(() => new ToJsonArray)
      complete(HttpEntity.Chunked.fromData(`application/json`, pub))
    },
    "localhost", 8080)
}
