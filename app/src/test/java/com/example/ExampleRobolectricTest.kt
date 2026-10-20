package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CallerInfo
import com.example.data.model.CallerType
import com.example.data.model.ScanSource
import com.example.scanner.DamagedNumberRepairer
import com.example.scanner.ImageEnhancer
import com.example.scanner.PhoneNumberExtractor
import com.example.ui.navigation.Screen
import com.example.utils.SpamDetector
import com.example.utils.SpamRiskTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun readStringFromContext() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Quick Call Scan", appName)
  }

  @Test
  fun testScreenBottomNavItemsNoNullPointerWhenScannerAccessedFirst() {
    val scannerRoute = Screen.Scanner.route
    assertEquals("scanner", scannerRoute)
    val routes = Screen.bottomNavItems.map {
      assertNotNull("Screen item in bottomNavItems must not be null", it)
      it.route
    }
    assertTrue(routes.contains("scanner"))
    assertTrue(routes.contains("home"))
  }

  @Test
  fun testPhoneNumberExtractorAndDamagedRepairer() {
    val sampleText = """
        ACME CORP
        John Doe - Sales Manager
        Tel: +1 (555) 234-5678
        WhatsApp: 984l2S4S67
        Visit us at www.example.com
    """.trimIndent()

    val extracted = PhoneNumberExtractor.extractPhoneNumbers(sampleText, ScanSource.VISITING_CARD)
    assertTrue("Should extract at least one phone number", extracted.isNotEmpty())

    val repaired = DamagedNumberRepairer.repairAndExtract(sampleText, ScanSource.VISITING_CARD)
    assertTrue("Should repair corrupted number 984l2S4S67 -> 9841254567", repaired.any { it.phoneNumber.contains("9841254567") })
  }

  @Test
  fun testKuwaitAndNepalPhoneExtractionAndDeduplication() {
    val kuwaitCardText = """
        AL-SULTAN TRADING W.L.L.
        Ahmed Al-Sabah - General Manager
        Salmiya, Block 10, Kuwait
        Tel: +965 91234567
        Direct: 91234567
        Email: info@alsultan-kw.com
        Web: www.alsultan-kw.com
    """.trimIndent()

    val doc = PhoneNumberExtractor.extractFullDocument(kuwaitCardText, ScanSource.VISITING_CARD)
    assertEquals("Kuwait 🇰🇼", doc.detectedCountry)
    assertEquals("+965", doc.detectedCountryCode)
    assertEquals("AL-SULTAN TRADING W.L.L.", doc.businessName)
    assertEquals("Ahmed Al-Sabah", doc.contactName)
    assertTrue(doc.emails.contains("info@alsultan-kw.com"))
    assertTrue(doc.websites.any { it.contains("alsultan-kw.com") })
    // Verify duplicate +965 91234567 and 91234567 were merged into exactly 1 entry!
    assertEquals("Duplicate numbers must be consolidated", 1, doc.phoneNumbers.size)

    val nepalDocText = """
        Himalayan Trekking Pvt. Ltd.
        Er. Pasang Sherpa
        Thamel, Kathmandu, Nepal
        Tel: +977 9841234567
        Office: 01-4412345
    """.trimIndent()

    val nepalDoc = PhoneNumberExtractor.extractFullDocument(nepalDocText, ScanSource.VISITING_CARD)
    assertEquals("Nepal 🇳🇵", nepalDoc.detectedCountry)
    assertEquals("+977", nepalDoc.detectedCountryCode)
    assertTrue("Should extract Nepal mobile & landline", nepalDoc.phoneNumbers.size >= 2)
  }

  @Test
  fun testInvalidFormatRejection() {
    // Dates should NOT be extracted as phone numbers
    assertFalse(PhoneNumberExtractor.isValidPhoneNumber("20261019", "2026-10-19", "Date: 2026-10-19"))
    assertFalse(PhoneNumberExtractor.isValidPhoneNumber("19102026", "19/10/2026", "Date: 19/10/2026"))

    // Times should NOT be extracted
    assertFalse(PhoneNumberExtractor.isValidPhoneNumber("1230", "12:30 pm", "Time: 12:30 pm"))

    // Currencies with cents should NOT be extracted
    assertFalse(PhoneNumberExtractor.isValidPhoneNumber("15000", "150.00", "Total KD 150.00"))

    // Dummy repeated sequences (e.g. 00000000) or sequential digits (12345678) should NOT be extracted
    assertFalse(PhoneNumberExtractor.isValidPhoneNumber("00000000", "00000000", "00000000"))
    assertFalse(PhoneNumberExtractor.isValidPhoneNumber("12345678", "12345678", "12345678"))
  }

  @Test
  fun testImageEnhancementOperations() {
    val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    // Test safe downscale
    val downscaled = ImageEnhancer.safeDownscale(testBitmap, maxDim = 50)
    assertEquals(50, downscaled.width)
    assertEquals(50, downscaled.height)

    // Test rotate
    val rotated = ImageEnhancer.rotate(testBitmap, 90f)
    assertNotNull(rotated)

    // Test contrast & brightness
    val contrastEnhanced = ImageEnhancer.enhanceContrast(testBitmap, contrast = 1.5f, brightness = 10f)
    assertNotNull(contrastEnhanced)

    // Test sharpen & denoise
    val sharpened = ImageEnhancer.sharpen(testBitmap)
    assertNotNull(sharpened)
    val denoised = ImageEnhancer.denoise(testBitmap)
    assertNotNull(denoised)

    // Test grayscale & binarize
    val grayscale = ImageEnhancer.enhanceGrayscale(testBitmap)
    assertNotNull(grayscale)
    val binarized = ImageEnhancer.binarize(testBitmap)
    assertNotNull(binarized)
  }

  @Test
  fun testSpamDetector() {
    val fraudCaller = CallerInfo(
        phoneNumber = "+1 (888) 998-1234",
        normalizedNumber = "18889981234",
        name = "IRS Urgent Scam",
        callerType = CallerType.SPAM,
        spamScore = 95,
        reportsCount = 300
    )

    val analysis = SpamDetector.analyze(fraudCaller)
    assertEquals(SpamRiskTier.SEVERE_FRAUD, analysis.tier)
    assertTrue(analysis.shouldAutoBlock)
  }

  @Test
  fun testApkVerifierRejectsNonExistentFile() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val nonExistent = java.io.File("/tmp/non_existent.apk")
    val result = com.example.utils.ApkVerifier.verifyApk(context, nonExistent)
    assertTrue("Should fail on missing APK file", result is com.example.utils.ApkVerifier.VerificationResult.Error)
  }

  @Test
  fun testApkVerifierIntegrityChecksumMismatch() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tempFile = java.io.File.createTempFile("test_apk", ".apk")
    tempFile.writeText("fake apk binary content")
    try {
      val result = com.example.utils.ApkVerifier.verifyApk(context, tempFile, "0000000000000000000000000000000000000000000000000000000000000000")
      assertTrue("Should fail on archive parse or checksum", result is com.example.utils.ApkVerifier.VerificationResult.Error)
    } finally {
      tempFile.delete()
    }
  }

  @Test
  fun testCallDetectionManagerIncomingCallProcessing() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    // Verify starting listener and handling incoming call does not crash
    com.example.service.CallDetectionManager.startListening(context)
    com.example.service.CallDetectionManager.handleCallState(
        context = context,
        state = android.telephony.TelephonyManager.CALL_STATE_RINGING,
        incomingNumber = "+1 (800) 555-0199"
    )
    com.example.service.CallDetectionManager.handleCallState(
        context = context,
        state = android.telephony.TelephonyManager.CALL_STATE_IDLE,
        incomingNumber = null
    )
    com.example.service.CallDetectionManager.stopListening()
  }
}
