name := "bankAccount"

version := "0.1"

scalaVersion := "2.12.6"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "com.github.finagle" %% "finch-core" % "0.21.0",
  "com.github.finagle" %% "finch-circe" % "0.21.0",
  "io.circe" %% "circe-generic" % "0.9.3",
  "io.circe" %% "circe-core" % "0.9.3",
  "io.circe" %% "circe-parser" % "0.9.3",
  "com.twitter" %% "util-collection" % "18.6.0",
  "com.twitter" %% "twitter-server" % "18.6.0",
  "ch.qos.logback"  %  "logback-classic"     % "1.2.3",
  "com.typesafe" % "config" % "1.3.2",
  "org.scala-stm" %% "scala-stm" % "0.8",
)

lazy val app = (project in file("app")).
  settings(
    mainClass in assembly := Some("com.imbetgames.slot.Main"),
    // more settings here ...
  )

val meta = """META.INF(.)*""".r

assemblyMergeStrategy in assembly := {
  case "BUILD" => MergeStrategy.discard
  case meta(_)  => MergeStrategy.discard // or MergeStrategy.discard, your choice
  case other => MergeStrategy.defaultMergeStrategy(other)
}
