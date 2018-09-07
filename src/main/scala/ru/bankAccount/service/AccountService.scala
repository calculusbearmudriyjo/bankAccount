package ru.bankAccount.service

import com.twitter.util.Future
import scala.concurrent.stm._

case class Account(name: String, amount: Long)

final case class AccountException(message: String) extends Exception
final case class TransferException(message: String) extends Exception
final case class FillAccountException(message: String) extends Exception

class AccountService {
  var accounts: Map[String, Ref[Account]] = Map()

  def getAccount(name: String): Future[Option[Account]] = Future{accounts.get(name).map(_.single.get)}
  def getAccountList(): Future[List[Account]] = Future{accounts.values.map(_.single.get).toList}

  def createAccount(name: String): Future[Either[Exception,Account]] = Future {
    if(accounts.get(name).isEmpty) {
      val newAccount = Ref(Account(name, 0L))
      accounts = accounts + (name -> newAccount)
      Right(newAccount.single())
    } else {
      Left(AccountException("account exist"))
    }
  }

  def transferAmount(from: String, to: String, amount: Long): Future[Either[Exception,Account]] = Future {
    accounts.get(from) match {
        case None => Left(TransferException(s"account from not found"))
        case Some(f) => accounts.get(to) match {
          case None => Left(TransferException(s"account to not found"))
          case Some(t) => {
            var res: Either[Exception,Account] = Left(TransferException("cannot transfer money from :" + f.single().name + ", to :" + t.single().name))
            atomic { implicit trx =>
              if(f().amount - amount >= 0) {
                f() = Account(f().name, f().amount - amount)
                t() = Account(t().name, t().amount + amount)
                res = Right(f())
              }
            }
            res
          }
      }
    }
  }

  def fillAccount(name: String, amount: Long): Future[Either[Exception,Account]] = Future {
    accounts.get(name) match {
      case None => Left(FillAccountException("fill account not found"))
      case Some(a) => {
        var res: Either[Exception,Account] = Left(TransferException("cannot fill account, trx rollback"))
        atomic {implicit trx =>
          a() = Account(a().name, a().amount + amount)
          res = Right(a())
        }
        res
      }
    }
  }
}

object AccountService {
  def apply(): AccountService = new AccountService()
}
