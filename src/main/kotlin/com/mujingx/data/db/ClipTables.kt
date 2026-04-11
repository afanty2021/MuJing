package com.mujingx.data.db

import java.sql.Connection
import java.sql.SQLException

object ClipTables {
    fun createTables(connection: Connection) {
        try {
            // Create clip_collections table
            connection.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS clip_collections (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    description TEXT,
                    created_at INTEGER NOT NULL
                )
            """)

            // Create clips table
            connection.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS clips (
                    id TEXT PRIMARY KEY,
                    video_path TEXT NOT NULL,
                    start_time INTEGER NOT NULL,
                    end_time INTEGER NOT NULL,
                    subtitle_text TEXT NOT NULL,
                    translated_text TEXT,
                    note TEXT,
                    video_clip_path TEXT,
                    collection_id TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    FOREIGN KEY (collection_id) REFERENCES clip_collections(id) ON DELETE CASCADE
                )
            """)

            // Create clip_tags table
            connection.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS clip_tags (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL,
                    color TEXT
                )
            """)

            // Create clip_tag_relations table
            connection.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS clip_tag_relations (
                    clip_id TEXT NOT NULL,
                    tag_id INTEGER NOT NULL,
                    PRIMARY KEY (clip_id, tag_id),
                    FOREIGN KEY (clip_id) REFERENCES clips(id) ON DELETE CASCADE,
                    FOREIGN KEY (tag_id) REFERENCES clip_tags(id) ON DELETE CASCADE
                )
            """)

            // Create indexes for better performance
            connection.createStatement().execute("""
                CREATE INDEX IF NOT EXISTS idx_clips_collection_id ON clips(collection_id)
            """)

            connection.createStatement().execute("""
                CREATE INDEX IF NOT EXISTS idx_clip_tag_relations_clip_id ON clip_tag_relations(clip_id)
            """)

            connection.createStatement().execute("""
                CREATE INDEX IF NOT EXISTS idx_clip_tag_relations_tag_id ON clip_tag_relations(tag_id)
            """)

        } catch (e: SQLException) {
            throw SQLException("Failed to create clip tables", e)
        }
    }
}