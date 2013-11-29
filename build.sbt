name := "play"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "mysql" % "mysql-connector-java" % "5.1.24",
  "com.github.seratch" %% "scalikejdbc"               % "[1.6, )",
  "com.github.seratch" %% "scalikejdbc-interpolation" % "[1.6, )",
  "org.scalikejdbc"     %% "scalikejdbc-async"             % "[0.3,)",
  "org.scalikejdbc"     %% "scalikejdbc-async-play-plugin" % "[0.3,)",
  "com.github.mauricio" %% "mysql-async"              % "0.2.8",
  "eu.henkelmann" % "actuarius_2.10.0" % "0.2.6"
)

play.Project.playScalaSettings

scalikejdbcSettings

atmosPlaySettings
