package ru.bankAccount.service

import org.scalatest.{BeforeAndAfter, FunSuite}
import com.twitter.util.{Return, Throw, Future => TwitterFuture}
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}

class AccountServiceSuite extends FunSuite with BeforeAndAfter{
  implicit val ec = ExecutionContext.global
  var service = AccountService()
  val accountName = "account1"
  val accountName2 = "account2"

  def fromTwitter[A](twitterFuture: TwitterFuture[A]): Future[A] = {
    val promise = Promise[A]()
    twitterFuture respond {
      case Return(a) => promise success a
      case Throw(e) => promise failure e
    }
    promise.future
  }

  test("check create account") {
    val listAcc = Await.result(fromTwitter(service.getAccountList()), 2 seconds)
    assert(listAcc.isEmpty)

    service.createAccount(accountName)
    val res = Await.result(fromTwitter(service.getAccount(accountName)), 2 seconds).get
    assert(res.name === accountName)
    assert(res.amount === 0)
  }

  test("check exception create account if it exist") {
    service.createAccount(accountName)
    val res = Await.result(fromTwitter(service.createAccount(accountName)), 2 seconds)
    res match {
      case Left(e: AccountException) => assert(true)
      case _ => fail()
    }
  }

  test("check list account") {
    service.createAccount(accountName)
    service.createAccount(accountName2)
    val list = Await.result(fromTwitter(service.getAccountList()), 2 seconds)
    assert(list.size === 2)

    assert(list.head.name === accountName)
    assert(list(1).name === accountName2)
  }

  test("debit account") {
    service.createAccount(accountName)
    service.fillAccount(accountName, 1000)
    val res = Await.result(fromTwitter(service.getAccount(accountName)), 2 seconds).get
    assert(res.name === accountName)
    assert(res.amount === 1000)
  }

  test("debit account that not exist") {
    Await.result(fromTwitter(service.fillAccount(accountName, 1000)), 2 seconds) match {
      case Left(e: FillAccountException) => assert(true)
      case _ => fail()
    }
  }

  test("transfer money between account") {
    service.createAccount(accountName)
    service.createAccount(accountName2)
    service.fillAccount(accountName, 1000)
    val acc1BeforeTransfer = Await.result(fromTwitter(service.getAccount(accountName)), 2 seconds).get
    val acc2BeforeTransfer = Await.result(fromTwitter(service.getAccount(accountName2)), 2 seconds).get

    assert(acc1BeforeTransfer.amount === 1000)
    assert(acc2BeforeTransfer.amount === 0)

    service.transferAmount(accountName, accountName2, 1000)

    val acc1AfterTransfer = Await.result(fromTwitter(service.getAccount(accountName)), 2 seconds).get
    val acc2AfterTransfer = Await.result(fromTwitter(service.getAccount(accountName2)), 2 seconds).get

    assert(acc1AfterTransfer.amount === 0)
    assert(acc2AfterTransfer.amount === 1000)
  }

  test("transfer money between account that not exist From") {
    service.createAccount(accountName)
    service.fillAccount(accountName, 1000)

    Await.result(fromTwitter(service.transferAmount(accountName, accountName2, 1000)), 2 seconds) match {
      case Left(e: TransferException) => assert(true)
      case _ => fail()
    }
  }

  test("transfer money between account that not exist To") {
    service.createAccount(accountName)
    service.fillAccount(accountName, 1000)

    Await.result(fromTwitter(service.transferAmount(accountName2, accountName, 1000)), 2 seconds) match {
      case Left(e: TransferException) => assert(true)
      case _ => fail()
    }
  }

  test("transfer not enough money between account") {
    service.createAccount(accountName)
    service.createAccount(accountName2)
    service.fillAccount(accountName, 1000)
    val acc1BeforeTransfer = Await.result(fromTwitter(service.getAccount(accountName)), 2 seconds).get
    val acc2BeforeTransfer = Await.result(fromTwitter(service.getAccount(accountName2)), 2 seconds).get

    assert(acc1BeforeTransfer.amount === 1000)
    assert(acc2BeforeTransfer.amount === 0)

    Await.result(fromTwitter(service.transferAmount(accountName2, accountName, 2000)), 2 seconds) match {
      case Left(e: TransferException) => assert(true)
      case _ => fail()
    }
  }

  test("Stress test transfer money") {
    service.createAccount(accountName)
    service.createAccount(accountName2)

    service.fillAccount(accountName, 1000)
    service.fillAccount(accountName2, 1000)

    val listTrx1 = (0 to 1000).map(x => Future{service.transferAmount(accountName, accountName2, 1)})
    val listTrx2 = (0 to 1000).map(x => Future{service.transferAmount(accountName2, accountName, 1)})
    val totalTrx = listTrx1 ++ listTrx2

    Await.result(Future.sequence(totalTrx), 10 seconds)

    val acc1 = Await.result(fromTwitter(service.getAccount(accountName)), 2 seconds).get
    val acc2 = Await.result(fromTwitter(service.getAccount(accountName2)), 2 seconds).get

    assert(acc1.amount + acc2.amount === 2000)
  }



  before {
    service = AccountService()
  }
}
