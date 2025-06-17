package com.example.langbridgai.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Constants for the database and table schema
object TranslationContract {
    object TranslationEntry {
        const val TABLE_NAME = "translation_history"
        const val COLUMN_ID = "id"
        const val COLUMN_ORIGINAL_TEXT = "original_text"
        const val COLUMN_TRANSLATED_TEXT = "translated_text"
        const val COLUMN_FROM_LANG = "from_lang"
        const val COLUMN_TO_LANG = "to_lang"
        const val COLUMN_TIMESTAMP = "timestamp"
    }
}

/**
 * SQLiteOpenHelper for managing database creation and version management for translation history.
 */
class TranslationDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "translation_history.db"
        const val DATABASE_VERSION = 1 // Database version

        // SQL statement to create the translation history table
        private const val SQL_CREATE_ENTRIES =
            "CREATE TABLE ${TranslationContract.TranslationEntry.TABLE_NAME} (" +
                    "${TranslationContract.TranslationEntry.COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${TranslationContract.TranslationEntry.COLUMN_ORIGINAL_TEXT} TEXT NOT NULL," +
                    "${TranslationContract.TranslationEntry.COLUMN_TRANSLATED_TEXT} TEXT NOT NULL," +
                    "${TranslationContract.TranslationEntry.COLUMN_FROM_LANG} TEXT," + // Source language code
                    "${TranslationContract.TranslationEntry.COLUMN_TO_LANG} TEXT," +   // Target language code
                    "${TranslationContract.TranslationEntry.COLUMN_TIMESTAMP} TEXT NOT NULL)" // Timestamp of translation

        // SQL statement to drop (delete) the table if it exists
        private const val SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS ${TranslationContract.TranslationEntry.TABLE_NAME}"
    }

    /**
     * Called when the database is created for the first time.
     * This is where the schema for the database is created.
     */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    /**
     * Called when the database needs to be upgraded.
     * This method is used to handle schema changes (e.g., adding a new column, dropping a table).
     * For this simple example, it just drops and recreates the table, discarding old data.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    /**
     * Called when the database needs to be downgraded.
     * In this case, it calls onUpgrade to re-create the database.
     */
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    /**
     * Inserts a new translation record into the database.
     * @param originalText The text that was translated (source text).
     * @param translatedText The result of the translation.
     * @param fromLang The language code of the original text (can be "auto" or null).
     * @param toLang The language code of the translated text.
     * @return The row ID of the newly inserted row, or -1 if an error occurred.
     */
    fun insertTranslation(
        originalText: String,
        translatedText: String,
        fromLang: String?, // Nullable for 'auto' or unspecified source
        toLang: String
    ): Long {
        val db = writableDatabase // Get a writable database instance

        // Create a new map of values, where column names are the keys
        val values = ContentValues().apply {
            put(TranslationContract.TranslationEntry.COLUMN_ORIGINAL_TEXT, originalText)
            put(TranslationContract.TranslationEntry.COLUMN_TRANSLATED_TEXT, translatedText)
            put(TranslationContract.TranslationEntry.COLUMN_FROM_LANG, fromLang ?: "") // Store empty string if null/auto
            put(TranslationContract.TranslationEntry.COLUMN_TO_LANG, toLang)
            // Store current timestamp as a string in "YYYY-MM-DD HH:MM:SS" format
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            put(TranslationContract.TranslationEntry.COLUMN_TIMESTAMP, dateFormat.format(Date()))
        }

        // Insert the new row, returning the primary key value of the new row
        val newRowId = db.insert(TranslationContract.TranslationEntry.TABLE_NAME, null, values)
        db.close() // Close the database connection to free up resources
        return newRowId
    }

    /**
     * Retrieves all translation history records from the database, ordered by timestamp (latest first).
     * @return A list of TranslationHistory objects.
     */
    fun getAllTranslations(): List<TranslationHistory> {
        val translations = mutableListOf<TranslationHistory>()
        val db = readableDatabase // Get a readable database instance

        // Define a projection specifying which columns to retrieve
        val projection = arrayOf(
            TranslationContract.TranslationEntry.COLUMN_ID,
            TranslationContract.TranslationEntry.COLUMN_ORIGINAL_TEXT,
            TranslationContract.TranslationEntry.COLUMN_TRANSLATED_TEXT,
            TranslationContract.TranslationEntry.COLUMN_FROM_LANG,
            TranslationContract.TranslationEntry.COLUMN_TO_LANG,
            TranslationContract.TranslationEntry.COLUMN_TIMESTAMP
        )

        // Define the sort order: latest translations first
        val sortOrder = "${TranslationContract.TranslationEntry.COLUMN_TIMESTAMP} DESC"

        // Perform the query
        val cursor = db.query(
            TranslationContract.TranslationEntry.TABLE_NAME, // The table to query
            projection, // The columns to return
            null, // The columns for the WHERE clause (null means all rows)
            null, // The values for the WHERE clause
            null, // Don't group the rows
            null, // Don't filter by row groups
            sortOrder // The sort order
        )

        // Iterate through the results and create TranslationHistory objects
        with(cursor) {
            while (moveToNext()) {
                val itemId = getLong(getColumnIndexOrThrow(TranslationContract.TranslationEntry.COLUMN_ID))
                val originalText = getString(getColumnIndexOrThrow(TranslationContract.TranslationEntry.COLUMN_ORIGINAL_TEXT))
                val translatedText = getString(getColumnIndexOrThrow(TranslationContract.TranslationEntry.COLUMN_TRANSLATED_TEXT))
                val fromLang = getString(getColumnIndexOrThrow(TranslationContract.TranslationEntry.COLUMN_FROM_LANG))
                val toLang = getString(getColumnIndexOrThrow(TranslationContract.TranslationEntry.COLUMN_TO_LANG))
                val timestamp = getString(getColumnIndexOrThrow(TranslationContract.TranslationEntry.COLUMN_TIMESTAMP))

                translations.add(TranslationHistory(itemId, originalText, translatedText, fromLang, toLang, timestamp))
            }
        }
        cursor.close() // Always close the cursor after use
        db.close() // Close the database connection
        return translations
    }

    /**
     * Deletes a specific translation record by its ID.
     * @param id The ID of the record to delete.
     * @return The number of rows affected (should be 1 if successful, 0 otherwise).
     */
    fun deleteTranslation(id: Long): Int {
        val db = writableDatabase
        val selection = "${TranslationContract.TranslationEntry.COLUMN_ID} LIKE ?"
        val selectionArgs = arrayOf(id.toString())
        val deletedRows = db.delete(TranslationContract.TranslationEntry.TABLE_NAME, selection, selectionArgs)
        db.close()
        return deletedRows
    }
}

/**
 * Data class representing a single entry in the translation history.
 */
data class TranslationHistory(
    val id: Long,
    val originalText: String,
    val translatedText: String,
    val fromLang: String,
    val toLang: String,
    val timestamp: String
)
