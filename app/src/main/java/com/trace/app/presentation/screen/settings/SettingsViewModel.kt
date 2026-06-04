package com.trace.app.presentation.screen.settings

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trace.app.domain.repository.TrafficRepository
import com.trace.app.proxy.TlsInterceptor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tlsInterceptor: TlsInterceptor,
    private val trafficRepository: TrafficRepository
) : ViewModel() {

    private val _hasCACert = MutableStateFlow(false)
    val hasCACert = _hasCACert.asStateFlow()

    init {
        _hasCACert.value = tlsInterceptor.hasCACertificate()
    }

    fun exportCertificate(context: Context) {
        viewModelScope.launch {
            try {
                val certFile = tlsInterceptor.exportCACertificate()
                if (certFile == null) {
                    Timber.e("Failed to export CA certificate")
                    return@launch
                }

                // Save to Downloads using MediaStore (Android 10+)
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "trace_ca.pem")
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/x-pem-file")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Download")
                    }
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        certFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Timber.i("Certificate exported to Downloads/trace_ca.pem")

                    // Show share sheet as confirmation
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/x-pem-file"
                        putExtra(Intent.EXTRA_STREAM, it)
                        putExtra(Intent.EXTRA_TEXT, "Certificate saved to Downloads folder as trace_ca.pem")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Certificate Exported"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error exporting certificate")
            }
        }
    }

    fun regenerateCertificate() {
        // Certificate is auto-generated on first launch
        _hasCACert.value = tlsInterceptor.hasCACertificate()
    }

    fun clearAllTraffic() {
        viewModelScope.launch {
            trafficRepository.clearAll()
        }
    }
}
