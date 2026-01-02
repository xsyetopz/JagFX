import com.typesafe.sbt.packager.archetypes

ThisBuild / semanticdbEnabled := true

val scala3Version = "3.7.4"

lazy val root = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "jagfx",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    Compile / mainClass := Some("jagfx.Launcher"),
    executableScriptName := "jagfx",
    libraryDependencies ++= {
      val javafxModules =
        Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
      val osName = {
        val name = System.getProperty("os.name").toLowerCase
        val arch = System.getProperty("os.arch").toLowerCase
        if (name.contains("linux")) "linux"
        else if (name.contains("mac")) {
          if (arch == "aarch64") "mac-aarch64" else "mac"
        } else if (name.contains("windows")) "win"
        else throw new Exception("Unknown OS name: " + name)
      }
      javafxModules
        .map(m => "org.openjfx" % s"javafx-$m" % "23.0.1" classifier osName)
    },
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.5.23",
      "com.outr" %% "scribe-slf4j" % "3.17.0",
      "org.kordamp.ikonli" % "ikonli-javafx" % "12.4.0",
      "org.kordamp.ikonli" % "ikonli-materialdesign2-pack" % "12.4.0",
      "org.scalameta" %% "munit" % "1.0.4" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    run / fork := true,
    run / connectInput := true,
    outputStrategy := Some(StdoutOutput)
  )

addCommandAlias("cli", "runMain jagfx.JagFXCli")

lazy val scss = taskKey[Unit]("Compile SCSS to CSS")
scss := {
  import scala.sys.process._

  def isToolAvailable(tool: String): Boolean =
    try Process(Seq("which", tool)).! == 0
    catch { case _: Exception => false }

  val compiler =
    if (isToolAvailable("bunx")) "bunx"
    else if (isToolAvailable("npx")) "npx"
    else ""
  if (compiler.isEmpty)
    throw new Exception(
      "SCSS compilation failed: neither 'bunx' nor 'npx' found in PATH"
    )

  val src = "src/main/scss/style.scss"
  val dst = "src/main/resources/jagfx/style.css"
  val cmd = s"$compiler sass $src $dst --no-source-map"
  val exit = cmd.!
  if (exit != 0) throw new Exception(s"SCSS compilation failed with code $exit")
}

Compile / compile := ((Compile / compile) dependsOn scss).value
