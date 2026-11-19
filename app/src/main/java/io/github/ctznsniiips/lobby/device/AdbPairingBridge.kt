package io.github.ctznsniiips.lobby.device

/**
 * A tiny hand-off between [io.github.ctznsniiips.lobby.HomeOverrideService] (an AccessibilityService, a separate
 * component from the UI) and the view model, for the experimental auto-pair flow.
 *
 * When auto-pair is armed, the accessibility service reads the 6-digit code off the live "Pair device with pairing
 * code" dialog and drops it here. Because the dialog never leaves the screen, its pairing server stays alive — which
 * is the whole point: on TVs that tear the server down the moment the dialog is backgrounded, this is the only way
 * to pair on-device without a second machine.
 */
object AdbPairingBridge {
    @Volatile var listening: Boolean = false
        private set
    @Volatile private var onCode: ((String) -> Unit)? = null

    fun start(onCode: (String) -> Unit) {
        this.onCode = onCode
        listening = true
    }

    fun stop() {
        listening = false
        onCode = null
    }

    /** Called by the accessibility service with a 6-digit code scraped from the pairing dialog. */
    fun submit(code: String) {
        if (listening) onCode?.invoke(code)
    }
}
