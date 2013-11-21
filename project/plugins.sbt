// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.1")

libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "5.1.24"
)

// Add scalikejdbc mapper generator
addSbtPlugin("com.github.seratch" %% "scalikejdbc-mapper-generator" % "[1.6, )")

// Add Typesafe Console plugin
addSbtPlugin("com.typesafe.sbt" % "sbt-atmos-play" % "0.3.2")
