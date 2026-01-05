package jagfx.ui.viewmodel

import scala.jdk.CollectionConverters.*

import jagfx.Constants
import jagfx.model.*
import jagfx.synth.SynthesisExecutor
import javafx.beans.property.*
import javafx.collections.*

/** Rack display mode for filter/envelope view switching. */
enum RackMode:
  case Main, Filter, Both

/** Root view model encapsulating entire `.synth` file state. */
class SynthViewModel:
  // Fields
  private val totalDuration = new SimpleIntegerProperty(0)
  private var toneClipboard: Option[Option[Tone]] = None

  private val activeToneIndex = new SimpleIntegerProperty(0)
  private val tones = FXCollections.observableArrayList[ToneViewModel]()
  private val loopStart = new SimpleIntegerProperty(0)
  private val loopEnd = new SimpleIntegerProperty(0)
  private val loopCount = new SimpleIntegerProperty(0)
  private val loopEnabled = new SimpleBooleanProperty(false)
  private val fileLoaded = new SimpleObjectProperty[java.lang.Long](0L)

  // TGT: false = TONE, true = ALL
  private val targetMode = new SimpleBooleanProperty(false)

  private val currentFilePath = new SimpleStringProperty("Untitled.synth")

  /** Current rack display mode. */
  val rackMode = new SimpleObjectProperty[RackMode](RackMode.Main)

  /** Currently selected cell index (`-1` if none). */
  val selectedCellIndex = new SimpleIntegerProperty(-1)

  for _ <- 0 until Constants.MaxTones do tones.add(new ToneViewModel())

  initDefault()

  /** Current file path property for window title. */
  def currentFilePathProperty: StringProperty = currentFilePath

  /** Sets current file path. */
  def setCurrentFilePath(path: String): Unit = currentFilePath.set(path)

  /** Initializes default tone state. */
  def initDefault(): Unit =
    tones.asScala.foreach(_.clear())

    val tone1 = tones.get(0)
    tone1.enabled.set(true)
    tone1.duration.set(1000)
    tone1.volume.waveform.set(Waveform.Square)

    val partial1 = tone1.partials(0)
    partial1.active.set(true)
    partial1.volume.set(100)

    selectedCellIndex.set(8)

    currentFilePath.set("Untitled.synth")

  /** Resets to default state. */
  def reset(): Unit = initDefault()

  /** Active tone index property. */
  def activeToneIndexProperty: IntegerProperty = activeToneIndex

  /** Returns currently active tone index. */
  def getActiveToneIndex: Int = activeToneIndex.get

  /** Sets active tone index. */
  def setActiveToneIndex(idx: Int): Unit = activeToneIndex.set(idx)

  /** Observable list of all tone view models. */
  def getTones: ObservableList[ToneViewModel] = tones

  /** Returns currently active tone view model. */
  def getActiveTone: ToneViewModel = tones.get(activeToneIndex.get)

  /** Loop start position property. */
  def loopStartProperty: IntegerProperty = loopStart

  /** Loop end position property. */
  def loopEndProperty: IntegerProperty = loopEnd

  /** Loop repetition count property. */
  def loopCountProperty: IntegerProperty = loopCount

  /** Loop enabled state property. */
  def loopEnabledProperty: BooleanProperty = loopEnabled

  /** Returns `true` if loop playback is enabled. */
  def isLoopEnabled: Boolean = loopEnabled.get

  /** Target mode property (`false` = ONE, `true` = ALL). */
  def targetModeProperty: BooleanProperty = targetMode

  /** Returns `true` if edits affect all tones. */
  def isTargetAll: Boolean = targetMode.get

  /** Total duration property (max of all tone durations). */
  def totalDurationProperty: IntegerProperty = totalDuration

  /** File loaded timestamp property for change detection. */
  def fileLoadedProperty: ObjectProperty[java.lang.Long] = fileLoaded

  /** Loads `.synth` file data into all tone view models. */
  def load(file: SynthFile): Unit =
    import Constants._
    SynthesisExecutor.cancelPending()

    loopStart.set(file.loop.begin)
    loopEnd.set(file.loop.end)
    for i <- 0 until MaxTones do
      val tone = file.tones.lift(i).flatten
      tones.get(i).load(tone)

    val maxDur = (0 until MaxTones)
      .flatMap(i => file.tones.lift(i).flatten)
      .map(t => t.duration + t.start)
      .maxOption
      .getOrElse(0)
    totalDuration.set(maxDur)
    activeToneIndex.set(0) // go TONE_0 whenever file loaded
    fileLoaded.set(System.currentTimeMillis())

  /** Converts all tone view models to model `SynthFile`. */
  def toModel(): SynthFile =
    val toneModels = tones
      .stream()
      .map(_.toModel())
      .toArray(size => new Array[Option[Tone]](size))
      .toVector
    val loop = LoopParams(loopStart.get, loopEnd.get)
    SynthFile(toneModels, loop)

  /** Copies active tone to clipboard. */
  def copyActiveTone(): Unit =
    toneClipboard = Some(getActiveTone.toModel())

  /** Pastes clipboard to active tone. */
  def pasteToActiveTone(): Unit =
    toneClipboard.foreach(getActiveTone.load)
