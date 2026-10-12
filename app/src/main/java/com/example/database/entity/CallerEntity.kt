package com.example.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.CallerInfo
import com.example.data.model.CallerType

@Entity(
    tableName = "callers",
    indices = [Index(value = ["normalizedNumber"], unique = true)]
)
data class CallerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val normalizedNumber: String,
    val name: String,
    val callerType: String = CallerType.UNKNOWN.name,
    val category: String = "Unknown",
    val spamScore: Int = 0,
    val reportsCount: Int = 0,
    val city: String = "Unknown",
    val country: String = "US",
    val carrier: String = "Cellular",
    val isBlocked: Boolean = false,
    val isFavorite: Boolean = false,
    val note: String? = null,
    val tag: String? = null,
    val avatarUrl: String? = null
) {
    fun toDomain(): CallerInfo {
        val type = try {
            CallerType.valueOf(callerType)
        } catch (_: Exception) {
            CallerType.UNKNOWN
        }
        return CallerInfo(
            id = id,
            phoneNumber = phoneNumber,
            normalizedNumber = normalizedNumber,
            name = name,
            callerType = type,
            category = category,
            spamScore = spamScore,
            reportsCount = reportsCount,
            city = city,
            country = country,
            carrier = carrier,
            isBlocked = isBlocked,
            isFavorite = isFavorite,
            note = note,
            tag = tag,
            avatarUrl = avatarUrl
        )
    }

    companion object {
        fun fromDomain(caller: CallerInfo): CallerEntity {
            return CallerEntity(
                id = caller.id,
                phoneNumber = caller.phoneNumber,
                normalizedNumber = caller.normalizedNumber,
                name = caller.name,
                callerType = caller.callerType.name,
                category = caller.category,
                spamScore = caller.spamScore,
                reportsCount = caller.reportsCount,
                city = caller.city,
                country = caller.country,
                carrier = caller.carrier,
                isBlocked = caller.isBlocked,
                isFavorite = caller.isFavorite,
                note = caller.note,
                tag = caller.tag,
                avatarUrl = caller.avatarUrl
            )
        }
    }
}
