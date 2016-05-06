// PROJECT PROPERTIES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
organization in ThisBuild := "org.mbari.vars"

name := "video-asset-manager"

version in ThisBuild := "1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.8"

//crossScalaVersions := Seq("2.11.8", "2.10.5", "2.11.6")

// https://tpolecat.github.io/2014/04/11/scalac-flags.html
scalacOptions in ThisBuild ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",       // yes, this is 2 args
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture")

javacOptions in ThisBuild ++= Seq("-target", "1.8", "-source","1.8")

incOptions := incOptions.value.withNameHashing(true)

// DEPENDENCIES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

updateOptions := updateOptions.value.withCachedResolution(true)

// Add SLF4J, Logback and testing libs
libraryDependencies ++= {
  val slf4jVersion = "1.7.21"
  val logbackVersion = "1.1.7"
  val derbyVersion = "10.12.1.1"
  Seq(
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    "ch.qos.logback" % "logback-core" % logbackVersion,
    "com.typesafe" % "config" % "1.3.0",
    "javax.transaction" % "jta" % "1.1",
    "junit" % "junit" % "4.12" % "test",
    "net.sourceforge.jtds" % "jtds" % "1.3.1",
    "org.apache.derby" % "derby" % derbyVersion % "test",
    "org.apache.derby" % "derbyclient" % derbyVersion % "test",
    "org.apache.derby" % "derbynet" % derbyVersion % "test",
    "org.eclipse.persistence" % "eclipselink" % "2.6.2",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test",
    "org.slf4j" % "log4j-over-slf4j" % slf4jVersion,
    "org.slf4j" % "slf4j-api" % slf4jVersion)
}

resolvers in ThisBuild ++= Seq(Resolver.mavenLocal,
    "hohonuuli-bintray" at "http://dl.bintray.com/hohonuuli/maven")

//publishMavenStyle := true

//publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

// OTHER SETTINGS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

// -- PROMPT
// set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state =>
  val user = System.getProperty("user.name")
  user + "@" + Project.extract(state).currentRef.project + ":sbt> "
}


// -- VERSIONREPORT
// Add this setting to your project to generate a version report (See ExtendedBuild.scala too.)
// Use as 'sbt versionReport' or 'sbt version-report'
versionReport <<= (externalDependencyClasspath in Compile, streams) map {
  (cp: Seq[Attributed[File]], streams) =>
    val report = cp.map {
      attributed =>
        attributed.get(Keys.moduleID.key) match {
          case Some(moduleId) => "%40s %20s %10s %10s".format(
            moduleId.organization,
            moduleId.name,
            moduleId.revision,
            moduleId.configurations.getOrElse("")
          )
          case None =>
            // unmanaged JAR, just
            attributed.data.getAbsolutePath
        }
    }.sortBy(a => a.trim.split("\\s+").map(_.toUpperCase).take(2).last).mkString("\n")
    streams.log.info(report)
    report
}


// -- VERSION.PROPERTIES
// Code for adding a version.propertes file
gitHeadCommitSha := scala.util.Try(Process("git rev-parse HEAD").lines.head).getOrElse("")

makeVersionProperties := {
  val propFile = (resourceManaged in Compile).value / "version.properties"
  val content = "version=%s" format (gitHeadCommitSha.value)
  IO.write(propFile, content)
  Seq(propFile)
}

resourceGenerators in Compile <+= makeVersionProperties


// -- SBT-PACK
// For sbt-pack
packAutoSettings

// For sbt-pack
val apps = Seq("main")

packAutoSettings ++ Seq(packExtraClasspath := apps.map(_ -> Seq("${PROG_HOME}/conf")).toMap,
  packJvmOpts := apps.map(_ -> Seq("-Duser.timezone=UTC", "-Xmx4g")).toMap,
  packDuplicateJarStrategy := "latest",
  packJarNameConvention := "original")


// -- SCALARIFORM
// Format code on save with scalariform
import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform

SbtScalariform.scalariformSettings

SbtScalariform.ScalariformKeys.preferences := SbtScalariform.ScalariformKeys.preferences.value
  .setPreference(IndentSpaces, 2)
  .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, false)
  .setPreference(DoubleIndentClassDeclaration, true)

// Fail if style is bad
scalastyleFailOnError := true

// -- TODOS
// default tags are TODO, FIXME, WIP and XXX. I want the following instead
todosTags := Set("TODO", "FIXME", "WTF?")

// -- MISC
// fork a new JVM for run and test:run
fork := true

// Aliases
addCommandAlias("cleanall", ";clean;clean-files")

initialCommands in console :=
  """
    |import java.util.Date
  """.stripMargin
