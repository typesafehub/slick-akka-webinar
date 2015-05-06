/**
 * Copyright (C) 2015 Typesafe Inc. <http://www.typesafe.com>
 */

import java.sql.Date
import DataModel._
import slick.driver.H2Driver.api._
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.scaladsl.FlowGraph.Implicits._
import akka.actor.ActorSystem

object EtlDemo extends App {
  val sys = ActorSystem("EtlDemo")
  implicit val mat = ActorFlowMaterializer()(sys)
  import sys.dispatcher

  val db1 = Database.forConfig("usersDB")
  val db2 = Database.forConfig("shippedOrdersDB")
  val db3 = Database.forConfig("openOrdersDB")
  val db4 = Database.forConfig("reportingDB")

  val pUsers = db1.stream(users.result)
  
  def pShippedOrdersByUserId(id: Int) =
    db2.stream(orders.filter(_.userId === id).result)

  def pOpenOrdersByUserId(id: Int) =
    db3.stream(orders.filter(_.userId === id).result)

  Await.result(db4.run(denormalizedOrders.delete), 3.seconds)
  sys.log.info("denormalized orders deleted")

  def denormalize(user: User, order: Order, shipped: Boolean) =
    DenormalizedOrder(order.id, order.date.toString, user.id, user.name, shipped)

  val future =
    Source(pUsers)
      .map { user =>
        val shipped = Source(pShippedOrdersByUserId(user.id))
          .map(order => denormalize(user, order, true))
          
        val notShipped = Source(pOpenOrdersByUserId(user.id))
          .map(order => denormalize(user, order, false))

        Source() { implicit b =>
          val merge = b.add(Merge[DenormalizedOrder](2))
          shipped ~> merge
          notShipped ~> merge
          merge.out
        }
      }
      .flatten(FlattenStrategy.concat)
      .grouped(1000)
      // parallelism tunable here as well
      .mapAsync(4)(g => db4.run(denormalizedOrders ++= g))
      .runForeach(r => sys.log.info("wrote {}", r))

  Await.result(future, 10.seconds)
  sys.log.info("done")

  sys.shutdown()

  //Finally: Make sure to close all DBs
  db1.close()
  db2.close()
  db3.close()
  db4.close()
}
