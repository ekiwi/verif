name := "verif"

version := "0.0.1"

scalaVersion := "2.12.12"
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-language:reflectiveCalls")

fork in test := true
cancelable in Global := true

libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.3-SNAPSHOT"
libraryDependencies += "com.lihaoyi" %% "utest" % "0.7.2" % "test"

testFrameworks += new TestFramework("utest.runner.Framework")

//// Jmh Settings
//enablePlugins(JmhPlugin)
//// To have benchmark files within src/test instead of src/main
//sourceDirectory in Jmh := (sourceDirectory in Test).value
//classDirectory in Jmh := (classDirectory in Test).value
//dependencyClasspath in Jmh := (dependencyClasspath in Test).value
//// rewire tasks, so that 'jmh:run' automatically invokes 'jmh:compile' (otherwise a clean 'jmh:run' would fail)
//compile in Jmh := (compile in Jmh).dependsOn(compile in Test).value
//run in Jmh := (run in Jmh).dependsOn(Keys.compile in Jmh).evaluated

exportJars := true
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases"),
  Resolver.mavenLocal
)

