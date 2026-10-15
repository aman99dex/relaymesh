package io.github.aman99dex.relaymesh

import android.app.Application
import io.github.aman99dex.relaymesh.core.Identity
import java.io.File

/** Manual wiring: no DI framework until the object graph actually needs one. */
class RelayMeshApp : Application() {
    val db by lazy { AppDb.open(this) }

    val identity by lazy {
        Identity.fromPrivateKey(KeyVault.secret(File(filesDir, "identity.key")) { Identity.generate().privateKeyBytes() })
    }

    private val prefs by lazy { getSharedPreferences("profile", MODE_PRIVATE) }
    var displayName: String
        get() = prefs.getString("name", "").orEmpty()
        set(value) = prefs.edit().putString("name", value).apply()
}
