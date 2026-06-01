package com.linkyun.her;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

final class MemoryStore extends SQLiteOpenHelper {
    MemoryStore(Activity activity) {
        super(activity, "her_memory.db", null, 1);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "agent_name TEXT NOT NULL," +
                "started_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "session_id INTEGER NOT NULL," +
                "role TEXT NOT NULL," +
                "content TEXT NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "compacted INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE memories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "session_id INTEGER NOT NULL," +
                "kind TEXT NOT NULL," +
                "content TEXT NOT NULL," +
                "source_first_message_id INTEGER," +
                "source_last_message_id INTEGER," +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE VIRTUAL TABLE memory_fts USING fts4(content, kind)");
        db.execSQL("CREATE INDEX idx_messages_session_compacted ON messages(session_id, compacted, id)");
        db.execSQL("CREATE INDEX idx_memories_kind ON memories(kind, created_at)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS memory_fts");
        db.execSQL("DROP TABLE IF EXISTS memories");
        db.execSQL("DROP TABLE IF EXISTS messages");
        db.execSQL("DROP TABLE IF EXISTS sessions");
        onCreate(db);
    }

    long startSession(String agentName) {
        ContentValues values = new ContentValues();
        values.put("agent_name", agentName);
        values.put("started_at", System.currentTimeMillis());
        return getWritableDatabase().insert("sessions", null, values);
    }

    void insertMessage(long sessionId, String role, String content) {
        ContentValues values = new ContentValues();
        values.put("session_id", sessionId);
        values.put("role", role);
        values.put("content", content);
        values.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insert("messages", null, values);
    }

    void insertMemory(long sessionId, String kind, String content, long firstId, long lastId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("session_id", sessionId);
        values.put("kind", kind);
        values.put("content", content);
        values.put("source_first_message_id", firstId);
        values.put("source_last_message_id", lastId);
        values.put("created_at", System.currentTimeMillis());
        db.insert("memories", null, values);

        ContentValues fts = new ContentValues();
        fts.put("content", content);
        fts.put("kind", kind);
        db.insert("memory_fts", null, fts);
    }

    MemoryChunk unsummarizedChunk(long sessionId, int minCount, int minChars) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, role, content FROM messages WHERE session_id=? AND compacted=0 ORDER BY id ASC",
                new String[]{String.valueOf(sessionId)});
        long first = 0;
        long last = 0;
        int count = 0;
        StringBuilder transcript = new StringBuilder();
        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                if (first == 0) first = id;
                last = id;
                count++;
                transcript.append(cursor.getString(1)).append(": ")
                        .append(cursor.getString(2)).append('\n');
            }
        } finally {
            cursor.close();
        }
        if (count < minCount && transcript.length() < minChars) return null;
        return new MemoryChunk(first, last, transcript.toString());
    }

    void markCompacted(long lastId) {
        ContentValues values = new ContentValues();
        values.put("compacted", 1);
        getWritableDatabase().update("messages", values, "id<=?", new String[]{String.valueOf(lastId)});
    }

    String relevantMemory(String query) {
        StringBuilder builder = new StringBuilder();
        if (query != null && !query.trim().isEmpty()) {
            String match = sanitizeFts(query);
            if (!match.isEmpty()) {
                Cursor cursor = getReadableDatabase().rawQuery(
                        "SELECT kind, content FROM memory_fts WHERE memory_fts MATCH ? LIMIT 4",
                        new String[]{match});
                try {
                    while (cursor.moveToNext()) {
                        if (isTransientMemory(cursor.getString(1))) continue;
                        builder.append("- [").append(cursor.getString(0)).append("] ")
                                .append(cursor.getString(1)).append('\n');
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        Cursor recent = getReadableDatabase().rawQuery(
                "SELECT kind, content FROM memories ORDER BY id DESC LIMIT 6", null);
        try {
            while (recent.moveToNext()) {
                if (isTransientMemory(recent.getString(1))) continue;
                builder.append("- [").append(recent.getString(0)).append("] ")
                        .append(recent.getString(1)).append('\n');
            }
        } finally {
            recent.close();
        }
        return builder.toString();
    }

    String latestTone() {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT content FROM memories WHERE kind='tone' ORDER BY id DESC LIMIT 1", null);
        try {
            if (cursor.moveToFirst()) return cursor.getString(0);
        } finally {
            cursor.close();
        }
        return "保持温柔大姐姐语气：成熟、关照、亲近但有边界。";
    }

    void resetAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("memory_fts", null, null);
        db.delete("memories", null, null);
        db.delete("messages", null, null);
        db.delete("sessions", null, null);
    }

    void clearSession(long sessionId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("messages", "session_id=?", new String[]{String.valueOf(sessionId)});
        db.delete("sessions", "id=?", new String[]{String.valueOf(sessionId)});
    }

    private String sanitizeFts(String query) {
        String normalized = query.replaceAll("[^\\p{L}\\p{N}\\s]", " ").trim();
        if (normalized.isEmpty()) return "";
        String[] parts = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.length() < 2) continue;
            if (builder.length() > 0) builder.append(' ');
            builder.append(part);
        }
        return builder.toString();
    }

    private boolean isTransientMemory(String content) {
        return WeatherSkill.isTransientMemory(content) || NewsSkill.isTransientMemory(content);
    }
}
