import com.typesafe.sbt.packager.archetypes
import scala.sys.process._

// --- Global / Build-wide Settings ---
Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / organization := "jagfx"
ThisBuild / version := "0.2.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.7.4"

ThisBuild / semanticdbEnabled := true

// --- Dependency Versions ---
val javaFxVersion = "23.0.1"
val logbackVersion = "1.5.23"
val scribeVersion = "3.17.0"
val ikonliVersion = "12.4.0"
val munitVersion = "1.0.4"

// --- Setting Keys ---
val isFatJar =
  settingKey[Boolean]("Flag to include all platform natives in the build")

// --- Project Definition ---
lazy val root = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging, JlinkPlugin)
  .settings(
    name := "jagfx",
    isFatJar := false,

    // App Packaging
    Compile / mainClass := Some("jagfx.Launcher"),
    executableScriptName := "jagfx",

    // Dependencies
    libraryDependencies ++= javaFxDependencies.value,
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      "com.outr" %% "scribe-slf4j" % scribeVersion,
      "org.kordamp.ikonli" % "ikonli-javafx" % ikonliVersion,
      "org.kordamp.ikonli" % "ikonli-materialdesign2-pack" % ikonliVersion,
      "org.scalameta" %% "munit" % munitVersion % Test
    ),

    // Test & Run Configuration
    testFrameworks += new TestFramework("munit.Framework"),
    run / fork := true,
    run / connectInput := true,
    outputStrategy := Some(StdoutOutput),

    // --- Assembly (Fat JAR) Settings ---
    assembly / mainClass := Some("jagfx.Launcher"),
    assembly / assemblyJarName := s"jagfx-all-platforms-${version.value}.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("module-info.class")         => MergeStrategy.discard
      case x if x.endsWith("module-info.class")  => MergeStrategy.discard
      case x if x.contains("META-INF/versions/") => MergeStrategy.first
      case x                                     =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    },

    // --- Jlink Settings ---
    jlinkIgnoreMissingDependency := { _ => true },
    jlinkOptions ++= Seq(
      "--no-header-files",
      "--no-man-pages",
      "--strip-debug"
    ),
    jlinkModules ++= Seq("jdk.unsupported", "java.scripting", "java.xml")
  )

// --- Command Aliases ---
addCommandAlias("cli", "runMain jagfx.JagFXCli")
addCommandAlias(
  "fatJar",
  "; set root / isFatJar := true; assembly; set root / isFatJar := false"
)

// --- Helper Functions & Tasks ---
lazy val javaFxDependencies = Def.setting {
  val modules =
    Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")

  if (isFatJar.value) {
    val platforms = Seq("win", "linux", "mac", "mac-aarch64")
    for {
      m <- modules
      p <- platforms
    } yield "org.openjfx" % s"javafx-$m" % javaFxVersion classifier p
  } else {
    val osName = System.getProperty("os.name").toLowerCase
    val osArch = System.getProperty("os.arch").toLowerCase

    val classifier =
      if (osName.contains("linux")) "linux"
      else if (osName.contains("mac")) {
        if (osArch == "aarch64") "mac-aarch64" else "mac"
      } else if (osName.contains("windows")) "win"
      else throw new Exception(s"Unknown OS name: $osName")

    modules.map(m =>
      "org.openjfx" % s"javafx-$m" % javaFxVersion classifier classifier
    )
  }
}

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
  if (exit != 0) throw new Exception(s"SCSS compilation failed with code $exit")
}

Compile / compile := ((Compile / compile) dependsOn scss).value
