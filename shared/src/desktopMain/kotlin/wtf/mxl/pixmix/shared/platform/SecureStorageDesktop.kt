package wtf.mxl.pixmix.shared.platform

import java.util.prefs.Preferences

/**
 * Desktop secure-ish storage. Uses java.util.prefs which on Linux writes to
 * `~/.java/.userPrefs/wtf/mxl/pixmix/secure/` with default permissions. Not
 * encrypted at rest — same trust model as the app's own data dir. Good enough
 * for a personal-use client; if/when a hardened option is needed, look at
 * libsecret / KWallet bindings.
 */
class SecureStorageDesktop : SecureStorage {

    private val prefs: Preferences = Preferences.userRoot().node("wtf/mxl/pixmix/secure")

    override fun getString(key: String): String? = prefs.get(key, null)

    override fun putString(key: String, value: String) {
        prefs.put(key, value)
        prefs.flush()
    }

    override fun remove(key: String) {
        prefs.remove(key)
        prefs.flush()
    }

    override fun clear() {
        prefs.clear()
        prefs.flush()
    }
}
