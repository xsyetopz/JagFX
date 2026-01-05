package jagfx.ui.viewmodel

import javafx.beans.property._
import javafx.collections._
import jagfx.model._
import jagfx.Constants
import scala.jdk.CollectionConverters._
import jagfx.synth.SynthesisExecutor

enum RackMode:
  case Main, Filter, Both

/** Root `ViewModel` encapsulating entire `.synth` file state. */
class SynthViewModel:
  private val _activeToneIndex = new SimpleIntegerProperty(0)
  private val _tones = FXCollections.observableArrayList[ToneViewModel]()
  private val _loopStart = new SimpleIntegerProperty(0)
  private val _loopEnd = new SimpleIntegerProperty(0)
  private val _loopCount = new SimpleIntegerProperty(0)
  private val _loopEnabled = new SimpleBooleanProperty(false)
  private val _fileLoaded = new SimpleObjectProperty[java.lang.Long](0L)

  val rackMode = new SimpleObjectProperty[RackMode](RackMode.Main)
  val selectedCellIndex = new SimpleIntegerProperty(-1)

  // TGT: false = TONE, true = ALL
  private val _targetMode = new SimpleBooleanProperty(false)

  private val _currentFilePath = new SimpleStringProperty("Untitled.synth")
  def currentFilePathProperty: StringProperty = _currentFilePath
  def setCurrentFilePath(path: String): Unit = _currentFilePath.set(path)

  for _ <- 0 until Constants.MaxTones do _tones.add(new ToneViewModel())

  initDefault()

  def initDefault(): Unit =
    _tones.asScala.foreach(_.clear())

    val t1 = _tones.get(0)
    t1.enabled.set(true)
    t1.duration.set(1000)
    t1.volume.form.set(WaveForm.Square)
    val h1 = t1.harmonics(0)
    h1.active.set(true)
    h1.volume.set(100)

    h1.volume.set(100)

    // select OUTPUT cell by default to avoid selectless state
    selectedCellIndex.set(8)

    _currentFilePath.set("Untitled.synth")

  def reset(): Unit = initDefault()

  def activeToneIndexProperty: IntegerProperty = _activeToneIndex
  def getActiveToneIndex: Int = _activeToneIndex.get
  def setActiveToneIndex(idx: Int): Unit = _activeToneIndex.set(idx)

  def getTones: ObservableList[ToneViewModel] = _tones
  def getActiveTone: ToneViewModel = _tones.get(_activeToneIndex.get)

  def loopStartProperty: IntegerProperty = _loopStart
  def loopEndProperty: IntegerProperty = _loopEnd
  def loopCountProperty: IntegerProperty = _loopCount

  def loopEnabledProperty: BooleanProperty = _loopEnabled
  def isLoopEnabled: Boolean = _loopEnabled.get

  def targetModeProperty: BooleanProperty = _targetMode
  def isTargetAll: Boolean = _targetMode.get

  // max of `tone.duration + tone.start` across all active tones
  private val _totalDuration = new SimpleIntegerProperty(0)

  def totalDurationProperty: IntegerProperty = _totalDuration
  def fileLoadedProperty: ObjectProperty[java.lang.Long] = _fileLoaded

  def load(file: SynthFile): Unit =
    import Constants._
    SynthesisExecutor.cancelPending()

    _loopStart.set(file.loop.begin)
    _loopEnd.set(file.loop.end)
    for i <- 0 until MaxTones do
      val tone = file.tones.lift(i).flatten
      _tones.get(i).load(tone)

    val maxDur = (0 until MaxTones)
      .flatMap(i => file.tones.lift(i).flatten)
      .map(t => t.duration + t.start)
      .maxOption
      .getOrElse(0)
    _totalDuration.set(maxDur)
    _activeToneIndex.set(0) // go `Tone 1` whenever file loaded
    _fileLoaded.set(System.currentTimeMillis())

  def toModel(): SynthFile =
    val toneModels = _tones
      .stream()
      .map(_.toModel())
      .toArray(size => new Array[Option[Tone]](size))
      .toVector
    val loop = LoopParams(_loopStart.get, _loopEnd.get)
    SynthFile(toneModels, loop)

  private var _toneClipboard: Option[Option[Tone]] = None

  def copyActiveTone(): Unit =
    _toneClipboard = Some(getActiveTone.toModel())

  def pasteToActiveTone(): Unit =
    _toneClipboard.foreach(getActiveTone.load)
