/**
 * Copyright (C) 2015 Typesafe Inc. <http://www.typesafe.com>
 */

import java.sql.Date
import slick.driver.H2Driver.api._

object DataModel {

  case class User(id: Int, name: String)

  class Users(tag: Tag) extends Table[User](tag, "USERS") {
    def id = column[Int]("ID", O.PrimaryKey)
    def name = column[String]("NAME")
    def * = (id, name) <> (User.tupled, User.unapply)
  }
  lazy val users = TableQuery[Users]

  case class Order(id: Int, date: Date, userId: Int)

  class Orders(tag: Tag) extends Table[Order](tag, "ORDERS") {
    def id = column[Int]("ID", O.PrimaryKey)
    def date = column[Date]("DATE")
    def userID = column[Int]("USER_ID")
    def * = (id, date, userID) <> (Order.tupled, Order.unapply)
  }
  lazy val orders = TableQuery[Orders]

  case class DenormalizedOrder(id: Int, date: Date, userId: Int, userName: String, shipped: Boolean)

  class DenormalizedOrders(tag: Tag) extends Table[DenormalizedOrder](tag, "ORDERS") {
    def id = column[Int]("ID", O.PrimaryKey)
    def date = column[Date]("DATE")
    def userID = column[Int]("USER_ID")
    def userName = column[String]("USER_NAME")
    def shipped = column[Boolean]("SHIPPED")
    def * = (id, date, userID, userName, shipped) <> (DenormalizedOrder.tupled, DenormalizedOrder.unapply)
  }
  lazy val denormalizedOrders = TableQuery[DenormalizedOrders]

}
