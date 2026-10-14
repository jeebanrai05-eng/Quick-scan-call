package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CallerInfo
import com.example.data.model.CallerType
import com.example.data.model.ScanSource
import com.example.scanner.DamagedNumberRepairer
import com.example.scanner.PhoneNumberExtractor
import com.example.ui.navigation.Screen
import com.example.utils.SpamDetector
import com.example.utils.SpamRiskTier
import org.junit.Assert.assertEquals
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
}
