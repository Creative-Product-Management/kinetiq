package com.example.data.backup

import com.example.data.model.Category
import com.example.data.model.Habit
import com.example.data.model.HabitModality
import com.example.data.model.LogEntry
import com.example.data.model.RoutineStackEntity
import com.example.data.repository.HabitRepository
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@JsonClass(generateAdapter = false)
data class BackupHabit(
    val id: Long, val title: String, val description: String, val cueTrigger: String,
    val modality: String, val targetValue: Double, val floorValue: Double?, val unit: String,
    val categoryId: Long?, val colorHex: String, val iconName: String,
    val reminderHour: Int?, val reminderMinute: Int?, val reminderDaysMask: Int,
    val restDaysMask: Int, val routineStackId: Long?, val orderIndex: Int,
    val isArchived: Boolean, val createdAt: Long
)

@JsonClass(generateAdapter = false)
data class BackupLog(
    val id: Long, val habitId: Long, val dateString: String, val isCompleted: Boolean,
    val isFloorSustained: Boolean, val numericValue: Double, val durationSeconds: Int,
    val tallyCount: Int, val frictionTag: String?, val notes: String, val recordedAt: Long
)

@JsonClass(generateAdapter = false)
data class BackupCategory(val id: Long, val name: String, val colorHex: String, val iconName: String, val orderIndex: Int)

@JsonClass(generateAdapter = false)
data class BackupStack(val id: Long, val name: String, val description: String, val iconName: String, val colorHex: String)

@JsonClass(generateAdapter = false)
data class BackupPayload(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val exportedAt: Long,
    val categories: List<BackupCategory>,
    val stacks: List<BackupStack>,
    val habits: List<BackupHabit>,
    val logs: List<BackupLog>
) { companion object { const val CURRENT_SCHEMA = 1 } }

@JsonClass(generateAdapter = false)
// Brand rename: the format string is intentionally kept — changing it would reject every previously exported backup.
data class BackupEnvelope(val format: String = "kinetiq-backup", val version: Int = 1, val salt: String, val iv: String, val data: String)

/** Passphrase-encrypted, portable backup. AES-GCM with PBKDF2-derived key; no plaintext leaves the app. */
class BackupManager(private val habitRepository: HabitRepository) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val payloadAdapter = moshi.adapter(BackupPayload::class.java)
    private val envelopeAdapter = moshi.adapter(BackupEnvelope::class.java)

    suspend fun exportEncrypted(passphrase: CharArray): ByteArray {
        require(passphrase.size >= 8) { "Passphrase must be at least 8 characters" }
        val payload = BackupPayload(
            exportedAt = System.currentTimeMillis(),
            categories = habitRepository.snapshotCategories().map { BackupCategory(it.id, it.name, it.colorHex, it.iconName, it.orderIndex) },
            stacks = habitRepository.snapshotStacks().map { BackupStack(it.id, it.name, it.description, it.iconName, it.colorHex) },
            habits = habitRepository.snapshotHabits().map {
                BackupHabit(it.id, it.title, it.description, it.cueTrigger, it.modality.name, it.targetValue, it.floorValue, it.unit, it.categoryId, it.colorHex, it.iconName, it.reminderHour, it.reminderMinute, it.reminderDaysMask, it.restDaysMask, it.routineStackId, it.orderIndex, it.isArchived, it.createdAt)
            },
            logs = habitRepository.snapshotLogs().map {
                BackupLog(it.id, it.habitId, it.dateString, it.isCompleted, it.isFloorSustained, it.numericValue, it.durationSeconds, it.tallyCount, it.frictionTag, it.notes, it.recordedAt)
            }
        )
        return BackupCrypto.encrypt(payloadAdapter.toJson(payload).toByteArray(Charsets.UTF_8), passphrase, envelopeAdapter)
    }

    suspend fun importEncrypted(bytes: ByteArray, passphrase: CharArray) {
        val plain = BackupCrypto.decrypt(bytes, passphrase, envelopeAdapter)
        val payload = payloadAdapter.fromJson(plain.toString(Charsets.UTF_8))
            ?: throw IllegalArgumentException("Invalid backup file")
        require(payload.schemaVersion == BackupPayload.CURRENT_SCHEMA) { "Unsupported backup schema ${payload.schemaVersion}" }
        // Validate references before touching the DB.
        val habitIds = payload.habits.map { it.id }.toSet()
        require(payload.logs.all { it.habitId in habitIds }) { "Backup contains logs for unknown habits" }
        habitRepository.replaceAllData(
            categories = payload.categories.map { Category(it.id, it.name, it.colorHex, it.iconName, it.orderIndex) },
            stacks = payload.stacks.map { RoutineStackEntity(it.id, it.name, it.description, it.iconName, it.colorHex) },
            habits = payload.habits.map {
                Habit(id = it.id, title = it.title, description = it.description, cueTrigger = it.cueTrigger,
                    modality = runCatching { HabitModality.valueOf(it.modality) }.getOrDefault(HabitModality.YES_NO),
                    targetValue = it.targetValue, floorValue = it.floorValue, unit = it.unit, categoryId = it.categoryId,
                    colorHex = it.colorHex, iconName = it.iconName, reminderHour = it.reminderHour, reminderMinute = it.reminderMinute,
                    reminderDaysMask = it.reminderDaysMask, restDaysMask = it.restDaysMask, routineStackId = it.routineStackId,
                    orderIndex = it.orderIndex, isArchived = it.isArchived, createdAt = it.createdAt)
            },
            logs = payload.logs.map {
                LogEntry(it.id, it.habitId, it.dateString, it.isCompleted, it.isFloorSustained, it.numericValue, it.durationSeconds, it.tallyCount, it.frictionTag, it.notes, it.recordedAt)
            }
        )
    }
}

object BackupCrypto {
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun encrypt(plain: ByteArray, passphrase: CharArray, envelopeAdapter: com.squareup.moshi.JsonAdapter<BackupEnvelope>): ByteArray {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plain)
        val envelope = BackupEnvelope(
            salt = Base64.getEncoder().encodeToString(salt),
            iv = Base64.getEncoder().encodeToString(iv),
            data = Base64.getEncoder().encodeToString(encrypted)
        )
        return envelopeAdapter.toJson(envelope).toByteArray(Charsets.UTF_8)
    }

    fun decrypt(bytes: ByteArray, passphrase: CharArray, envelopeAdapter: com.squareup.moshi.JsonAdapter<BackupEnvelope>): ByteArray {
        val envelope = envelopeAdapter.fromJson(bytes.toString(Charsets.UTF_8))
            ?: throw IllegalArgumentException("Invalid backup file")
        require(envelope.format == "kinetiq-backup") { "Not a Kinetic Routines backup" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            deriveKey(passphrase, Base64.getDecoder().decode(envelope.salt)),
            GCMParameterSpec(128, Base64.getDecoder().decode(envelope.iv))
        )
        return cipher.doFinal(Base64.getDecoder().decode(envelope.data))
    }

    /** Standalone helpers for tests / non-Moshi call sites. */
    fun encryptRaw(plain: ByteArray, passphrase: CharArray): ByteArray {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(128, iv))
        return salt + iv + cipher.doFinal(plain)
    }

    fun decryptRaw(bytes: ByteArray, passphrase: CharArray): ByteArray {
        val salt = bytes.copyOfRange(0, 16); val iv = bytes.copyOfRange(16, 28); val data = bytes.copyOfRange(28, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(128, iv))
        return cipher.doFinal(data)
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }
}
