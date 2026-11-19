package io.github.ctznsniiips.lobby.device

/**
 * A tiny hand-off between [io.github.ctznsniiips.lobby.HomeOverrideService] (an AccessibilityService, a separate
 * component from the UI) and the view model, for the experimental auto-pair flow.
 *
 * When auto-pair is armed, the accessibility service reads the 6-digit code AND the "IP address & Port" off the live
 * "Pair device with pairing code" dialog and drops them here. Reading the endpoint off-screen (instead of via mDNS)
 * matters because Lobby is backgrounded while the dialog is up, and background mDNS discovery is unreliable — but the
 * accessibility service keeps receiving the dialog's window events regardless. The dialog never leaves the screen, so
 * its pairing server stays alive, which is the whole point on TVs that tear it down when it's backgrounded.
 */
object AdbPairingBridge {
    data class Scraped(val code: String?, val host: String?, val port: Int)

    @Volatile var listening: Boolean = false
        private set
    @Volatile private var onScraped: ((Scraped) -> Unit)? = null

    fun start(onScraped: (Scraped) -> Unit) {
        this.onScraped = onScraped
        listening = true
    }

    fun stop() {
        listening = false
        onScraped = null
    }

    /** Called by the accessibility service with whatever it could read off the pairing dialog this pass. */
    fun submit(scraped: Scraped) {
        if (listening) onScraped?.invoke(scraped)
    }
}
