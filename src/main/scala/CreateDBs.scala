/**
 * Copyright (C) 2015 Typesafe Inc. <http://www.typesafe.com>
 */

import java.sql.Date

import slick.driver.H2Driver.api._
import DataModel._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object CreateDBs extends App {
  val numUsers = 10
  val numOrders = 10000

  val db1 = Database.forConfig("usersDB")
  val db2 = Database.forConfig("shippedOrdersDB")
  val db3 = Database.forConfig("openOrdersDB")
  val db4 = Database.forConfig("reportingDB")

  val a1 = users.schema.create >> (users ++= ((1 to numUsers).map(i => User(i, "u"+i))))
  val a2 = orders.schema.create >> (orders ++= (
    for {
      u <- 1 to numUsers
      o <- 1 to numOrders if o % 2 == 0
    } yield Order(u + (o * numUsers), new Date(System.currentTimeMillis()), u)))
  val a3 = orders.schema.create >> (orders ++= (
    for {
      u <- 1 to numUsers
      o <- 1 to numOrders if o % 2 == 1
    } yield Order(u + (o * numUsers), new Date(System.currentTimeMillis()), u)))
  val a4 = denormalizedOrders.schema.create >> (denormalizedOrders ++= (
    for {
      u <- 1 to numUsers
      o <- 1 to numOrders if o % 2 == 0
    } yield DenormalizedOrder(u + (o * numUsers), (new Date(System.currentTimeMillis())).toString, u, "u"+u, o % 2 == 0)))

  try {
    Await.result(db1.run(a1), Duration.Inf)
    Await.result(db2.run(a2), Duration.Inf)
    Await.result(db3.run(a3), Duration.Inf)
    //Await.result(db4.run(a4), Duration.Inf)
    Await.result(db4.run(denormalizedOrders.schema.create), Duration.Inf)
  } finally {
    db1.close()
    db2.close()
    db3.close()
    db4.close()
  }
}
