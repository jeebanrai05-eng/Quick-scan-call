package com.example.data.repository

import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract
import com.example.data.model.CallRecord
import com.example.data.model.CallType
import com.example.data.model.CallerInfo
import com.example.data.model.CallerType
import com.example.data.model.SpamReport
import com.example.database.AppDatabase
import com.example.database.entity.CallRecordEntity
import com.example.database.entity.CallerEntity
import com.example.database.entity.SpamEntity
import com.example.scanner.PhoneNumberExtractor
import com.example.utils.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CallerRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    private val callerDao = database.callerDao()
    private val callRecordDao = database.callRecordDao()
    private val spamDao = database.spamDao()

    val allCallers: Flow<List<CallerInfo>> = callerDao.getAllCallers()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    val allRecords: Flow<List<CallRecord>> = callRecordDao.getAllRecords()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    val spamReports: Flow<List<SpamReport>> = spamDao.getAllSpamReports()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    val blockedCallers: Flow<List<CallerInfo>> = callerDao.getBlockedCallers()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    val favoriteCallers: Flow<List<CallerInfo>> = callerDao.getFavoriteCallers()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    suspend fun toggleFavorite(phoneNumber: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        val normalized = PhoneNumberExtractor.normalizePhoneNumber(phoneNumber)
        val existing = callerDao.getCallerByNormalizedNumber(normalized)
        if (existing != null) {
            callerDao.setFavoriteStatus(normalized, isFavorite)
        } else {
            val caller = getCaller(phoneNumber) ?: return@withContext
            callerDao.insertCaller(CallerEntity.fromDomain(caller.copy(isFavorite = isFavorite)))
        }
    }

    suspend fun updateNoteAndTag(phoneNumber: String, note: String?, tag: String?) = withContext(Dispatchers.IO) {
        val normalized = PhoneNumberExtractor.normalizePhoneNumber(phoneNumber)
        val existing = callerDao.getCallerByNormalizedNumber(normalized)
        if (existing != null) {
            callerDao.updateCallerNoteAndTag(normalized, note, tag)
        } else {
            val caller = getCaller(phoneNumber) ?: return@withContext
            callerDao.insertCaller(CallerEntity.fromDomain(caller.copy(note = note, tag = tag)))
        }
    }

    suspend fun getCaller(phoneNumber: String): CallerInfo? = withContext(Dispatchers.IO) {
        val normalized = PhoneNumberExtractor.normalizePhoneNumber(phoneNumber)
        
        // 1. Check local database for user-blocked, reported, or previously saved contacts
        val fromDb = callerDao.getCallerByNormalizedNumber(normalized)?.toDomain()
        if (fromDb != null) return@withContext fromDb

        // 2. Query device contacts if permission is available
        if (PermissionHelper.hasContactsPermission(context)) {
            val contact = lookupDeviceContact(phoneNumber)
            if (contact != null) {
                callerDao.insertCaller(CallerEntity.fromDomain(contact))
                return@withContext contact
            }
        }

        // 3. Resolve real country & region info from number
        val (country, region, carrier) = resolveOrigin(phoneNumber)

        CallerInfo(
            phoneNumber = phoneNumber,
            normalizedNumber = normalized,
            name = "Unlisted Number",
            callerType = CallerType.UNKNOWN,
            category = "Unlisted Caller",
            city = region,
            country = country,
            carrier = carrier
        )
    }

    suspend fun searchCallers(query: String): Flow<List<CallerInfo>> = withContext(Dispatchers.IO) {
        callerDao.searchCallers(query).map { list -> list.map { it.toDomain() } }
    }

    suspend fun addCaller(caller: CallerInfo) = withContext(Dispatchers.IO) {
        callerDao.insertCaller(CallerEntity.fromDomain(caller))
    }

    suspend fun blockNumber(phoneNumber: String, block: Boolean = true) = withContext(Dispatchers.IO) {
        val normalized = PhoneNumberExtractor.normalizePhoneNumber(phoneNumber)
        val existing = callerDao.getCallerByNormalizedNumber(normalized)
        if (existing != null) {
            callerDao.setBlockedStatus(normalized, block)
        } else {
            val (country, region, carrier) = resolveOrigin(phoneNumber)
            callerDao.insertCaller(
                CallerEntity(
                    phoneNumber = phoneNumber,
                    normalizedNumber = normalized,
                    name = "Blocked Number",
                    callerType = CallerType.SPAM.name,
                    category = "Blocked by User",
                    spamScore = 100,
                    city = region,
                    country = country,
                    carrier = carrier,
                    isBlocked = block
                )
            )
        }
    }

    suspend fun reportSpam(phoneNumber: String, reason: String, callerName: String? = null) = withContext(Dispatchers.IO) {
        val normalized = PhoneNumberExtractor.normalizePhoneNumber(phoneNumber)

        val existingSpam = spamDao.getSpamReport(normalized)
        if (existingSpam != null) {
            spamDao.incrementReportCount(normalized)
        } else {
            spamDao.insertSpamReport(
                SpamEntity(
                    phoneNumber = phoneNumber,
                    normalizedNumber = normalized,
                    reportReason = reason,
                    reportedCount = 1,
                    severity = "HIGH"
                )
            )
        }

        val existingCaller = callerDao.getCallerByNormalizedNumber(normalized)
        if (existingCaller != null) {
            callerDao.reportCallerAsSpam(normalized, 95)
        } else {
            val (country, region, carrier) = resolveOrigin(phoneNumber)
            callerDao.insertCaller(
                CallerEntity(
                    phoneNumber = phoneNumber,
                    normalizedNumber = normalized,
                    name = callerName ?: "Reported Spam",
                    callerType = CallerType.SPAM.name,
                    category = reason,
                    spamScore = 95,
                    reportsCount = 1,
                    city = region,
                    country = country,
                    carrier = carrier,
                    isBlocked = true
                )
            )
        }
    }

    suspend fun addCallRecord(record: CallRecord) = withContext(Dispatchers.IO) {
        callRecordDao.insertRecord(CallRecordEntity.fromDomain(record))
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        callRecordDao.clearAllRecords()
    }

    suspend fun deleteRecord(id: Long) = withContext(Dispatchers.IO) {
        callRecordDao.deleteRecordById(id)
    }

    private fun lookupDeviceContact(phoneNumber: String): CallerInfo? {
        val clean = PhoneNumberExtractor.normalizePhoneNumber(phoneNumber)
        if (clean.length < 5) return null

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
        val selectionArgs = arrayOf("%${clean.takeLast(8)}%")

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "Contact"
                    val (country, region, carrier) = resolveOrigin(phoneNumber)
                    return CallerInfo(
                        phoneNumber = phoneNumber,
                        normalizedNumber = clean,
                        name = name ?: "Contact",
                        callerType = CallerType.CONTACT,
                        category = "Personal Contact",
                        city = region,
                        country = country,
                        carrier = carrier
                    )
                }
            }
        } catch (_: Exception) {
            // Permission or provider unavailable
        }
        return null
    }

    suspend fun syncDeviceContacts(): Int = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasContactsPermission(context)) return@withContext 0

        val entities = mutableListOf<CallerEntity>()
        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val seenNumbers = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    val rawNum = if (numIdx >= 0) cursor.getString(numIdx) ?: continue else continue
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Contact" else "Contact"
                    val normalized = PhoneNumberExtractor.normalizePhoneNumber(rawNum)

                    if (normalized.length >= 7 && seenNumbers.add(normalized)) {
                        val (country, region, carrier) = resolveOrigin(rawNum)
                        entities.add(
                            CallerEntity(
                                phoneNumber = rawNum,
                                normalizedNumber = normalized,
                                name = name,
                                callerType = CallerType.CONTACT.name,
                                category = "Phonebook Contact",
                                city = region,
                                country = country,
                                carrier = carrier
                            )
                        )
                    }
                }
            }

            if (entities.isNotEmpty()) {
                callerDao.insertCallers(entities)
            }
        } catch (_: Exception) {
            // Security or provider exception
        }
        return@withContext entities.size
    }

    suspend fun syncSystemCallLogs(): Int = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasPhonePermissions(context)) return@withContext 0

        val entities = mutableListOf<CallRecordEntity>()
        try {
            val uri = CallLog.Calls.CONTENT_URI
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            )
            val order = "${CallLog.Calls.DATE} DESC LIMIT 100"

            context.contentResolver.query(uri, projection, null, null, order)?.use { cursor ->
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)

                while (cursor.moveToNext()) {
                    val number = if (numIdx >= 0) cursor.getString(numIdx) ?: "Unknown" else "Unknown"
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val rawType = if (typeIdx >= 0) cursor.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                    val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                    val dur = if (durIdx >= 0) cursor.getInt(durIdx) else 0

                    val callType = when (rawType) {
                        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                        CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                        CallLog.Calls.BLOCKED_TYPE -> CallType.SPAM_BLOCKED
                        else -> CallType.INCOMING
                    }

                    entities.add(
                        CallRecordEntity(
                            phoneNumber = number,
                            callerName = name,
                            callType = callType.name,
                            timestamp = date,
                            durationSeconds = dur,
                            isSpam = callType == CallType.SPAM_BLOCKED
                        )
                    )
                }
            }

            if (entities.isNotEmpty()) {
                callRecordDao.insertRecords(entities)
            }
        } catch (_: Exception) {
            // Permission or system error
        }
        return@withContext entities.size
    }

    private fun resolveOrigin(phoneNumber: String): Triple<String, String, String> {
        val clean = phoneNumber.trim()
        return when {
            clean.startsWith("+1") || clean.length == 10 && !clean.startsWith("+") -> {
                Triple("US / CA", "North America", "Cellular Network")
            }
            clean.startsWith("+44") -> {
                Triple("UK", "United Kingdom", "BT / Vodafone / O2")
            }
            clean.startsWith("+91") -> {
                Triple("IN", "India", "Jio / Airtel / Vi")
            }
            clean.startsWith("+49") -> {
                Triple("DE", "Germany", "Telekom / Vodafone")
            }
            clean.startsWith("+33") -> {
                Triple("FR", "France", "Orange / SFR")
            }
            clean.startsWith("+61") -> {
                Triple("AU", "Australia", "Telstra / Optus")
            }
            clean.startsWith("+81") -> {
                Triple("JP", "Japan", "NTT Docomo / SoftBank")
            }
            clean.startsWith("+") -> {
                Triple("Global", "International", "International Carrier")
            }
            else -> {
                Triple("Local", "Mobile", "Local Network")
            }
        }
    }
}
