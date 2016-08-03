// PROJECT PROPERTIES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
organization in ThisBuild := "org.mbari.vars"

name := "vampire-squid"

version in ThisBuild := "1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.8"

mainClass in Compile := Some("JettyMain")

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
  "-Xfuture")

javacOptions in ThisBuild ++= Seq("-target", "1.8", "-source","1.8")

incOptions := incOptions.value.withNameHashing(true)

// DEPENDENCIES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

updateOptions := updateOptions.value.withCachedResolution(true)

// Add SLF4J, Logback and testing libs
libraryDependencies ++= {
  val akkaVersion = "2.4.7"
  val configVersion = "1.3.0"
  val derbyVersion = "10.12.1.1"
  val eclipselinkVersion = "2.6.3"
  val gsonJavatimeVersion = "1.1.1"
  val gsonVersion = "2.7"
  val h2Version = "1.4.192"
  val jettyVersion = "9.3.8.v20160314"
  val jtaVersion = "1.1"
  val jtdsVersion = "1.3.1"
  val junitVersion = "4.12"
  val logbackVersion = "1.1.7"
  val scalaTestVersion = "2.2.6"
  val scalatraVersion = "2.4.1"
  val slf4jVersion = "1.7.21"
  Seq(
    "ch.qos.logback"           % "logback-classic"                % logbackVersion,
    "ch.qos.logback"           % "logback-core"                   % logbackVersion,
    "com.fatboyindustrial.gson-javatime-serialisers" % "gson-javatime-serialisers" % gsonJavatimeVersion,
    "com.google.code.gson"     % "gson"                           % gsonVersion,
    "com.h2database"           % "h2"                             % h2Version             % "test",
    "com.typesafe"             % "config"                         % configVersion,
    "com.typesafe.akka"       %% "akka-actor"                     % akkaVersion,
    "commons-codec"            % "commons-codec"                  % "1.10",
    "javax.servlet"            % "javax.servlet-api"              % "3.1.0",
    "javax.transaction"        % "jta"                            % jtaVersion,
    "junit"                    % "junit"                          % junitVersion          % "test",
    "net.databinder.dispatch" %% "dispatch-core"                  % "0.11.3",
    "net.sourceforge.jtds"     % "jtds"                           % jtdsVersion,
    "org.apache.derby"         % "derby"                          % derbyVersion, //          % "test",
    "org.apache.derby"         % "derbyclient"                    % derbyVersion, //          % "test",
    "org.apache.derby"         % "derbynet"                       % derbyVersion, //          % "test",
    "org.eclipse.jetty"        % "jetty-server"                   % jettyVersion          % "container;compile;test",
    "org.eclipse.jetty"        % "jetty-servlets"                 % jettyVersion          % "container;compile;test",
    "org.eclipse.jetty"        % "jetty-webapp"                   % jettyVersion          % "container;compile;test",
    "org.eclipse.persistence"  % "eclipselink"                    % eclipselinkVersion,
    "org.json4s"              %% "json4s-jackson"                  % "3.3.0",
    "org.scalatest"           %% "scalatest"                      % scalaTestVersion      % "test",
    "org.scalatra"            %% "scalatra"                       % scalatraVersion,
    "org.scalatra"            %% "scalatra-json"                  % scalatraVersion,
    "org.scalatra"            %% "scalatra-scalate"               % scalatraVersion,
    "org.scalatra"            %% "scalatra-slf4j"                 % scalatraVersion,
    "org.scalatra"            %% "scalatra-swagger"               % scalatraVersion,
    "org.scalatra"            %% "scalatra-swagger-ext"           % scalatraVersion,
    "org.slf4j"                % "log4j-over-slf4j"               % slf4jVersion,
    "org.slf4j"                % "slf4j-api"                      % slf4jVersion)
      .map(_.excludeAll(ExclusionRule("org.slf4j", "slf4j-jdk14"),
        ExclusionRule("javax.servlet", "servlet-api")))
}

resolvers in ThisBuild ++= Seq(Resolver.mavenLocal,
    Classpaths.typesafeReleases,
    "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
    "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
    "hohonuuli-bintray" at "http://dl.bintray.com/hohonuuli/maven")

ivyXML := <dependencies>
  <exclude module="slf4j-log4j12"/>
  <exclude module="grizzled-slf4j_2.9.1"/>
  <exclude module="jsr311-api" />
</dependencies>

//publishMavenStyle := true

//publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

// OTHER SETTINGS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


// -- sbt-native-packager
enablePlugins(JavaServerAppPackaging)

// -- xsbt-web-plugin
enablePlugins(JettyPlugin)

mainClass in assembly := Some("JettyMain")

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
   .setPreference(DanglingCloseParenthesis, Prevent)

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
