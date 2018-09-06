package ru.bankAccount.service

import com.twitter.util.Future

import scala.concurrent.stm._

case class Account(name: String, amount: Long)

class AccountService {
  val accounts: Map[String, Ref[Account]] = Map()

  def createAccount(name: String): Future[Account] = Future {
    val newAccount = Ref(Account(name, 0L))
    accounts + (name -> newAccount)
    newAccount.single()
  }
  
//  def

  def test(): String = "Hello, world"
}

object AccountService {
  def apply(): AccountService = new AccountService()
}
