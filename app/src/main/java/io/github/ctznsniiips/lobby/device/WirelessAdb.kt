package io.github.ctznsniiips.lobby.device

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Experimental: finds Android's Wireless Debugging (ADB-over-TLS) endpoints on this device using mDNS.
 *
 * This is the first, standalone step of the self-update-over-ADB spike. Before Lobby can ever pair with and drive the
 * on-device ADB daemon to install its own updates, it has to prove it can even *see* that daemon — and find the host
 * and port Android assigns it, which are randomized and not shown anywhere the user can copy. This class answers only
 * that question. Pairing (SPAKE2 + TLS) and the ADB wire protocol come in a later phase on top of a vetted library.
 *
 * Android advertises two Bonjour/mDNS service types on the loopback/LAN while Wireless Debugging is on:
 *   - "_adb-tls-connect._tcp"  — the connectable daemon; present whenever Wireless Debugging is enabled.
 *   - "_adb-tls-pairing._tcp"  — only while the "Pair device with pairing code" dialog is actually open.
 *
 * So discovering the connect service tells us Wireless Debugging is on and hands us host:port; discovering the pairing
 * service tells us the user currently has the pairing dialog up (and its one-shot port).
 */
class WirelessAdb(context: Context) {

    data class Result(
        /** True if THIS device's own "_adb-tls-connect._tcp" service was seen — i.e. our Wireless Debugging is enabled. */
        val wirelessDebuggingOn: Boolean = false,
        val connectHost: String? = null,
        val connectPort: Int = 0,
        /** Non-zero only while the pairing dialog is open on THIS device. */
        val pairingPort: Int = 0,
        /** How many OTHER devices on the LAN were advertising an ADB daemon (ignored — we only ever target ourselves). */
        val otherDevices: Int = 0,
        val supported: Boolean = true,
        val note: String? = null,
    )

    private val appContext = context.applicationContext
    private val nsd = appContext.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val connectListener = AtomicBoolean(false)
    private var connectDiscovery: NsdManager.DiscoveryListener? = null
    private var pairingDiscovery: NsdManager.DiscoveryListener? = null

    // Resolves have to be serialized: on API < 34 a second concurrent resolve fails with FAILURE_ALREADY_ACTIVE.
    private val resolveQueue = ConcurrentLinkedQueue<NsdServiceInfo>()
    private val resolving = AtomicBoolean(false)

    /**
     * Runs discovery for [timeoutMs] and reports once with whatever was found. Safe to call again after it reports.
     * [onResult] is posted off the NSD callback thread; callers should hop to their own thread/state as needed.
     */
    fun scan(timeoutMs: Long = 4_000L, onResult: (Result) -> Unit) {
        val manager = nsd
        if (manager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            onResult(
                Result(
                    supported = false,
                    note = if (manager == null) "This device has no network service discovery."
                    else "Wireless Debugging needs Android 11 or newer.",
                )
            )
            return
        }
        if (!connectListener.compareAndSet(false, true)) {
            onResult(Result(note = "A scan is already running."))
            return
        }

        var connectHost: String? = null
        var connectPort = 0
        var pairingPort = 0
        var sawConnect = false
        val otherHosts = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        val localIps = localAddresses()
        val done = AtomicBoolean(false)

        fun finish() {
            if (!done.compareAndSet(false, true)) return
            stop()
            connectListener.set(false)
            val note = when {
                sawConnect -> null
                otherHosts.isNotEmpty() ->
                    "This TV isn't advertising Wireless Debugging (saw ${otherHosts.size} other device(s), ignored). " +
                        "Turn it on under Developer options → Wireless debugging."
                else -> "No Wireless Debugging service found. Turn it on under Developer options → Wireless debugging."
            }
            onResult(
                Result(
                    wirelessDebuggingOn = sawConnect,
                    connectHost = connectHost,
                    connectPort = connectPort,
                    pairingPort = pairingPort,
                    otherDevices = otherHosts.size,
                    note = note,
                )
            )
        }

        fun pumpResolves() {
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
                    pumpResolves()
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val type = serviceInfo.serviceType ?: ""
                    @Suppress("DEPRECATION") val host = serviceInfo.host?.hostAddress?.substringBefore('%')
                    val port = serviceInfo.port
                    // Only ever accept an endpoint that belongs to THIS device: mDNS surfaces every device on the LAN
                    // with Wireless Debugging on, but Lobby can only install on itself, so a neighbour's daemon is noise.
                    val isOurs = host != null && localIps.contains(host)
                    if (type.contains("adb-tls-connect")) {
                        if (isOurs) {
                            sawConnect = true
                            connectHost = host
                            connectPort = port
                        } else if (host != null) {
                            otherHosts.add(host)
                        }
                    } else if (type.contains("adb-tls-pairing") && isOurs) {
                        pairingPort = port
                    }
                    resolving.set(false)
                    pumpResolves()
                }
            })
        }

        fun discoveryFor(type: String): NsdManager.DiscoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                // Resolve it to learn its host/port; onServiceResolved decides whether it's ours.
                resolveQueue.add(serviceInfo)
                pumpResolves()
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
        }

        try {
            connectDiscovery = discoveryFor("_adb-tls-connect._tcp").also {
                manager.discoverServices("_adb-tls-connect._tcp", NsdManager.PROTOCOL_DNS_SD, it)
            }
            pairingDiscovery = discoveryFor("_adb-tls-pairing._tcp").also {
                manager.discoverServices("_adb-tls-pairing._tcp", NsdManager.PROTOCOL_DNS_SD, it)
            }
        } catch (e: Exception) {
            Log.w(TAG, "discovery start failed", e)
            connectListener.set(false)
            onResult(Result(supported = false, note = "Couldn't start discovery: ${e.message}"))
            return
        }

        // One-shot: report whatever we have after the window closes.
        android.os.Handler(appContext.mainLooper).postDelayed({ finish() }, timeoutMs)
    }

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
        val manager = nsd ?: return
        connectDiscovery?.let { runCatching { manager.stopServiceDiscovery(it) } }
        pairingDiscovery?.let { runCatching { manager.stopServiceDiscovery(it) } }
        connectDiscovery = null
        pairingDiscovery = null
        resolveQueue.clear()
        resolving.set(false)
    }

    companion object {
        private const val TAG = "WirelessAdb"
    }
}
