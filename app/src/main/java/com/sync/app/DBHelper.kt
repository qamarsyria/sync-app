package com.sync.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(ctx: Context) : SQLiteOpenHelper(ctx, "sync.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT,
                title TEXT,
                body TEXT,
                timestamp INTEGER
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
        db.execSQL("DROP TABLE IF EXISTS logs")
        onCreate(db)
    }

    fun insert(type: String, title: String, body: String) {
        val values = ContentValues().apply {
            put("type", type)
            put("title", title)
            put("body", body)
            put("timestamp", System.currentTimeMillis())
        }
        writableDatabase.insert("logs", null, values)
    }

    fun getAll(): List<LogEntry> {
        val list = mutableListOf<LogEntry>()
        val cursor = readableDatabase.rawQuery(
            "SELECT id, type, title, body, timestamp FROM logs ORDER BY timestamp DESC LIMIT 1000",
            null
        )
        while (cursor.moveToNext()) {
            list.add(
                LogEntry(
                    cursor.getLong(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getLong(4)
                )
            )
        }
        cursor.close()
        return list
    }

    fun clearAll() {
        writableDatabase.delete("logs", null, null)
    }
}

data class LogEntry(
    val id: Long,
    val type: String,
    val title: String,
    val body: String,
    val timestamp: Long
)
