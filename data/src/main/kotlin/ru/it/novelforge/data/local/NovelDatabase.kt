package ru.it.novelforge.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        StoryEntity::class,
        CharacterEntity::class,
        StoryNodeEntity::class,
        StoryConnectionEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class NovelDatabase : RoomDatabase() {
    abstract fun storyDao(): StoryDao

    companion object {
        val MIGRATION_1_2 = Migration(1, 2) { database ->
            database.execSQL(
                "ALTER TABLE story_nodes ADD COLUMN formattingJson TEXT NOT NULL DEFAULT '[]'"
            )
        }

        val MIGRATION_2_3 = Migration(2, 3) { database ->
            database.execSQL(
                "ALTER TABLE stories ADD COLUMN manuscript TEXT NOT NULL DEFAULT ''"
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS characters (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    storyId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    role TEXT NOT NULL,
                    description TEXT NOT NULL,
                    personality TEXT NOT NULL,
                    appearance TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    FOREIGN KEY(storyId) REFERENCES stories(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )

            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_characters_storyId ON characters(storyId)"
            )
        }
    }
}
