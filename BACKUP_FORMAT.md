# Kinetic Routines — Backup Format (`.kqbak`)

Byte-for-byte specification of the passphrase-encrypted portable backup.  
**Source:** `app/src/main/java/com/example/data/backup/BackupManager.kt`  
**Golden sample:** [`golden/backup-sample.kqbak`](golden/backup-sample.kqbak) + [`golden/backup-sample.payload.json`](golden/backup-sample.payload.json)  
(produced by `BackupGoldenFixtureTest`; passphrase below).

Companion: [SPEC.md](SPEC.md), [ALGORITHMS.md](ALGORITHMS.md).

---

## File on disk

| Property | Value |
|----------|-------|
| Suggested filename | `kinetic-routines-backup.kqbak` (Settings export picker) |
| MIME / picker type | `application/octet-stream` |
| Encoding of file bytes | **UTF-8** text (JSON object — the envelope) |
| Inner payload | UTF-8 JSON, then AES-GCM encrypted, then Base64 in envelope `data` |

The `.kqbak` file is **not** a binary blob of `salt‖iv‖ciphertext`. That raw layout exists only as `BackupCrypto.encryptRaw` / `decryptRaw` for unit tests and is **not** the portable format.

---

## Outer envelope (`BackupEnvelope`)

UTF-8 JSON object written by Moshi (`KotlinJsonAdapterFactory`). Field names are camelCase property names.

| Field | JSON type | Default (Kotlin) | Meaning |
|-------|-----------|------------------|---------|
| `format` | string | `"kinetiq-backup"` | Magic. Import **requires** exact match |
| `version` | number (int) | `1` | Envelope crypto/layout version (not payload schema) |
| `salt` | string | *(required)* | PBKDF2 salt, **Base64** (standard alphabet, with padding) |
| `iv` | string | *(required)* | AES-GCM IV/nonce, **Base64** |
| `data` | string | *(required)* | Ciphertext ‖ GCM tag, **Base64** |

### Example shape

```json
{
  "format": "kinetiq-backup",
  "version": 1,
  "salt": "<base64 16 raw bytes>",
  "iv": "<base64 12 raw bytes>",
  "data": "<base64 ciphertext||tag>"
}
```

Moshi may omit keys that equal Kotlin defaults when serializing in some configurations; production `encrypt` always constructs `BackupEnvelope(salt=…, iv=…, data=…)` so `format` / `version` use defaults and **are included** by Moshi’s reflective adapter for this data class (present in golden output). Importers should treat missing `format` as invalid if they require an explicit magic, and treat missing `version` as `1` only if matching Moshi defaults — the app’s decrypt path requires a fully parsed envelope and checks `format == "kinetiq-backup"`.

### Encodings

| Layer | Codec |
|-------|--------|
| Envelope file | UTF-8 JSON (no BOM) |
| `salt`, `iv`, `data` | `java.util.Base64.getEncoder()` / `getDecoder()` — **basic** Base64 (not URL-safe), **with** `=` padding, **no** line wraps |
| Decrypted payload | UTF-8 JSON |

### Binary sizes after Base64 decode

| Field | Raw byte length |
|-------|-----------------|
| `salt` | **16** |
| `iv` | **12** |
| `data` | `len(ciphertext) + 16` (16 = GCM authentication tag) |

---

## Cryptography

### Key derivation (PBKDF2)

| Parameter | Value |
|-----------|--------|
| Algorithm | `PBKDF2WithHmacSHA256` (`SecretKeyFactory`) |
| Password | `CharArray` passphrase (export requires `length >= 8`) |
| Salt | 16 random bytes (`SecureRandom`), or fixed in golden |
| Iterations | **`120_000`** |
| Derived key length | **`256` bits** (32 bytes) |
| Key type | `SecretKeySpec(…, "AES")` |

```
key ← PBKDF2-HMAC-SHA256(passphrase, salt, iterations=120000, dkLen=32)
```

### Content encryption (AES-GCM)

| Parameter | Value |
|-----------|--------|
| Transformation | `AES/GCM/NoPadding` |
| Key | 256-bit AES from PBKDF2 |
| IV / nonce | **12** bytes (`SecureRandom` or golden fixed) |
| GCM tag length | **128** bits (`GCMParameterSpec(128, iv)`) |
| Plaintext | UTF-8 bytes of `BackupPayload` JSON |
| `Cipher.doFinal` output | `ciphertext ‖ tag` (tag is the last **16** bytes) |
| **AAD** | **Not used.** `Cipher.updateAAD` is never called. Authenticated data is ciphertext+tag only; envelope `format`/`version` are **not** bound into the AEAD. |

### Encrypt steps (production `.kqbak`)

```
1. require passphrase.length >= 8
2. Build BackupPayload; Moshi → UTF-8 plain bytes
3. salt ← 16 random bytes; iv ← 12 random bytes
4. key ← PBKDF2(...)
5. cipher.init(ENCRYPT, key, GCMParameterSpec(128, iv))
   // no updateAAD
6. encrypted ← cipher.doFinal(plain)   // ciphertext || 16-byte tag
7. envelope ← {
     format: "kinetiq-backup",   // default
     version: 1,                 // default
     salt: Base64(salt),
     iv: Base64(iv),
     data: Base64(encrypted)
   }
8. file bytes ← UTF-8(Moshi JSON of envelope)
```

### Decrypt steps

```
1. Parse envelope JSON as UTF-8 → BackupEnvelope (null → "Invalid backup file")
2. require envelope.format == "kinetiq-backup"  (else "Not a Kinetic Routines backup")
3. salt ← Base64Decode(envelope.salt); iv ← Base64Decode(envelope.iv)
4. key ← PBKDF2(passphrase, salt, 120000, 256)
5. cipher.init(DECRYPT, key, GCMParameterSpec(128, iv))
6. plain ← cipher.doFinal(Base64Decode(envelope.data))
   // wrong passphrase / tampering → AEAD failure (exception)
7. Parse plain as UTF-8 BackupPayload JSON
```

### Non-portable raw layout (`encryptRaw` / `decryptRaw`)

Used only in JVM unit tests (`ReminderAndValidationTest`):

```
bytes = salt (16) ‖ iv (12) ‖ ciphertext‖tag
```

No JSON envelope. **Do not** use this for TypeScript / cross-app import.

---

## Inner payload (`BackupPayload`)

UTF-8 JSON after successful GCM decrypt. Moshi + `KotlinJsonAdapterFactory`.

| Field | Type | Default | Notes |
|-------|------|---------|--------|
| `schemaVersion` | `Int` | `1` (`CURRENT_SCHEMA`) | Must equal `1` on import |
| `exportedAt` | `Long` | *(required in practice)* | `System.currentTimeMillis()` at export |
| `categories` | `List<BackupCategory>` | — | |
| `stacks` | `List<BackupStack>` | — | |
| `habits` | `List<BackupHabit>` | — | |
| `logs` | `List<BackupLog>` | — | |

### `BackupCategory`

| Field | Type |
|-------|------|
| `id` | `Long` |
| `name` | `String` |
| `colorHex` | `String` |
| `iconName` | `String` |
| `orderIndex` | `Int` |

### `BackupStack`

| Field | Type |
|-------|------|
| `id` | `Long` |
| `name` | `String` |
| `description` | `String` |
| `iconName` | `String` |
| `colorHex` | `String` |

### `BackupHabit`

| Field | Type | Default | Notes |
|-------|------|---------|--------|
| `id` | `Long` | | Preserved Room id |
| `title` | `String` | | |
| `description` | `String` | | |
| `cueTrigger` | `String` | | |
| `modality` | `String` | | Enum **name**: `YES_NO`, `NUMERIC`, `TIMER`, `TALLY` |
| `targetValue` | `Double` | | |
| `floorValue` | `Double?` | | JSON `null` if unset |
| `unit` | `String` | | |
| `categoryId` | `Long?` | | |
| `colorHex` | `String` | | |
| `iconName` | `String` | | |
| `reminderHour` | `Int?` | | |
| `reminderMinute` | `Int?` | | |
| `reminderDaysMask` | `Int` | | Bit `(isoDow−1)`; default in app is `127` |
| `restDaysMask` | `Int` | | |
| `routineStackId` | `Long?` | | |
| `orderIndex` | `Int` | | |
| `isArchived` | `Boolean` | | |
| `createdAt` | `Long` | | Epoch millis |
| `checkInQuestion` | `String` | `""` | Absent in older backups → Moshi default `""` |

### `BackupLog`

| Field | Type |
|-------|------|
| `id` | `Long` |
| `habitId` | `Long` |
| `dateString` | `String` (`yyyy-MM-dd`) |
| `isCompleted` | `Boolean` |
| `isFloorSustained` | `Boolean` |
| `numericValue` | `Double` |
| `durationSeconds` | `Int` |
| `tallyCount` | `Int` |
| `frictionTag` | `String?` |
| `notes` | `String` |
| `recordedAt` | `Long` |

Boolean JSON: `true` / `false`. Numbers are JSON numbers (Moshi may emit ints without `.0` for whole Doubles).

---

## Validation rules (import)

Order as in `BackupManager.importEncrypted`:

| # | Check | Failure |
|---|--------|---------|
| 1 | Envelope JSON parses to non-null | `"Invalid backup file"` |
| 2 | `envelope.format == "kinetiq-backup"` | `"Not a Kinetic Routines backup"` |
| 3 | GCM decrypt succeeds | Crypto exception (wrong passphrase / corrupt `data`) |
| 4 | Payload JSON parses to non-null | `"Invalid backup file"` |
| 5 | `payload.schemaVersion == 1` | `"Unsupported backup schema {n}"` |
| 6 | Every `log.habitId` ∈ `{ habit.id }` | `"Backup contains logs for unknown habits"` |
| 7 | Then `replaceAllData(...)` | DB replace |

### Soft mappings on import

| Case | Behavior |
|------|----------|
| Unknown `modality` string | `HabitModality.valueOf` fails → **`YES_NO`** |
| Missing `checkInQuestion` | Default `""` |
| Export passphrase &lt; 8 | Rejected at **export** only: `"Passphrase must be at least 8 characters"` |

Category / stack FK consistency is **not** pre-validated beyond log→habit ids (orphaned `categoryId` / `routineStackId` may be written as-is).

---

## Brand / compatibility notes

- Envelope `format` string **`kinetiq-backup`** is intentional and stable (renaming would reject all old backups).
- Envelope `version` and payload `schemaVersion` are independent; both are currently `1`.
- Settings UI filename `kinetic-routines-backup.kqbak` is cosmetic; content is the envelope JSON.

---

## Golden fixture for TypeScript importers

| File | Contents |
|------|----------|
| [`golden/backup-sample.meta.json`](golden/backup-sample.meta.json) | Passphrase, hex salt/IV, algorithm parameters |
| [`golden/backup-sample.payload.json`](golden/backup-sample.payload.json) | Exact decrypted UTF-8 plaintext (Moshi compact + `serializeNulls`) |
| [`golden/backup-sample.payload.pretty.json`](golden/backup-sample.payload.pretty.json) | Same payload, indented (for humans; not the encrypt input) |
| [`golden/backup-sample.kqbak`](golden/backup-sample.kqbak) | Envelope file bytes (encrypt of `payload.json` with fixed salt/IV) |

**Passphrase:** `kinetiq-golden-pass` (20 chars, ≥ 8)

**Fixed salt (16 bytes):** `00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f`  
**Fixed IV (12 bytes):** `10 11 12 13 14 15 16 17 18 19 1a 1b`

Regenerate:

```text
./gradlew :app:testDebugUnitTest --tests com.example.BackupGoldenFixtureTest
```

Importer test recipe:

1. Read `.kqbak` as UTF-8 → parse envelope.
2. Assert `format` / Base64-decode `salt` / `iv` match meta.
3. PBKDF2-HMAC-SHA256 → AES-256-GCM decrypt `data` (tag 128-bit, **no AAD**).
4. Assert plaintext UTF-8 bytes equal `backup-sample.payload.json` (exact), or deep-equal after JSON parse.

---

*End of BACKUP_FORMAT.md*
