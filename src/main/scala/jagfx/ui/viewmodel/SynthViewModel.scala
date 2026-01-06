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
  private var voiceClipboard: Option[Option[Voice]] = None

  private val activeVoiceIndex = new SimpleIntegerProperty(0)
  private val voices = FXCollections.observableArrayList[VoiceViewModel]()
  private val loopStart = new SimpleIntegerProperty(0)
  private val loopEnd = new SimpleIntegerProperty(0)
  private val loopCount = new SimpleIntegerProperty(0)
  private val loopEnabled = new SimpleBooleanProperty(false)
  private val fileLoaded = new SimpleObjectProperty[java.lang.Long](0L)

  // TGT: false = ONE, true = ALL
  private val targetMode = new SimpleBooleanProperty(false)

  private val currentFilePath = new SimpleStringProperty("Untitled.synth")

  /** Current rack display mode. */
  val rackMode = new SimpleObjectProperty[RackMode](RackMode.Main)

  /** Currently selected cell index (`-1` if none). */
  val selectedCellIndex = new SimpleIntegerProperty(-1)

  for _ <- 0 until Constants.MaxVoices do voices.add(new VoiceViewModel())

  initDefault()

  /** Current file path property for window title. */
  def currentFilePathProperty: StringProperty = currentFilePath

  /** Sets current file path. */
  def setCurrentFilePath(path: String): Unit = currentFilePath.set(path)

  /** Initializes default voice state. */
  def initDefault(): Unit =
    voices.asScala.foreach(_.clear())

    val voice1 = voices.get(0)
    voice1.enabled.set(true)
    voice1.duration.set(1000)
    voice1.volume.waveform.set(Waveform.Square)

    val partial1 = voice1.partials(0)
    partial1.active.set(true)
    partial1.volume.set(100)

    selectedCellIndex.set(8)

    currentFilePath.set("Untitled.synth")

  /** Resets to default state. */
  def reset(): Unit = initDefault()

  /** Active voice index property. */
  def activeVoiceIndexProperty: IntegerProperty = activeVoiceIndex

  /** Returns currently active voice index. */
  def getActiveVoiceIndex: Int = activeVoiceIndex.get

  /** Sets active voice index. */
  def setActiveVoiceIndex(idx: Int): Unit = activeVoiceIndex.set(idx)

  /** Observable list of all voice view models. */
  def getVoices: ObservableList[VoiceViewModel] = voices

  /** Returns currently active voice view model. */
  def getActiveVoice: VoiceViewModel = voices.get(activeVoiceIndex.get)

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

  /** Returns `true` if edits affect all voices. */
  def isTargetAll: Boolean = targetMode.get

  /** Total duration property (max of all voice durations). */
  def totalDurationProperty: IntegerProperty = totalDuration

  /** File loaded timestamp property for change detection. */
  def fileLoadedProperty: ObjectProperty[java.lang.Long] = fileLoaded

  /** Loads `.synth` file data into all voice view models. */
  def load(file: SynthFile): Unit =
    import Constants._
    SynthesisExecutor.cancelPending()

    loopStart.set(file.loop.begin)
    loopEnd.set(file.loop.end)
    for i <- 0 until MaxVoices do
      val voice = file.voices.lift(i).flatten
      voices.get(i).load(voice)

    val maxDur = (0 until MaxVoices)
      .flatMap(i => file.voices.lift(i).flatten)
      .map(t => t.duration + t.start)
      .maxOption
      .getOrElse(0)
    totalDuration.set(maxDur)
    activeVoiceIndex.set(0) // go TONE_0 whenever file loaded
    fileLoaded.set(System.currentTimeMillis())

  /** Converts all voice view models to model `SynthFile`. */
  def toModel(): SynthFile =
    val voiceModels = voices
      .stream()
      .map(_.toModel())
      .toArray(size => new Array[Option[Voice]](size))
      .toVector
    val loop = LoopParams(loopStart.get, loopEnd.get)
    SynthFile(voiceModels, loop)

  /** Copies active voice to clipboard. */
  def copyActiveVoice(): Unit =
    voiceClipboard = Some(getActiveVoice.toModel())

  /** Pastes clipboard to active voice. */
  def pasteToActiveVoice(): Unit =
    voiceClipboard.foreach(getActiveVoice.load)
