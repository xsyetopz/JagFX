package jagfx.ui.viewmodel

import jagfx.model.*

class EnvelopeViewModelSuite extends munit.FunSuite:

  test("isZero returns true for default empty envelope"):
    val vm = new EnvelopeViewModel()
    assert(vm.isZero)

  test("isZero returns false if segments exist"):
    val vm = new EnvelopeViewModel()
    val env = Envelope(Waveform.Square, 0, 0, Vector(EnvelopeSegment(10, 10)))
    vm.load(env)
    assert(!vm.isZero)

  test("isZero returns true if segments have zero peaks"):
    val vm = new EnvelopeViewModel()
    val env = Envelope(Waveform.Square, 0, 0, Vector(EnvelopeSegment(10, 0)))
    vm.load(env)
    assert(vm.isZero)

  test("isZero returns false if start or end is non-zero"):
    val vm = new EnvelopeViewModel()
    // start != 0
    vm.load(Envelope(Waveform.Square, 10, 0, Vector.empty))
    assert(!vm.isZero)

    // end != 0
    vm.load(Envelope(Waveform.Square, 0, 10, Vector.empty))
    assert(!vm.isZero)
