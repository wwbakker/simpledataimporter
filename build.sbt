lazy val akkaHttpVersion = "10.1.3"
lazy val akkaVersion    = "2.5.14"
val circeVersion = "0.9.3"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "nl.wwbakker",
      scalaVersion    := "2.12.6"
    )),
    name := "SimpleDataImporter",
    resolvers += Resolver.bintrayRepo("hseeberger", "maven"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
      "de.heikoseeberger" %% "akka-http-circe"      % "1.21.0",

      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.5"         % Test
    ) ++ Seq(
      "io.circe"          %% "circe-core",
      "io.circe"          %% "circe-generic",
      "io.circe"          %% "circe-parser"
    ).map(_ % circeVersion)

  )
