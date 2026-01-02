import com.typesafe.sbt.packager.archetypes

val scala3Version = "3.7.4"

lazy val root = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "jagfx",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    Compile / mainClass := Some("jagfx.JagFXCli"),
    executableScriptName := "jagfx-cli",
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
      "com.outr" %% "scribe-slf4j" % "3.17.0",
      "ch.qos.logback" % "logback-classic" % "1.5.23",
      "org.scalameta" %% "munit" % "1.0.4" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    run / fork := true,
    run / connectInput := true,
    outputStrategy := Some(StdoutOutput)
  )
