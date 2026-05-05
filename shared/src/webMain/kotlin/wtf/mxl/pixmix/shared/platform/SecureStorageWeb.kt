package wtf.mxl.pixmix.shared.platform

import kotlinx.browser.localStorage
import org.w3c.dom.get
import org.w3c.dom.set

/**
 * Web `SecureStorage` is not encrypted at rest — `localStorage` is plain text. Same trust
 * model as the desktop java.util.prefs impl. The web build's threat model is "single-user
 * laptop, browser-attached devtools is acceptable" — anything more sensitive shouldn't go
 * through a frontend in the first place.
 */
class SecureStorageWeb : SecureStorage {

    override fun getString(key: String): String? = localStorage[ns(key)]

    override fun putString(key: String, value: String) {
        localStorage[ns(key)] = value
    }

    override fun remove(key: String) {
        localStorage.removeItem(ns(key))
    }

    override fun clear() {
        // Only clear our own keys, not unrelated ones the user/app might have.
        val ours = (0 until localStorage.length)
            .mapNotNull { localStorage.key(it) }
            .filter { it.startsWith(PREFIX) }
        ours.forEach { localStorage.removeItem(it) }
    }

    private fun ns(key: String) = "$PREFIX$key"

    companion object {
        private const val PREFIX = "pixmix_secure."
    }
}
