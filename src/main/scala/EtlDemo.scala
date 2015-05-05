/**
 * Copyright (C) 2015 Typesafe Inc. <http://www.typesafe.com>
 */

import java.sql.Date

import DataModel._
import slick.driver.H2Driver.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object EtlDemo extends App {
  val db1 = Database.forConfig("usersDB")
  val db2 = Database.forConfig("shippedOrdersDB")
  val db3 = Database.forConfig("openOrdersDB")
  val db4 = Database.forConfig("reportingDB")

  val pUsers = db1.stream(users.result)
  def pShippedOrdersByUserId(id: Int) = db2.stream(orders.filter(_.userID === id).result)
  def pOpenOrdersByUserId(id: Int) = db3.stream(orders.filter(_.userID === id).result)

  //TODO: Iterate over all users, get their shipped and open orders, create a DenormalizedOrder for each record,
  //group them in groups of 1000, then call db4.run(denormalizedOrders ++= g) for each group and wait until the
  //Future completes.

  //Finally: Make sure to close all DBs
  db1.close()
  db2.close()
  db3.close()
  db4.close()
}
