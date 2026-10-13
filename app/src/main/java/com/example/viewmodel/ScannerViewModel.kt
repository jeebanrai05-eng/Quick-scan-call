package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.QuickCallApp
import com.example.data.model.ScanResult
import com.example.data.model.ScanSource
import com.example.data.repository.CallerRepository
import com.example.scanner.AiNumberScanner
import com.example.scanner.OcrScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

enum class ScannerEngineMode {
    TURBO_MULTI_PASS,   // 120 FPS Local CV Contrast & Binarization + Glitch Repair
    AI_DEEP_RESTORE     // Gemini Super-Resolution AI for Blurry / Damaged numbers
}

data class ScannerUiState(
    val isScanning: Boolean = true,
    val detectedNumbers: List<ScanResult> = emptyList(),
    val latestScannedText: String = "",
    val activeScanSource: ScanSource = ScanSource.LIVE_CAMERA,
    val engineMode: ScannerEngineMode = ScannerEngineMode.TURBO_MULTI_PASS,
    val autoOpenDialer: Boolean = false,
    val isTorchOn: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val isProcessingImage: Boolean = false,
    val isAiProcessing: Boolean = false,
    val isAiConfigured: Boolean = AiNumberScanner.isAiAvailable(),
    val autoDialTriggeredFor: String? = null,
    val message: String? = null
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CallerRepository = (application as QuickCallApp).repository

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    val scanVault: StateFlow<List<ScanResult>> = repository.scanHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createCameraAnalyzer(): ImageAnalysis.Analyzer {
        return OcrScanner.LiveImageAnalyzer { scanResults, fullText ->
            if (scanResults.isNotEmpty()) {
                viewModelScope.launch {
                    val enrichedResults = scanResults.map { result ->
                        val caller = repository.getCaller(result.phoneNumber)
                        result.copy(callerInfo = caller)
                    }

                    val currentList = _uiState.value.detectedNumbers.toMutableList()
                    val newItems = mutableListOf<ScanResult>()

                    for (item in enrichedResults) {
                        if (currentList.none { it.formattedNumber == item.formattedNumber }) {
                            currentList.add(0, item)
                            newItems.add(item)
                        }
                    }

                    if (newItems.isNotEmpty()) {
                        repository.saveScannedNumbers(newItems)
                    }

                    _uiState.value = _uiState.value.copy(
                        detectedNumbers = currentList.take(30),
                        latestScannedText = fullText.take(200)
                    )

                    // Auto open dialer if enabled and first number detected
                    if (_uiState.value.autoOpenDialer &&
                        _uiState.value.autoDialTriggeredFor != enrichedResults.first().phoneNumber
                    ) {
                        _uiState.value = _uiState.value.copy(
                            autoDialTriggeredFor = enrichedResults.first().phoneNumber
                        )
                    }
                }
            } else if (fullText.isNotBlank()) {
                _uiState.value = _uiState.value.copy(latestScannedText = fullText.take(150))
            }
        }
    }

    /**
     * Executes AI Super-Resolution Scan & Repair for blurry, scratched, or faint numbers.
     */
    fun scanBitmapWithAi(bitmap: Bitmap, source: ScanSource = ScanSource.LIVE_CAMERA) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiProcessing = true, message = "Analyzing with AI Vision...")
            val (aiResults, feedback) = AiNumberScanner.analyzeImageWithAi(bitmap, source)

            val enriched = aiResults.map { res ->
                val caller = repository.getCaller(res.phoneNumber)
                res.copy(callerInfo = caller)
            }

            if (enriched.isNotEmpty()) {
                repository.saveScannedNumbers(enriched)
            }

            val currentList = _uiState.value.detectedNumbers.toMutableList()
            for (item in enriched) {
                if (currentList.none { it.formattedNumber == item.formattedNumber }) {
                    currentList.add(0, item)
                }
            }

            _uiState.value = _uiState.value.copy(
                isAiProcessing = false,
                detectedNumbers = if (enriched.isNotEmpty()) currentList else _uiState.value.detectedNumbers,
                message = feedback
            )
        }
    }

    fun scanImageUri(context: Context, uri: Uri, source: ScanSource) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingImage = true, activeScanSource = source)

            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    var inputStream: InputStream? = null
                    try {
                        inputStream = context.contentResolver.openInputStream(uri)
                        BitmapFactory.decodeStream(inputStream)
                    } finally {
                        inputStream?.close()
                    }
                }.getOrNull()
            }

            if (bitmap == null) {
                _uiState.value = _uiState.value.copy(
                    isProcessingImage = false,
                    message = "Could not load selected image"
                )
                return@launch
            }

            val (results, feedback) = if (_uiState.value.engineMode == ScannerEngineMode.AI_DEEP_RESTORE) {
                val (aiResults, msg) = AiNumberScanner.analyzeImageWithAi(bitmap, source)
                Pair(aiResults, msg)
            } else {
                val multiPassResults = OcrScanner.processBitmapMultiPass(bitmap, source)
                val msg = if (multiPassResults.isEmpty()) "No numbers recognized" else "Turbo Multi-Pass restored ${multiPassResults.size} number(s)"
                Pair(multiPassResults, msg)
            }

            val enriched = results.map { res ->
                val caller = repository.getCaller(res.phoneNumber)
                res.copy(callerInfo = caller)
            }

            if (enriched.isNotEmpty()) {
                repository.saveScannedNumbers(enriched)
            }

            _uiState.value = _uiState.value.copy(
                isProcessingImage = false,
                detectedNumbers = enriched,
                message = feedback
            )
        }
    }

    fun toggleEngineMode() {
        val newMode = if (_uiState.value.engineMode == ScannerEngineMode.TURBO_MULTI_PASS) {
            ScannerEngineMode.AI_DEEP_RESTORE
        } else {
            ScannerEngineMode.TURBO_MULTI_PASS
        }
        _uiState.value = _uiState.value.copy(
            engineMode = newMode,
            message = if (newMode == ScannerEngineMode.AI_DEEP_RESTORE) {
                "Switched to AI Super-Resolution (Deep blur & damage restoration)"
            } else {
                "Switched to Turbo Multi-Pass (Instant 120 FPS Local Engine)"
            }
        )
    }

    fun toggleTorch() {
        _uiState.value = _uiState.value.copy(isTorchOn = !_uiState.value.isTorchOn)
    }

    fun setZoomRatio(ratio: Float) {
        _uiState.value = _uiState.value.copy(zoomRatio = ratio.coerceIn(1.0f, 4.0f))
    }

    fun toggleAutoDial() {
        _uiState.value = _uiState.value.copy(autoOpenDialer = !_uiState.value.autoOpenDialer)
    }

    fun setScanSource(source: ScanSource) {
        _uiState.value = _uiState.value.copy(activeScanSource = source)
    }

    fun clearDetected() {
        _uiState.value = _uiState.value.copy(detectedNumbers = emptyList(), latestScannedText = "")
    }

    fun deleteFromVault(phoneNumber: String) {
        viewModelScope.launch {
            repository.deleteScannedNumber(phoneNumber)
        }
    }

    fun clearVault() {
        viewModelScope.launch {
            repository.clearScanHistory()
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun resetAutoDialTrigger() {
        _uiState.value = _uiState.value.copy(autoDialTriggeredFor = null)
    }
}
