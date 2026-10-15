package io.github.aman99dex.relaymesh

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import io.github.aman99dex.relaymesh.core.ContactCard
import io.github.aman99dex.relaymesh.core.fingerprint
import io.github.aman99dex.relaymesh.core.fromB64
import io.github.aman99dex.relaymesh.core.toB64
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No screenshots, and no preview in the recent-apps screen.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        val app = application as RelayMeshApp
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Scaffold { padding -> Home(app, Modifier.padding(padding)) }
            }
        }
    }
}

@Composable
private fun Home(app: RelayMeshApp, modifier: Modifier) {
    var name by remember { mutableStateOf(app.displayName) }
    if (name.isBlank()) {
        NameSetup(modifier) { app.displayName = it; name = it }
        return
    }

    val contacts by app.db.contacts().all().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    val myCard = remember(name) { ContactCard(name, app.identity.publicKey).encode(app.identity) }

    val scan = rememberLauncherForActivityResult(ScanContract()) { result ->
        val text = result.contents ?: return@rememberLauncherForActivityResult // user cancelled
        val card = ContactCard.decode(text)
        message = when {
            card == null -> "This isn't a valid RelayMesh contact code."
            card.publicKey.contentEquals(app.identity.publicKey) -> "This is your own code."
            else -> {
                scope.launch { app.db.contacts().upsert(Contact(card.publicKey.toB64(), card.name, System.currentTimeMillis())) }
                "Added ${card.name}. Compare fingerprints in person to verify."
            }
        }
    }

    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("You: $name", style = MaterialTheme.typography.titleLarge)
                Image(remember(myCard) { qrBitmap(myCard).asImageBitmap() }, "Your contact code", Modifier.size(260.dp))
                Fingerprint(app.identity.publicKey)
                Button(onClick = {
                    scan.launch(ScanOptions().setPrompt("Scan a RelayMesh contact code").setBeepEnabled(false).setOrientationLocked(false))
                }) { Text("Scan contact") }
                message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        item { Text("Contacts (${contacts.size})", style = MaterialTheme.typography.titleMedium) }
        items(contacts, key = { it.publicKey }) { c ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(c.name, style = MaterialTheme.typography.titleSmall)
                    Fingerprint(c.publicKey.fromB64())
                }
            }
        }
    }
}

@Composable
private fun Fingerprint(publicKey: ByteArray) =
    Text(fingerprint(publicKey), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)

@Composable
private fun NameSetup(modifier: Modifier, onDone: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    val trimmed = input.trim()
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Welcome to RelayMesh", style = MaterialTheme.typography.headlineSmall)
        Text("This phone creates your identity as a key pair. You don't need a phone number or an account. Choose the name your contacts will see.")
        OutlinedTextField(input, { if (it.length <= ContactCard.MAX_NAME) input = it }, label = { Text("Display name") }, singleLine = true)
        Button(onClick = { onDone(trimmed) }, enabled = trimmed.isNotEmpty()) { Text("Continue") }
    }
}

private fun qrBitmap(text: String, size: Int = 512): Bitmap {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val px = IntArray(size * size) { if (m[it % size, it / size]) Color.BLACK else Color.WHITE }
    return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
}
