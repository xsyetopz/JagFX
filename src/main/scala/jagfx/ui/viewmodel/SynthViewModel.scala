package jagfx.ui.viewmodel

import javafx.beans.property._
import javafx.collections._
import jagfx.model._
import jagfx.Constants
import scala.jdk.CollectionConverters._

enum RackMode:
  case Main, Filter, Both

/** Root `ViewModel` encapsulating entire `.synth` file state. */
class SynthViewModel:
  private val activeToneIndex = new SimpleIntegerProperty(0)
  private val tones = FXCollections.observableArrayList[ToneViewModel]()
  private val loopStart = new SimpleIntegerProperty(0)
  private val loopEnd = new SimpleIntegerProperty(0)
  private val loopCount = new SimpleIntegerProperty(0)
  private val loopEnabled = new SimpleBooleanProperty(false)

  val rackMode = new SimpleObjectProperty[RackMode](RackMode.Main)
  val selectedCellIndex = new SimpleIntegerProperty(-1)

  // TGT: false = TONE, true = ALL
  private val targetMode = new SimpleBooleanProperty(false)

  private val currentFilePath = new SimpleStringProperty("Untitled.synth")
  def currentFilePathProperty: StringProperty = currentFilePath
  def setCurrentFilePath(path: String): Unit = currentFilePath.set(path)

  for _ <- 0 until Constants.MaxTones do tones.add(new ToneViewModel())

  initDefault()

  def initDefault(): Unit =
    tones.asScala.foreach(_.clear())

    val t1 = tones.get(0)
    t1.enabled.set(true)
    t1.duration.set(1000)
    t1.volume.form.set(WaveForm.Square)
    val h1 = t1.harmonics(0)
    h1.active.set(true)
    h1.volume.set(100)

    h1.volume.set(100)

    // select OUTPUT cell by default to avoid selectless state
    selectedCellIndex.set(8)

    currentFilePath.set("Untitled.synth")

  def reset(): Unit = initDefault()

  def activeToneIndexProperty: IntegerProperty = activeToneIndex
  def getActiveToneIndex: Int = activeToneIndex.get
  def setActiveToneIndex(idx: Int): Unit = activeToneIndex.set(idx)

  def getTones: ObservableList[ToneViewModel] = tones
  def getActiveTone: ToneViewModel = tones.get(activeToneIndex.get)

  def loopStartProperty: IntegerProperty = loopStart
  def loopEndProperty: IntegerProperty = loopEnd
  def loopCountProperty: IntegerProperty = loopCount

  def loopEnabledProperty: BooleanProperty = loopEnabled
  def isLoopEnabled: Boolean = loopEnabled.get

  def targetModeProperty: BooleanProperty = targetMode
  def isTargetAll: Boolean = targetMode.get

  // max of `tone.duration + tone.start` across all active tones
  private val totalDuration = new SimpleIntegerProperty(0)

  def totalDurationProperty: IntegerProperty = totalDuration

  private val fileLoaded = new SimpleObjectProperty[java.lang.Long](0L)
  def fileLoadedProperty: ObjectProperty[java.lang.Long] = fileLoaded

  def load(file: SynthFile): Unit =
    import Constants._
    loopStart.set(file.loop.begin)
    loopEnd.set(file.loop.end)
    for i <- 0 until MaxTones do
      val tone = file.tones.lift(i).flatten
      tones.get(i).load(tone)

    // total duration from active tones
    val maxDur = (0 until MaxTones)
      .flatMap(i => file.tones.lift(i).flatten)
      .map(t => t.duration + t.start)
      .maxOption
      .getOrElse(0)
    totalDuration.set(maxDur)

    fileLoaded.set(System.currentTimeMillis())

  def toModel(): SynthFile =
    val toneModels = tones
      .stream()
      .map(_.toModel())
      .toArray(size => new Array[Option[Tone]](size))
      .toVector
    val loop = LoopParams(loopStart.get, loopEnd.get)
    SynthFile(toneModels, loop)

  private var toneClipboard: Option[Option[Tone]] = None

  def copyActiveTone(): Unit =
    toneClipboard = Some(getActiveTone.toModel())

  def pasteToActiveTone(): Unit =
    toneClipboard.foreach(getActiveTone.load)
