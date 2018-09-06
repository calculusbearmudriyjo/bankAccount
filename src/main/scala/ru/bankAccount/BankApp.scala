package ru.bankAccount

import com.twitter.finagle.Http
import com.twitter.logging.Logging
import com.twitter.server.TwitterServer
import com.twitter.util.{Await, Future}
import com.typesafe.config.ConfigFactory
import io.finch._
import io.finch.circe._
import io.finch.syntax._
import io.circe.generic.auto._
import ru.bankAccount.config.Config.ServerConfig
import ru.bankAccount.service.AccountService


object BankApp extends TwitterServer with Logging {
  private lazy val config = ConfigFactory.load()
  private lazy val serverConf = config.getConfig("ru.bankAccount.server")
  private lazy val accountService = AccountService()
  case class Response(text: String)

  val accountEndpoint: Endpoint[Response] =
    get("account") {
     Ok(Response(accountService.test()))
    }

  def main() {
    val server = ServerConfig(serverConf.getString("host"), serverConf.getInt("port"))
    val app = Http
      .server
      .withAdmissionControl.concurrencyLimit(
      maxConcurrentRequests = 400,
      maxWaiters = 100
    ).serve(s"${server.host}:${server.port}", accountEndpoint.toService)

    onExit{
      app.close()
    }
    Await.ready(app)
  }

}