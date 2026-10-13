package com.example.data.repository

import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract
import com.example.data.model.CallRecord
import com.example.data.model.CallType
import com.example.data.model.CallerInfo
import com.example.data.model.CallerType
import com.example.data.model.ScanResult
import com.example.data.model.SpamReport
import com.example.database.AppDatabase
import com.example.database.entity.CallRecordEntity
import com.example.database.entity.CallerEntity
import com.example.database.entity.ScanHistoryEntity
import com.example.database.entity.SpamEntity
import com.example.scanner.AiCallerAuditor
import com.example.scanner.AiSecurityAudit
import com.example.scanner.PhoneNumberExtractor
import com.example.utils.CarrierResolver
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
    private val scanHistoryDao = database.scanHistoryDao()

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

    val scanHistory: Flow<List<ScanResult>> = scanHistoryDao.getAllScanned()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    suspend fun saveScannedNumber(result: ScanResult) = withContext(Dispatchers.IO) {
        val carrierOrigin = CarrierResolver.resolve(result.phoneNumber)
        scanHistoryDao.insertScanned(ScanHistoryEntity.fromDomain(result, carrierOrigin.carrier))
    }

    suspend fun saveScannedNumbers(results: List<ScanResult>) = withContext(Dispatchers.IO) {
        val entities = results.map { res ->
            val origin = CarrierResolver.resolve(res.phoneNumber)
            ScanHistoryEntity.fromDomain(res, origin.carrier)
        }
        scanHistoryDao.insertScannedList(entities)
    }

    suspend fun clearScanHistory() = withContext(Dispatchers.IO) {
        scanHistoryDao.clearAllScanned()
    }

    suspend fun deleteScannedNumber(phoneNumber: String) = withContext(Dispatchers.IO) {
        scanHistoryDao.deleteByPhoneNumber(phoneNumber)
    }

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

    suspend fun auditCallerWithAi(phoneNumber: String): AiSecurityAudit = withContext(Dispatchers.IO) {
        val caller = getCaller(phoneNumber)
        AiCallerAuditor.auditNumberWithAi(
            phoneNumber = phoneNumber,
            callerName = caller?.name,
            category = caller?.category,
            reportsCount = caller?.reportsCount ?: 0
        )
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

        // 3. Resolve real country & region info with CarrierResolver
        val origin = CarrierResolver.resolve(phoneNumber)

        CallerInfo(
            phoneNumber = phoneNumber,
            normalizedNumber = normalized,
            name = if (origin.carrierCode in listOf("NTC", "NCELL", "SMART")) "${origin.carrier} User" else "Unlisted Number",
            callerType = CallerType.UNKNOWN,
            category = origin.region,
            city = origin.region,
            country = origin.country,
            carrier = origin.carrier
        )
    }

    suspend fun reportSpam(
        phoneNumber: String,
        reason: String,
        details: String?,
        category: String = "Spam / Telemarketing"
    ) = withContext(Dispatchers.IO) {
        val normalized = PhoneNumberExtractor.normalizePhoneNumber(phoneNumber)
        val origin = CarrierResolver.resolve(phoneNumber)

        val report = SpamEntity(
            phoneNumber = phoneNumber,
            normalizedNumber = normalized,
            reportReason = reason,
            reportedCount = 1,
            severity = if (category.contains("FRAUD", ignoreCase = true)) "CRITICAL" else "HIGH",
            reportedAt = System.currentTimeMillis()
        )
        spamDao.insertSpamReport(report)

        val existing = callerDao.getCallerByNormalizedNumber(normalized)
        if (existing != null) {
            callerDao.reportCallerAsSpam(normalized, spamScore = (existing.spamScore + 25).coerceAtMost(100))
        } else {
            val caller = CallerEntity(
                phoneNumber = phoneNumber,
                normalizedNumber = normalized,
                name = "Reported Spam Caller",
                callerType = CallerType.SPAM.name,
                category = category,
                spamScore = 75,
                reportsCount = 1,
                city = origin.region,
                country = origin.country,
                carrier = origin.carrier,
                isBlocked = true
            )
            callerDao.insertCaller(caller)
        }
    }

    suspend fun setBlockedStatus(phoneNumber: String, isBlocked: Boolean) = withContext(Dispatchers.IO) {
        val normalized = PhoneNumberExtractor.normalizePhoneNumber(phoneNumber)
        val existing = callerDao.getCallerByNormalizedNumber(normalized)
        if (existing != null) {
            callerDao.setBlockedStatus(normalized, isBlocked)
        } else {
            val origin = CarrierResolver.resolve(phoneNumber)
            val caller = CallerEntity(
                phoneNumber = phoneNumber,
                normalizedNumber = normalized,
                name = "Blocked Caller",
                callerType = CallerType.SPAM.name,
                category = "User Blocked",
                city = origin.region,
                country = origin.country,
                carrier = origin.carrier,
                isBlocked = isBlocked
            )
            callerDao.insertCaller(caller)
        }
    }

    suspend fun logCall(
        phoneNumber: String,
        callerName: String?,
        callType: CallType,
        durationSeconds: Int = 0,
        isSpam: Boolean = false,
        spamReason: String? = null
    ) = withContext(Dispatchers.IO) {
        val origin = CarrierResolver.resolve(phoneNumber)
        val entity = CallRecordEntity(
            phoneNumber = phoneNumber,
            callerName = callerName ?: if (origin.carrierCode in listOf("NTC", "NCELL")) origin.carrier else null,
            callType = callType.name,
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            isSpam = isSpam,
            spamReason = spamReason,
            category = origin.region
        )
        callRecordDao.insertRecord(entity)
    }

    suspend fun addCallRecord(record: CallRecord) = withContext(Dispatchers.IO) {
        callRecordDao.insertRecord(CallRecordEntity.fromDomain(record))
    }

    suspend fun clearCallRecords() = withContext(Dispatchers.IO) {
        callRecordDao.clearAllRecords()
    }

    suspend fun deleteCallRecord(id: Long) = withContext(Dispatchers.IO) {
        callRecordDao.deleteRecordById(id)
    }

    private fun lookupDeviceContact(phoneNumber: String): CallerInfo? {
        val clean = PhoneNumberExtractor.normalizePhoneNumber(phoneNumber)
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
        val selectionArgs = arrayOf("%${clean.takeLast(8)}%")

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "Contact"
                    val origin = CarrierResolver.resolve(phoneNumber)
                    return CallerInfo(
                        phoneNumber = phoneNumber,
                        normalizedNumber = clean,
                        name = name ?: "Contact",
                        callerType = CallerType.CONTACT,
                        category = "Personal Contact",
                        city = origin.region,
                        country = origin.country,
                        carrier = origin.carrier
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
                        val origin = CarrierResolver.resolve(rawNum)
                        entities.add(
                            CallerEntity(
                                phoneNumber = rawNum,
                                normalizedNumber = normalized,
                                name = name,
                                callerType = CallerType.CONTACT.name,
                                category = "Phonebook Contact",
                                city = origin.region,
                                country = origin.country,
                                carrier = origin.carrier
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
}
