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
import slick.driver.H2Driver.api._
import DataModel._

object HttpDemo extends App {
  implicit val sys = ActorSystem("TheDemo")
  implicit val mat = ActorFlowMaterializer()
  implicit val timeout = Timeout(3.seconds)
  import sys.dispatcher

  import DefaultJsonProtocol._
  implicit val denormalizedOrderFormat = jsonFormat5(DenormalizedOrder.apply)

  class ToJsonArray[T](implicit f: RootJsonFormat[T])
      extends PushPullStage[T, ByteString] {
    private var first = true

    override def onPush(elem: T, ctx: Context[ByteString]) = {
      val leading = if (first) { first = false; "[" } else ","
      ctx.push(ByteString(leading + elem.toJson.compactPrint + "\n"))
    }

    override def onPull(ctx: Context[ByteString]) =
      if (ctx.isFinishing) {
        if (first) ctx.pushAndFinish(ByteString("[]"))
        else ctx.pushAndFinish(ByteString("]"))
      } else ctx.pull()

    override def onUpstreamFinish(ctx: Context[ByteString]) =
      ctx.absorbTermination()
  }

  val db = Database.forConfig("reportingDB")

  private def getFromDb(userId: Int): Publisher[DenormalizedOrder] =
    db.stream(denormalizedOrders.filter(_.userId === userId).result)

  Http().bindAndHandle(
    (get & path("orders" / IntNumber)) { userId =>
      val pub =
        Source(getFromDb(userId))
          .transform(() => new ToJsonArray)
      complete(HttpEntity.Chunked.fromData(`application/json`, pub))
    },
    "localhost", 8080)

  println("listening")
}
