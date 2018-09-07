Project emulate bank account.
Technology: Finagle, Circe, Scala-stm, ScalaTest

> project build : sbt assembly

> project run : sbt run

> project test run: sbt test 

server start in port: 8099

config locate: src/main/resources/application.conf

Endpoints:
>GET - http://localhost:8099/account - show list account
 
>GET - http://localhost:8099/account/account-name - show account

>POST - http://localhost:8099/account - add account
 {"name": "account name"}

>POST - http://localhost:8099/account/account-name/fill - debit account
 {"amount": 1000}

>POST - http://localhost:8099/account/from-account-name/transfer - transfer account money
{
	"to": "test2",
	"amount": 10001
}