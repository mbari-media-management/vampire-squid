val akkaVersion = "2.4.17"
val auth0Version = "3.1.0"
val codecVersion = "1.10"
val configVersion = "1.3.1"
val derbyVersion = "10.13.1.1"
val eclipselinkVersion = "2.6.4"
val gsonJavatimeVersion = "1.1.1"
val gsonVersion = "2.8.0"
val h2Version = "1.4.194"
val jettyVersion = "9.4.3.v20170317"
val json4sVersion = "3.5.1"
val jtaVersion = "1.1"
val jtdsVersion = "1.3.1"
val junitVersion = "4.12"
val logbackVersion = "1.2.3"
val rabbitmqVersion = "4.1.0"
val scalaTestVersion = "3.0.1"
val scalatraVersion = "2.5.0"
val servletVersion = "3.1.0"
val slf4jVersion = "1.7.25"


lazy val buildSettings = Seq(
  organization := "org.mbari.vars",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.1",
  crossScalaVersions := Seq("2.12.1")
)

lazy val consoleSettings = Seq(
  shellPrompt := { state =>
    val user = System.getProperty("user.name")
    user + "@" + Project.extract(state).currentRef.project + ":sbt> "
  },
  initialCommands in console :=
    """
      |import java.time.Instant
      |import java.util.UUID
    """.stripMargin
)

lazy val dependencySettings = Seq(
  resolvers ++= Seq(
    Resolver.mavenLocal,
    Resolver.bintrayRepo("hohonuuli", "maven"),
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  )
)

lazy val optionSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8", // yes, this is 2 args
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Xfuture"),
  javacOptions ++= Seq("-target", "1.8", "-source", "1.8"),
  incOptions := incOptions.value.withNameHashing(true),
  updateOptions := updateOptions.value.withCachedResolution(true)
)

lazy val appSettings = buildSettings ++
  consoleSettings ++
  dependencySettings ++
  optionSettings ++ Seq(
  todosTags := Set("TODO", "FIXME", "WTF"),
  fork := true
)

lazy val apps = Seq("jetty-main") // for sbt-pack

lazy val `vampire-squid` = (project in file("."))
  .enablePlugins(JettyPlugin)
  .settings(appSettings)
  .settings(
    mainClass in assembly := Some("JettyMain"),
    libraryDependencies ++=   Seq(
      "ch.qos.logback"           % "logback-classic"                % logbackVersion,
      "ch.qos.logback"           % "logback-core"                   % logbackVersion,
      "com.auth0"                % "java-jwt"                       % auth0Version,
      "com.fatboyindustrial.gson-javatime-serialisers" % "gson-javatime-serialisers" % gsonJavatimeVersion,
      "com.rabbitmq"             % "amqp-client"                    % rabbitmqVersion,
      "com.google.code.gson"     % "gson"                           % gsonVersion,
      "com.h2database"           % "h2"                             % h2Version             % "test",
      "com.typesafe"             % "config"                         % configVersion,
      "com.typesafe.akka"       %% "akka-actor"                     % akkaVersion,
      "commons-codec"            % "commons-codec"                  % codecVersion,
      "javax.servlet"            % "javax.servlet-api"              % servletVersion,
      "javax.transaction"        % "jta"                            % jtaVersion,
      "junit"                    % "junit"                          % junitVersion          % "test",
      "net.sourceforge.jtds"     % "jtds"                           % jtdsVersion,
      "org.apache.derby"         % "derby"                          % derbyVersion, //          % "test",
      "org.apache.derby"         % "derbyclient"                    % derbyVersion, //          % "test",
      "org.apache.derby"         % "derbynet"                       % derbyVersion, //          % "test",
      "org.eclipse.jetty"        % "jetty-server"                   % jettyVersion          % "container;compile;test",
      "org.eclipse.jetty"        % "jetty-servlets"                 % jettyVersion          % "container;compile;test",
      "org.eclipse.jetty"        % "jetty-webapp"                   % jettyVersion          % "container;compile;test",
      "org.eclipse.persistence"  % "eclipselink"                    % eclipselinkVersion,
      "org.json4s"              %% "json4s-jackson"                 % json4sVersion,
      "org.scalatest"           %% "scalatest"                      % scalaTestVersion      % "test",
      "org.scalatra"            %% "scalatra"                       % scalatraVersion,
      "org.scalatra"            %% "scalatra-json"                  % scalatraVersion,
      "org.scalatra"            %% "scalatra-scalate"               % scalatraVersion,
      "org.scalatra"            %% "scalatra-slf4j"                 % scalatraVersion,
      "org.scalatra"            %% "scalatra-swagger"               % scalatraVersion,
      "org.scalatra"            %% "scalatra-swagger-ext"           % scalatraVersion,
      "org.scalatra"            %% "scalatra-scalatest"             % scalatraVersion,
      "org.slf4j"                % "log4j-over-slf4j"               % slf4jVersion,
      "org.slf4j"                % "slf4j-api"                      % slf4jVersion)
        .map(_.excludeAll(ExclusionRule("org.slf4j", "slf4j-jdk14"),
          ExclusionRule("javax.servlet", "servlet-api")))
  )
  .settings( // config sbt-pack
    packAutoSettings ++ Seq(
      packExtraClasspath := apps.map(_ -> Seq("${PROG_HOME}/conf")).toMap,
      packJvmOpts := apps.map(_ -> Seq("-Duser.timezone=UTC", "-Xmx4g")).toMap,
      packDuplicateJarStrategy := "latest",
      packJarNameConvention := "original"
    )
  )

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

// Aliases
addCommandAlias("cleanall", ";clean;clean-files")

