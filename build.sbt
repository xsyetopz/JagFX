import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import scala.sys.process._

Global / onChangedBuildSource := ReloadOnSourceChanges
Global / excludeLintKeys += Universal / libraryDependencies

ThisBuild / organization := "jagfx"
ThisBuild / version := "1.0.2"
ThisBuild / scalaVersion := "3.7.4"

ThisBuild / semanticdbEnabled := true

val javaFxVersion = "23.0.1"
val logbackVersion = "1.5.23"
val scribeVersion = "3.17.0"
val ikonliVersion = "12.4.0"
val munitVersion = "1.0.4"

lazy val root = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "jagfx",
    Compile / mainClass := Some("jagfx.Launcher"),
    executableScriptName := "jagfx",
    maintainer := "xsyetopz",
    libraryDependencies ++= currentJavaFxDeps,
    Universal / libraryDependencies ++= allJavaFxDeps,
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      "com.outr" %% "scribe-slf4j" % scribeVersion,
      "org.kordamp.ikonli" % "ikonli-javafx" % ikonliVersion,
      "org.kordamp.ikonli" % "ikonli-materialdesign2-pack" % ikonliVersion,
      "org.scalameta" %% "munit" % munitVersion % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    Compile / resourceGenerators += Def.task {
      scss.value
      Seq.empty[File]
    }.taskValue
  )

addCommandAlias(
  "dev",
  "; set run / fork := false; set run / connectInput := true; run"
)
addCommandAlias(
  "prod",
  "; set run / fork := true; set run / connectInput := false; run"
)
addCommandAlias(
  "cli",
  "; set run / fork := true; set run / connectInput := true; runMain jagfx.JagFXCli"
)
addCommandAlias(
  "dist",
  "; set run / fork := true; universal:packageBin"
)

lazy val javaFxModules =
  Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")

lazy val osClassifier = {
  val osName = System.getProperty("os.name").toLowerCase
  val osArch = System.getProperty("os.arch").toLowerCase
  if (osName.contains("linux")) "linux"
  else if (osName.contains("mac")) {
    if (osArch == "aarch64") "mac-aarch64" else "mac"
  } else if (osName.contains("windows")) "win"
  else throw new Exception(s"Unknown OS: $osName")
}

lazy val currentJavaFxDeps =
  javaFxModules.map(m =>
    "org.openjfx" % s"javafx-$m" % javaFxVersion classifier osClassifier
  )

lazy val allJavaFxDeps =
  for {
    m <- javaFxModules
    p <- Seq("win", "linux", "mac", "mac-aarch64")
  } yield "org.openjfx" % s"javafx-$m" % javaFxVersion classifier p

lazy val scss = taskKey[Unit]("Compile SCSS to CSS")
scss := {
  def isToolAvailable(tool: String): Boolean =
    try Process(Seq("which", tool)).! == 0
    catch { case _: Exception => false }

  val compiler =
    if (isToolAvailable("bunx")) "bunx"
    else if (isToolAvailable("npx")) "npx"
    else
      throw new Exception(
        "SCSS compilation failed: neither 'bunx' nor 'npx' found in PATH"
      )

  val src = "src/main/scss/style.scss"
  val dst = "src/main/resources/jagfx/style.css"
  val exit = s"$compiler sass $src $dst --no-source-map".!
  if (exit != 0)
    throw new Exception(s"SCSS compilation failed with code $exit")
}
