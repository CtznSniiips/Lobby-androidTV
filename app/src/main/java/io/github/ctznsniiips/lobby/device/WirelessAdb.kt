package io.github.ctznsniiips.lobby.device

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Experimental: finds Android's Wireless Debugging (ADB-over-TLS) endpoints on THIS device using mDNS.
 *
 * Two modes:
 *  - [scan] — a one-shot probe that reports once, used to confirm Wireless Debugging is on and grab the connect host/port.
 *  - [watch] — a continuous watch used during pairing. The catch on a single-screen TV is that Android only advertises
 *    "_adb-tls-pairing._tcp" (and keeps its pairing server alive) WHILE the "Pair device with pairing code" dialog is
 *    showing. Lobby can't be foreground at the same time, so instead it watches in the background, captures and holds
 *    the pairing port the instant the dialog opens, and the user returns to Lobby with HOME (which only pauses the
 *    dialog, leaving the server alive) rather than BACK (which dismisses it).
 *
 * Every endpoint is matched against this device's own interface addresses: mDNS surfaces every device on the LAN with
 * Wireless Debugging on, but Lobby can only ever install on itself, so a neighbour's daemon is noise.
 */
class WirelessAdb(context: Context) {

    data class Result(
        /** True if THIS device's own "_adb-tls-connect._tcp" service was seen — i.e. our Wireless Debugging is enabled. */
        val wirelessDebuggingOn: Boolean = false,
        val connectHost: String? = null,
        val connectPort: Int = 0,
        /** The pairing service's own resolved host + port — valid only while the pairing dialog is open on THIS device. */
        val pairingHost: String? = null,
        val pairingPort: Int = 0,
        /** How many OTHER devices on the LAN were advertising an ADB daemon (ignored — we only ever target ourselves). */
        val otherDevices: Int = 0,
        val supported: Boolean = true,
        val note: String? = null,
    )

    private val appContext = context.applicationContext
    private val nsd = appContext.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private var connectDiscovery: NsdManager.DiscoveryListener? = null
    private var pairingDiscovery: NsdManager.DiscoveryListener? = null
    private val resolveQueue = ConcurrentLinkedQueue<NsdServiceInfo>()
    private val resolving = AtomicBoolean(false)
    private val active = AtomicBoolean(false)

    // State for the running session (one scan or watch at a time).
    @Volatile private var sawConnect = false
    @Volatile private var connectHost: String? = null
    @Volatile private var connectPort = 0
    @Volatile private var pairingHost: String? = null
    @Volatile private var pairingPort = 0
    private val otherHosts = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    @Volatile private var localIps: Set<String> = emptySet()
    @Volatile private var reporter: ((Result) -> Unit)? = null

    /** One-shot detect: starts discovery, reports once after [timeoutMs], then stops. */
    fun scan(timeoutMs: Long = 4_000L, onResult: (Result) -> Unit) {
        when (begin(continuous = false)) {
            StartResult.UNSUPPORTED -> { onResult(Result(supported = false, note = unsupportedNote())); return }
            StartResult.BUSY -> { onResult(Result(note = "A scan is already running.")); return }
            StartResult.FAILED -> { onResult(Result(supported = false, note = "Couldn't start discovery.")); return }
            StartResult.OK -> {}
        }
        android.os.Handler(appContext.mainLooper).postDelayed({
            val r = snapshot()
            stop()
            onResult(r)
        }, timeoutMs)
    }

    /** Continuous watch for the pairing flow: calls [onUpdate] now and whenever the known endpoints change, until [stop]. */
    fun watch(onUpdate: (Result) -> Unit) {
        when (begin(continuous = true)) {
            StartResult.UNSUPPORTED -> { onUpdate(Result(supported = false, note = unsupportedNote())); return }
            StartResult.BUSY -> { onUpdate(Result(note = "Already watching.")); return }
            StartResult.FAILED -> { onUpdate(Result(supported = false, note = "Couldn't start discovery.")); return }
            StartResult.OK -> { reporter = onUpdate; onUpdate(snapshot()) }
        }
    }

    private enum class StartResult { OK, BUSY, UNSUPPORTED, FAILED }

    private fun begin(continuous: Boolean): StartResult {
        val manager = nsd ?: return StartResult.UNSUPPORTED
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return StartResult.UNSUPPORTED
        if (!active.compareAndSet(false, true)) return StartResult.BUSY
        sawConnect = false
        connectHost = null
        connectPort = 0
        pairingHost = null
        pairingPort = 0
        otherHosts.clear()
        resolveQueue.clear()
        resolving.set(false)
        reporter = null
        localIps = localAddresses()
        return try {
            connectDiscovery = discoveryFor(manager).also {
                manager.discoverServices("_adb-tls-connect._tcp", NsdManager.PROTOCOL_DNS_SD, it)
            }
            pairingDiscovery = discoveryFor(manager).also {
                manager.discoverServices("_adb-tls-pairing._tcp", NsdManager.PROTOCOL_DNS_SD, it)
            }
            StartResult.OK
        } catch (e: Exception) {
            Log.w(TAG, "discovery start failed", e)
            active.set(false)
            StartResult.FAILED
        }
    }

    private fun discoveryFor(manager: NsdManager): NsdManager.DiscoveryListener =
        object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                resolveQueue.add(serviceInfo)
                pumpResolves(manager)
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                // The pairing dialog was dismissed or the daemon went away: drop the stale pairing port.
                if (serviceInfo.serviceType?.contains("adb-tls-pairing") == true && pairingPort != 0) {
                    pairingHost = null
                    pairingPort = 0
                    reporter?.invoke(snapshot())
                }
            }
        }

    private fun pumpResolves(manager: NsdManager) {
        if (!resolving.compareAndSet(false, true)) return
        val next = resolveQueue.poll()
        if (next == null) {
            resolving.set(false)
            return
        }
        @Suppress("DEPRECATION")
        manager.resolveService(next, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                resolving.set(false)
                pumpResolves(manager)
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val type = serviceInfo.serviceType ?: ""
                @Suppress("DEPRECATION") val host = serviceInfo.host?.hostAddress?.substringBefore('%')
                val port = serviceInfo.port
                // Only ever accept an endpoint that belongs to THIS device.
                val isOurs = host != null && localIps.contains(host)
                var changed = false
                if (type.contains("adb-tls-connect")) {
                    if (isOurs) {
                        sawConnect = true
                        connectHost = host
                        connectPort = port
                        changed = true
                    } else if (host != null) {
                        changed = otherHosts.add(host)
                    }
                } else if (type.contains("adb-tls-pairing") && isOurs) {
                    pairingHost = host
                    pairingPort = port
                    changed = true
                }
                resolving.set(false)
                if (changed) reporter?.invoke(snapshot())
                pumpResolves(manager)
            }
        })
    }

    private fun snapshot(): Result {
        val note = when {
            sawConnect -> null
            otherHosts.isNotEmpty() ->
                "This TV isn't advertising Wireless Debugging (saw ${otherHosts.size} other device(s), ignored). " +
                    "Turn it on under Developer options → Wireless debugging."
            else -> "No Wireless Debugging service found. Turn it on under Developer options → Wireless debugging."
        }
        return Result(
            wirelessDebuggingOn = sawConnect,
            connectHost = connectHost,
            connectPort = connectPort,
            pairingHost = pairingHost,
            pairingPort = pairingPort,
            otherDevices = otherHosts.size,
            note = note,
        )
    }

    private fun unsupportedNote(): String =
        if (nsd == null) "This device has no network service discovery." else "Wireless Debugging needs Android 11 or newer."

    /** Every IP address bound to one of this device's own up interfaces, IPv6 zone id stripped, for self-matching. */
    private fun localAddresses(): Set<String> {
        val out = mutableSetOf<String>()
        try {
            for (ni in java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp) continue
                for (addr in java.util.Collections.list(ni.inetAddresses)) {
                    addr.hostAddress?.substringBefore('%')?.let { out.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "localAddresses failed", e)
        }
        return out
    }

    fun stop() {
        val manager = nsd
        reporter = null
        if (manager != null) {
            connectDiscovery?.let { runCatching { manager.stopServiceDiscovery(it) } }
            pairingDiscovery?.let { runCatching { manager.stopServiceDiscovery(it) } }
        }
        connectDiscovery = null
        pairingDiscovery = null
        resolveQueue.clear()
        resolving.set(false)
        active.set(false)
    }

    companion object {
        private const val TAG = "WirelessAdb"
    }
}
