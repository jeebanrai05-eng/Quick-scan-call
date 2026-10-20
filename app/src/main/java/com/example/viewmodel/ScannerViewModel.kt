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
import com.example.data.model.BatchPage
import com.example.data.model.DocumentData
import com.example.data.model.ScanResult
import com.example.data.model.ScanSource
import com.example.data.repository.CallerRepository
import com.example.scanner.AiNumberScanner
import com.example.scanner.ImageEnhancer
import com.example.scanner.OcrScanner
import com.example.scanner.PhoneNumberExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

enum class ScannerEngineMode {
    TURBO_MULTI_PASS,   // 120 FPS Local CV Contrast & Binarization + Glitch Repair (Offline)
    AI_DEEP_RESTORE     // Gemini Super-Resolution AI for Blurry / Damaged numbers
}

data class ScannerUiState(
    val isScanning: Boolean = true,
    val detectedNumbers: List<ScanResult> = emptyList(),
    val latestDocument: DocumentData? = null,
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
    val cameraGuidance: OcrScanner.CameraGuidance = OcrScanner.CameraGuidance(
        message = "Align visiting card or document inside frame",
        isOptimal = false,
        hasDocumentEdges = false
    ),
    // Image Enhancement Studio State
    val capturedBitmap: Bitmap? = null,
    val enhancedBitmap: Bitmap? = null,
    val showEnhanceStudio: Boolean = false,
    val enhanceContrast: Float = 1.0f,
    val enhanceBrightness: Float = 0.0f,
    val enhanceRotation: Float = 0.0f,
    val isSharpenActive: Boolean = false,
    val isDenoiseActive: Boolean = false,
    val isGrayscaleActive: Boolean = false,
    val isInvertActive: Boolean = false,
    val isBinarizeActive: Boolean = false,
    // Batch Scanning / Multi-Page State
    val isBatchMode: Boolean = false,
    val batchPages: List<BatchPage> = emptyList(),
    val selectedPageIndex: Int = 0,
    val showBatchReviewSheet: Boolean = false,
    // Vault & Edit Dialogs
    val showVaultSheet: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingResult: ScanResult? = null,
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
        return OcrScanner.LiveImageAnalyzer { documentData, guidance ->
            viewModelScope.launch {
                val newNumbers = documentData.phoneNumbers
                if (newNumbers.isNotEmpty()) {
                    val enrichedResults = newNumbers.map { result ->
                        val caller = repository.getCaller(result.phoneNumber)
                        result.copy(callerInfo = caller)
                    }

                    val currentList = _uiState.value.detectedNumbers.toMutableList()
                    val toPersist = mutableListOf<ScanResult>()

                    for (item in enrichedResults) {
                        val exists = currentList.any {
                            PhoneNumberExtractor.normalizePhoneNumber(it.phoneNumber) ==
                                    PhoneNumberExtractor.normalizePhoneNumber(item.phoneNumber)
                        }
                        if (!exists) {
                            currentList.add(0, item)
                            toPersist.add(item)
                        }
                    }

                    if (toPersist.isNotEmpty()) {
                        repository.saveScannedNumbers(toPersist)
                    }

                    _uiState.value = _uiState.value.copy(
                        detectedNumbers = currentList.take(40),
                        latestDocument = documentData,
                        latestScannedText = documentData.fullRawText.take(200),
                        cameraGuidance = guidance
                    )

                    // Auto open dialer if enabled and first number detected
                    if (_uiState.value.autoOpenDialer &&
                        _uiState.value.autoDialTriggeredFor != enrichedResults.first().phoneNumber
                    ) {
                        _uiState.value = _uiState.value.copy(
                            autoDialTriggeredFor = enrichedResults.first().phoneNumber
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        cameraGuidance = guidance,
                        latestScannedText = if (documentData.fullRawText.isNotBlank()) {
                            documentData.fullRawText.take(150)
                        } else _uiState.value.latestScannedText
                    )
                }
            }
        }
    }

    /**
     * Executes AI Super-Resolution Scan & Repair for blurry, scratched, or faint numbers.
     */
    fun scanBitmapWithAi(bitmap: Bitmap, source: ScanSource = ScanSource.LIVE_CAMERA) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAiProcessing = true,
                message = "Analyzing with AI Neural Super-Resolution..."
            )

            // Safe downscale to protect device RAM
            val safeBitmap = withContext(Dispatchers.Default) {
                ImageEnhancer.safeDownscale(bitmap, 1800)
            }

            val (aiResults, feedback) = AiNumberScanner.analyzeImageWithAi(safeBitmap, source)

            val enriched = aiResults.map { res ->
                val caller = repository.getCaller(res.phoneNumber)
                res.copy(callerInfo = caller)
            }

            if (enriched.isNotEmpty()) {
                repository.saveScannedNumbers(enriched)
            }

            val currentList = _uiState.value.detectedNumbers.toMutableList()
            for (item in enriched) {
                val exists = currentList.any {
                    PhoneNumberExtractor.normalizePhoneNumber(it.phoneNumber) ==
                            PhoneNumberExtractor.normalizePhoneNumber(item.phoneNumber)
                }
                if (!exists) {
                    currentList.add(0, item)
                }
            }

            // Create synthesized DocumentData for AI scan
            val docData = DocumentData(
                phoneNumbers = if (enriched.isNotEmpty()) currentList else _uiState.value.detectedNumbers,
                confidence = if (enriched.isNotEmpty()) 0.98f else 0.85f,
                fullRawText = enriched.joinToString("\n") { it.rawText }
            )

            _uiState.value = _uiState.value.copy(
                isAiProcessing = false,
                detectedNumbers = if (enriched.isNotEmpty()) currentList else _uiState.value.detectedNumbers,
                latestDocument = docData,
                capturedBitmap = safeBitmap,
                message = feedback
            )
        }
    }

    /**
     * Turbo Multi-Pass on-device OCR scan with full document intelligence.
     */
    fun scanBitmapMultiPass(bitmap: Bitmap, source: ScanSource = ScanSource.LIVE_CAMERA, pageIndex: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessingImage = true,
                message = "Running Turbo Multi-Pass OCR..."
            )

            val safeBitmap = withContext(Dispatchers.Default) {
                ImageEnhancer.safeDownscale(bitmap, 1800)
            }

            val docData = withContext(Dispatchers.Default) {
                OcrScanner.processBitmapMultiPass(safeBitmap, source, pageIndex)
            }

            val enriched = docData.phoneNumbers.map { res ->
                val caller = repository.getCaller(res.phoneNumber)
                res.copy(callerInfo = caller)
            }

            if (enriched.isNotEmpty()) {
                repository.saveScannedNumbers(enriched)
            }

            val currentList = _uiState.value.detectedNumbers.toMutableList()
            for (item in enriched) {
                val exists = currentList.any {
                    PhoneNumberExtractor.normalizePhoneNumber(it.phoneNumber) ==
                            PhoneNumberExtractor.normalizePhoneNumber(item.phoneNumber)
                }
                if (!exists) {
                    currentList.add(0, item)
                }
            }

            val updatedDoc = docData.copy(phoneNumbers = enriched)

            // If in batch mode, add to batchPages
            var updatedBatchPages = _uiState.value.batchPages
            if (_uiState.value.isBatchMode) {
                val newPage = BatchPage(
                    pageIndex = updatedBatchPages.size + 1,
                    bitmap = safeBitmap,
                    documentData = updatedDoc
                )
                updatedBatchPages = updatedBatchPages + newPage
            }

            val feedback = if (enriched.isEmpty()) {
                "No phone numbers recognized. Try adjusting contrast or lighting."
            } else {
                "Restored ${enriched.size} number(s) [${docData.detectedCountry ?: "Multi-Pass"}]"
            }

            _uiState.value = _uiState.value.copy(
                isProcessingImage = false,
                detectedNumbers = currentList,
                latestDocument = updatedDoc,
                capturedBitmap = safeBitmap,
                batchPages = updatedBatchPages,
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

            val safeBitmap = withContext(Dispatchers.Default) {
                ImageEnhancer.safeDownscale(bitmap, 1800)
            }

            if (_uiState.value.engineMode == ScannerEngineMode.AI_DEEP_RESTORE) {
                scanBitmapWithAi(safeBitmap, source)
            } else {
                scanBitmapMultiPass(safeBitmap, source, pageIndex = _uiState.value.batchPages.size + 1)
            }
        }
    }

    // ==========================================
    // Interactive Image Enhancement Studio
    // ==========================================

    fun openEnhanceStudio(bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(
            capturedBitmap = bitmap,
            enhancedBitmap = bitmap,
            showEnhanceStudio = true,
            enhanceContrast = 1.0f,
            enhanceBrightness = 0.0f,
            enhanceRotation = 0.0f,
            isSharpenActive = false,
            isDenoiseActive = false,
            isGrayscaleActive = false,
            isInvertActive = false,
            isBinarizeActive = false
        )
    }

    fun closeEnhanceStudio() {
        _uiState.value = _uiState.value.copy(showEnhanceStudio = false)
    }

    fun setEnhanceContrast(contrast: Float) {
        _uiState.value = _uiState.value.copy(enhanceContrast = contrast)
        applyCurrentFilters()
    }

    fun setEnhanceBrightness(brightness: Float) {
        _uiState.value = _uiState.value.copy(enhanceBrightness = brightness)
        applyCurrentFilters()
    }

    fun rotateCapturedImage(degrees: Float = 90f) {
        val newRotation = (_uiState.value.enhanceRotation + degrees) % 360f
        _uiState.value = _uiState.value.copy(enhanceRotation = newRotation)
        applyCurrentFilters()
    }

    fun toggleSharpen() {
        _uiState.value = _uiState.value.copy(isSharpenActive = !_uiState.value.isSharpenActive)
        applyCurrentFilters()
    }

    fun toggleDenoise() {
        _uiState.value = _uiState.value.copy(isDenoiseActive = !_uiState.value.isDenoiseActive)
        applyCurrentFilters()
    }

    fun toggleGrayscale() {
        _uiState.value = _uiState.value.copy(isGrayscaleActive = !_uiState.value.isGrayscaleActive)
        applyCurrentFilters()
    }

    fun toggleInvert() {
        _uiState.value = _uiState.value.copy(isInvertActive = !_uiState.value.isInvertActive)
        applyCurrentFilters()
    }

    fun toggleBinarize() {
        _uiState.value = _uiState.value.copy(isBinarizeActive = !_uiState.value.isBinarizeActive)
        applyCurrentFilters()
    }

    fun autoPerspectiveCorrect() {
        val base = _uiState.value.capturedBitmap ?: return
        viewModelScope.launch {
            val corrected = withContext(Dispatchers.Default) {
                val edgeRect = ImageEnhancer.detectDocumentEdges(base)
                ImageEnhancer.crop(base, edgeRect, isNormalized = true)
            }
            _uiState.value = _uiState.value.copy(capturedBitmap = corrected, enhancedBitmap = corrected)
            applyCurrentFilters()
        }
    }

    private fun applyCurrentFilters() {
        val base = _uiState.value.capturedBitmap ?: return
        viewModelScope.launch {
            val processed = withContext(Dispatchers.Default) {
                var working = base
                if (_uiState.value.enhanceRotation != 0f) {
                    working = ImageEnhancer.rotate(working, _uiState.value.enhanceRotation)
                }
                if (_uiState.value.enhanceContrast != 1.0f || _uiState.value.enhanceBrightness != 0.0f) {
                    working = ImageEnhancer.enhanceContrast(
                        working,
                        contrast = _uiState.value.enhanceContrast,
                        brightness = _uiState.value.enhanceBrightness
                    )
                }
                if (_uiState.value.isGrayscaleActive) {
                    working = ImageEnhancer.enhanceGrayscale(working)
                }
                if (_uiState.value.isSharpenActive) {
                    working = ImageEnhancer.sharpen(working)
                }
                if (_uiState.value.isDenoiseActive) {
                    working = ImageEnhancer.denoise(working)
                }
                if (_uiState.value.isInvertActive) {
                    working = ImageEnhancer.invertColors(working)
                }
                if (_uiState.value.isBinarizeActive) {
                    working = ImageEnhancer.binarize(working)
                }
                working
            }
            _uiState.value = _uiState.value.copy(enhancedBitmap = processed)
        }
    }

    fun applyEnhancementsAndRescan() {
        val enhanced = _uiState.value.enhancedBitmap ?: _uiState.value.capturedBitmap ?: return
        _uiState.value = _uiState.value.copy(showEnhanceStudio = false)
        scanBitmapMultiPass(enhanced, _uiState.value.activeScanSource, pageIndex = _uiState.value.batchPages.size + 1)
    }

    // ==========================================
    // Batch Scanning / Multi-Page
    // ==========================================

    fun toggleBatchMode() {
        val newMode = !_uiState.value.isBatchMode
        _uiState.value = _uiState.value.copy(
            isBatchMode = newMode,
            message = if (newMode) "Multi-Page Batch Mode ON (Each capture adds a page)" else "Single Shot Mode active"
        )
    }

    fun removeBatchPage(index: Int) {
        val current = _uiState.value.batchPages.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _uiState.value = _uiState.value.copy(
                batchPages = current,
                selectedPageIndex = current.indices.lastOrNull() ?: 0
            )
        }
    }

    fun clearBatch() {
        _uiState.value = _uiState.value.copy(
            batchPages = emptyList(),
            selectedPageIndex = 0,
            showBatchReviewSheet = false
        )
    }

    fun toggleBatchReviewSheet(show: Boolean? = null) {
        _uiState.value = _uiState.value.copy(
            showBatchReviewSheet = show ?: !_uiState.value.showBatchReviewSheet
        )
    }

    fun selectBatchPage(index: Int) {
        _uiState.value = _uiState.value.copy(selectedPageIndex = index)
    }

    // ==========================================
    // Result Editing & Management
    // ==========================================

    fun openEditDialog(result: ScanResult) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            editingResult = result
        )
    }

    fun closeEditDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDialog = false,
            editingResult = null
        )
    }

    fun updateScannedResult(updated: ScanResult) {
        val current = _uiState.value.detectedNumbers.toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            current[index] = updated
        } else {
            current.add(0, updated)
        }

        viewModelScope.launch {
            repository.saveScannedNumber(updated)
        }

        _uiState.value = _uiState.value.copy(
            detectedNumbers = current,
            showEditDialog = false,
            editingResult = null,
            message = "Updated: ${updated.formattedNumber}"
        )
    }

    fun deleteDetectedNumber(resultId: String) {
        val current = _uiState.value.detectedNumbers.filterNot { it.id == resultId }
        _uiState.value = _uiState.value.copy(detectedNumbers = current)
    }

    // ==========================================
    // Export Data Formats (VCard, CSV, Text)
    // ==========================================

    fun generateVCardContent(): String {
        val sb = StringBuilder()
        val items = _uiState.value.detectedNumbers
        for (item in items) {
            sb.appendLine("BEGIN:VCARD")
            sb.appendLine("VERSION:3.0")
            val name = item.contactName ?: item.callerInfo?.name ?: "Scanned Contact"
            sb.appendLine("FN:$name")
            sb.appendLine("TEL;TYPE=CELL:${item.phoneNumber}")
            if (!item.businessName.isNullOrBlank()) {
                sb.appendLine("ORG:${item.businessName}")
            }
            if (!item.email.isNullOrBlank()) {
                sb.appendLine("EMAIL:${item.email}")
            }
            if (!item.address.isNullOrBlank()) {
                sb.appendLine("ADR:;;${item.address};;;;")
            }
            if (!item.website.isNullOrBlank()) {
                sb.appendLine("URL:${item.website}")
            }
            sb.appendLine("NOTE:Scanned via Quick Call Scanner (${item.label})")
            sb.appendLine("END:VCARD")
        }
        return sb.toString()
    }

    fun generateCsvContent(): String {
        val sb = StringBuilder()
        sb.appendLine("Contact Name,Phone Number,Formatted,Label,Business,Email,Website,Address,Country,Confidence")
        for (item in _uiState.value.detectedNumbers) {
            val name = (item.contactName ?: item.callerInfo?.name ?: "").replace(",", " ")
            val phone = item.phoneNumber
            val formatted = item.formattedNumber.replace(",", " ")
            val label = item.label.replace(",", " ")
            val business = (item.businessName ?: "").replace(",", " ")
            val email = (item.email ?: "").replace(",", " ")
            val website = (item.website ?: "").replace(",", " ")
            val address = (item.address ?: "").replace(",", " ")
            val country = (item.countryName ?: item.countryCode ?: "").replace(",", " ")
            val conf = "${(item.confidence * 100).toInt()}%"
            sb.appendLine("$name,$phone,$formatted,$label,$business,$email,$website,$address,$country,$conf")
        }
        return sb.toString()
    }

    fun getFormattedBatchSummaryText(): String {
        val sb = StringBuilder()
        sb.appendLine("📋 QUICK CALL SCANNER EXPORT")
        sb.appendLine("Total Numbers: ${_uiState.value.detectedNumbers.size}")
        val doc = _uiState.value.latestDocument
        if (doc?.businessName != null) sb.appendLine("🏢 Business: ${doc.businessName}")
        if (doc?.contactName != null) sb.appendLine("👤 Contact: ${doc.contactName}")
        if (doc?.emails?.isNotEmpty() == true) sb.appendLine("📧 Email(s): ${doc.emails.joinToString(", ")}")
        if (doc?.websites?.isNotEmpty() == true) sb.appendLine("🌐 Web: ${doc.websites.joinToString(", ")}")
        if (doc?.addresses?.isNotEmpty() == true) sb.appendLine("📍 Address: ${doc.addresses.joinToString("; ")}")
        sb.appendLine("------------------------------")
        for ((idx, item) in _uiState.value.detectedNumbers.withIndex()) {
            val country = item.countryName?.let { " ($it)" } ?: ""
            sb.appendLine("${idx + 1}. ${item.formattedNumber}$country [${item.label}]")
        }
        return sb.toString()
    }

    // ==========================================
    // Vault, Torch & Controls
    // ==========================================

    fun toggleVaultSheet() {
        _uiState.value = _uiState.value.copy(showVaultSheet = !_uiState.value.showVaultSheet)
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
        _uiState.value = _uiState.value.copy(
            detectedNumbers = emptyList(),
            latestDocument = null,
            latestScannedText = ""
        )
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
