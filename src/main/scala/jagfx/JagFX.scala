package jagfx

import javafx.application._
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import jagfx.ui.controller.MainController

val (minWidth, minHeight) = (800, 520)

/** GUI application entry point. */
class JagFX extends Application:
  override def start(stage: Stage): Unit =
    val root = MainController.createRoot()
    val scene = Scene(root, minWidth, minHeight)
    scene.getStylesheets.add(
      getClass.getResource("/jagfx/style.css").toExternalForm
    )

    stage.setTitle("JagFX")
    stage.setScene(scene)
    stage.setMinWidth(minWidth)
    stage.setMinHeight(minHeight)
    stage.setResizable(false)
    stage.setOnCloseRequest(_ => {
      Platform.exit()
      System.exit(0)
    })
    stage.show()

object JagFX:
  def main(args: Array[String]): Unit =
    Application.launch(classOf[JagFX], args*)
