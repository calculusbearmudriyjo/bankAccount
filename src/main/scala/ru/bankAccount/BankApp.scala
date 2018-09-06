package ru.bankAccount

import com.twitter.finagle.Http
import com.twitter.logging.Logging
import com.twitter.server.TwitterServer
import com.twitter.util.Await
import com.typesafe.config.ConfigFactory
import io.finch._
import io.finch.circe._
import io.finch.syntax._
import io.circe.generic.auto._
import ru.bankAccount.config.Config.ServerConfig
import ru.bankAccount.service.{Account, AccountService}


object BankApp extends TwitterServer with Logging {
  private lazy val config = ConfigFactory.load()
  private lazy val serverConf = config.getConfig("ru.bankAccount.server")
  private lazy val accountService = AccountService()

  case class addAccountRequest(name: String) extends AnyVal
  case class fillAccountRequest(amount: Long)
  case class TransferRequest(to: String, amount: Long)

  val listAccountEndpoint: Endpoint[List[Account]] =
    get("account") {
      for {
        r <- accountService.getAccount()
      } yield Ok(r)
    }

  val getAccountEndpoint: Endpoint[Option[Account]] =
    get("account" :: path[String]) { req: String =>
      for {
        r <- accountService.getAccount(req)
      } yield Ok(r)
    }

  val addAccountEndpoint: Endpoint[Account] =
    post("account" :: jsonBody[addAccountRequest]) { req: addAccountRequest =>
      for {
        r <- accountService.createAccount(req.name)
      } yield r match {
        case Right(acc) => Ok(acc)
        case Left(msg) => BadRequest(msg)
      }
    }

  val fillAccountEndpoint: Endpoint[Account] =
    post("account" :: path[String] :: "fill" :: jsonBody[fillAccountRequest]) { (name: String, req: fillAccountRequest) =>
      for {
        r <- accountService.fillAccount(name, req.amount)
      } yield r match {
        case Right(acc) => Ok(acc)
        case Left(msg) => BadRequest(msg)
      }
    }

  val transferAccountEndpoint: Endpoint[Account] =
    post("account" ::path[String] :: "transfer" :: jsonBody[TransferRequest]) { (from:String, req: TransferRequest) =>
      for {
        r <- accountService.transferAmount(from, req.to, req.amount)
      } yield r match {
        case Right(acc) => Ok(acc)
        case Left(msg) => BadRequest(msg)
      }
    }

  def main() {
    val server = ServerConfig(serverConf.getString("host"), serverConf.getInt("port"))
    val app = Http
      .server
      .withAdmissionControl.concurrencyLimit(
      maxConcurrentRequests = 400,
      maxWaiters = 100
    ).serve(s"${server.host}:${server.port}",
      (listAccountEndpoint :+:
      getAccountEndpoint :+:
      addAccountEndpoint :+:
      fillAccountEndpoint :+:
      transferAccountEndpoint).toService)

    onExit{
      app.close()
    }
    Await.ready(app)
  }

}