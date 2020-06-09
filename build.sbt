
lazy val commonSettigs = Seq(
name := "news-aggregator",
scalaVersion := "2.12.11",
resourceDirectory in Compile := file("RSSList.txt") / "./src/main/resources",
resourceDirectory in Runtime := file("RSSList.txt") / "./src/main/resources"
)

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "1.0.0-RC20", 
  "com.softwaremill.sttp.client" %% "core" % "2.1.5",
  "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % "2.1.5",
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0")
scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:postfixOps"
)

assemblyJarName in assembly := "news-aggregator.jar"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case x => MergeStrategy.first
}

enablePlugins(DockerPlugin)

dockerfile in docker := {
  // The assembly task generates a fat JAR file
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("openjdk:8-jre")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")