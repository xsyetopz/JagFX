package jagfx

import jagfx.ui.controller.MainController
import javafx.application.*
import javafx.scene.Scene
import javafx.stage.Stage

// Constants
private final val AppDims = (800, 520)

/** GUI application entry point. */
class JagFX extends Application:
  override def start(stage: Stage): Unit =
    val root = MainController.createRoot()
    val scene = Scene(root, AppDims._1, AppDims._2)
    scene.getStylesheets.add(
      getClass.getResource("/jagfx/style.css").toExternalForm
    )

    stage.setTitle("JagFX")
    stage.setScene(scene)
    stage.setMinWidth(AppDims._1)
    stage.setMinHeight(AppDims._2)
    stage.setResizable(false)
    stage.setOnCloseRequest(_ =>
      Platform.exit()
      System.exit(0)
    )
    stage.show()

object JagFX:
  /** Application entry point. */
  def main(args: Array[String]): Unit =
    Application.launch(classOf[JagFX], args*)
