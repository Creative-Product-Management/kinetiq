package com.example

import com.example.data.backup.BackupCategory
import com.example.data.backup.BackupCrypto
import com.example.data.backup.BackupEnvelope
import com.example.data.backup.BackupHabit
import com.example.data.backup.BackupLog
import com.example.data.backup.BackupPayload
import com.example.data.backup.BackupStack
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Base64

/**
 * Writes a deterministic encrypted backup + plaintext payload under golden/
 * so a TypeScript (or other) importer can verify against known bytes.
 *
 * See BACKUP_FORMAT.md for the full byte-level spec.
 */
class BackupGoldenFixtureTest {

    companion object {
        /** Documented in BACKUP_FORMAT.md — must stay stable for cross-platform tests. */
        const val PASSPHRASE = "kinetiq-golden-pass"

        val FIXED_SALT: ByteArray = byteArrayOf(
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f
        )

        val FIXED_IV: ByteArray = byteArrayOf(
            0x10, 0x11, 0x12, 0x13, 0x14, 0x15,
            0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b
        )
    }

    private fun goldenDir(): File {
        val cwd = File(System.getProperty("user.dir"))
        val root = when {
            cwd.resolve("BACKUP_FORMAT.md").exists() -> cwd
            cwd.name == "app" && cwd.parentFile.resolve("BACKUP_FORMAT.md").exists() -> cwd.parentFile
            cwd.parentFile?.resolve("BACKUP_FORMAT.md")?.exists() == true -> cwd.parentFile
            else -> cwd
        }
        return File(root, "golden").also { it.mkdirs() }
    }

    private fun samplePayload(): BackupPayload = BackupPayload(
        schemaVersion = BackupPayload.CURRENT_SCHEMA,
        exportedAt = 1_767_600_000_000L, // fixed: 2026-01-01T00:00:00Z-ish stable stamp
        categories = listOf(
            BackupCategory(
                id = 1,
                name = "Mindfulness",
                colorHex = "#4A5568",
                iconName = "SelfImprovement",
                orderIndex = 0
            )
        ),
        stacks = listOf(
            BackupStack(
                id = 1,
                name = "Morning Routine",
                description = "Start the day with intention",
                iconName = "WbSunny",
                colorHex = "#6B5A3D"
            )
        ),
        habits = listOf(
            BackupHabit(
                id = 1,
                title = "Meditate",
                description = "Sit quietly",
                cueTrigger = "After I pour coffee",
                modality = "YES_NO",
                targetValue = 1.0,
                floorValue = null,
                unit = "",
                categoryId = 1,
                colorHex = "#10B981",
                iconName = "SelfImprovement",
                reminderHour = 8,
                reminderMinute = 0,
                reminderDaysMask = 127,
                restDaysMask = 0,
                routineStackId = 1,
                orderIndex = 0,
                isArchived = false,
                createdAt = 1_760_000_000_000L,
                checkInQuestion = "Did you meditate today?"
            ),
            BackupHabit(
                id = 2,
                title = "Water",
                description = "",
                cueTrigger = "",
                modality = "NUMERIC",
                targetValue = 2500.0,
                floorValue = 500.0,
                unit = "ml",
                categoryId = 1,
                colorHex = "#3B82F6",
                iconName = "WaterDrop",
                reminderHour = null,
                reminderMinute = null,
                reminderDaysMask = 127,
                restDaysMask = 1 shl 6, // Sunday rest
                routineStackId = null,
                orderIndex = 1,
                isArchived = false,
                createdAt = 1_760_000_100_000L,
                checkInQuestion = ""
            ),
            BackupHabit(
                id = 3,
                title = "Focus block",
                description = "",
                cueTrigger = "",
                modality = "TIMER",
                targetValue = 25.0,
                floorValue = 10.0,
                unit = "mins",
                categoryId = null,
                colorHex = "#8B5CF6",
                iconName = "Bolt",
                reminderHour = 14,
                reminderMinute = 30,
                reminderDaysMask = 0b0011111, // Mon–Fri
                restDaysMask = 0,
                routineStackId = null,
                orderIndex = 2,
                isArchived = false,
                createdAt = 1_760_000_200_000L
            ),
            BackupHabit(
                id = 4,
                title = "Push-ups",
                description = "",
                cueTrigger = "",
                modality = "TALLY",
                targetValue = 20.0,
                floorValue = 5.0,
                unit = "times",
                categoryId = 1,
                colorHex = "#F59E0B",
                iconName = "FitnessCenter",
                reminderHour = null,
                reminderMinute = null,
                reminderDaysMask = 127,
                restDaysMask = 0,
                routineStackId = 1,
                orderIndex = 3,
                isArchived = false,
                createdAt = 1_760_000_300_000L
            )
        ),
        logs = listOf(
            BackupLog(
                id = 1, habitId = 1, dateString = "2026-01-01",
                isCompleted = true, isFloorSustained = false,
                numericValue = 1.0, durationSeconds = 0, tallyCount = 1,
                frictionTag = null, notes = "", recordedAt = 1_767_650_000_000L
            ),
            BackupLog(
                id = 2, habitId = 2, dateString = "2026-01-01",
                isCompleted = false, isFloorSustained = true,
                numericValue = 500.0, durationSeconds = 0, tallyCount = 0,
                frictionTag = null, notes = "", recordedAt = 1_767_651_000_000L
            ),
            BackupLog(
                id = 3, habitId = 4, dateString = "2025-12-31",
                isCompleted = false, isFloorSustained = false,
                numericValue = 0.0, durationSeconds = 0, tallyCount = 0,
                frictionTag = "Low Energy", notes = "Travel day", recordedAt = 1_767_580_000_000L
            ),
            BackupLog(
                id = 4, habitId = 3, dateString = "2026-01-01",
                isCompleted = true, isFloorSustained = false,
                numericValue = 25.0, durationSeconds = 1500, tallyCount = 0,
                frictionTag = null, notes = "", recordedAt = 1_767_652_000_000L
            )
        )
    )

    @Test
    fun `writes deterministic encrypted backup and decrypted payload to golden`() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val payloadAdapter = moshi.adapter(BackupPayload::class.java).serializeNulls()
        val envelopeAdapter = moshi.adapter(BackupEnvelope::class.java)

        val payload = samplePayload()
        // Exact plaintext bytes that were encrypted (Moshi compact + serializeNulls).
        val payloadJson = payloadAdapter.toJson(payload)
        val plainBytes = payloadJson.toByteArray(Charsets.UTF_8)

        val kqbakBytes = BackupCrypto.encrypt(
            plain = plainBytes,
            passphrase = PASSPHRASE.toCharArray(),
            envelopeAdapter = envelopeAdapter,
            saltOverride = FIXED_SALT,
            ivOverride = FIXED_IV
        )

        // Round-trip through production decrypt.
        val decrypted = BackupCrypto.decrypt(kqbakBytes, PASSPHRASE.toCharArray(), envelopeAdapter)
        assertArrayEquals(plainBytes, decrypted)
        val roundTrip = payloadAdapter.fromJson(decrypted.toString(Charsets.UTF_8))!!
        assertEquals(payload.schemaVersion, roundTrip.schemaVersion)
        assertEquals(payload.habits.size, roundTrip.habits.size)
        assertEquals(payload.logs.size, roundTrip.logs.size)
        assertEquals("Did you meditate today?", roundTrip.habits.first { it.id == 1L }.checkInQuestion)

        val envelope = envelopeAdapter.fromJson(kqbakBytes.toString(Charsets.UTF_8))!!
        assertEquals("kinetiq-backup", envelope.format)
        assertEquals(1, envelope.version)
        assertArrayEquals(FIXED_SALT, Base64.getDecoder().decode(envelope.salt))
        assertArrayEquals(FIXED_IV, Base64.getDecoder().decode(envelope.iv))
        val rawData = Base64.getDecoder().decode(envelope.data)
        assertTrue("GCM data must include 16-byte tag", rawData.size > 16)

        val dir = goldenDir()
        // Exact decrypted UTF-8 (no trailing newline) so importers can byte-compare after GCM.
        File(dir, "backup-sample.payload.json").writeBytes(plainBytes)
        File(dir, "backup-sample.kqbak").writeBytes(kqbakBytes)
        File(dir, "backup-sample.payload.pretty.json").writeText(
            payloadAdapter.indent("  ").toJson(payload) + "\n"
        )
        File(dir, "backup-sample.meta.json").writeText(
            """
            {
              "passphrase": "$PASSPHRASE",
              "saltHex": "${FIXED_SALT.joinToString("") { "%02x".format(it) }}",
              "ivHex": "${FIXED_IV.joinToString("") { "%02x".format(it) }}",
              "pbkdf2": {
                "algorithm": "PBKDF2WithHmacSHA256",
                "iterations": 120000,
                "keyBits": 256
              },
              "aesGcm": {
                "transformation": "AES/GCM/NoPadding",
                "tagBits": 128,
                "aad": null
              },
              "envelopeFormat": "kinetiq-backup",
              "envelopeVersion": 1,
              "payloadSchemaVersion": ${BackupPayload.CURRENT_SCHEMA},
              "files": {
                "encrypted": "backup-sample.kqbak",
                "decryptedJson": "backup-sample.payload.json",
                "decryptedJsonPretty": "backup-sample.payload.pretty.json"
              },
              "notes": "backup-sample.payload.json is the exact UTF-8 plaintext encrypted into the .kqbak (Moshi compact, serializeNulls). AAD is not used. See BACKUP_FORMAT.md."
            }
            """.trimIndent() + "\n"
        )

        assertTrue(File(dir, "backup-sample.kqbak").length() > 64)
        assertTrue(File(dir, "backup-sample.payload.json").length() > 64)
    }
}
