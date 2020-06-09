name := "news-aggregator" 

scalaVersion := "2.12.11"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "1.0.0-RC20", 
  "com.softwaremill.sttp.client" %% "core" % "2.1.5",
  "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % "2.1.5",
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0"
)
scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:postfixOps"
)

assemblyJarName in assembly := "news-aggregator.jar"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _ => MergeStrategy.filterDistinctLines
}

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")