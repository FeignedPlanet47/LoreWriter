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
    version = 6,
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


        val MIGRATION_3_4 = Migration(3, 4) { database ->
            database.execSQL(
                "ALTER TABLE stories ADD COLUMN wordDocument TEXT NOT NULL DEFAULT ''"
            )
        }


        val MIGRATION_4_5 = Migration(4, 5) { database ->
            database.execSQL("ALTER TABLE stories ADD COLUMN ownerUserId TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE stories ADD COLUMN cloudId TEXT")
            database.execSQL("ALTER TABLE stories ADD COLUMN cloudRevision INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE stories ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 1")
            database.execSQL("ALTER TABLE stories ADD COLUMN pendingDelete INTEGER NOT NULL DEFAULT 0")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_stories_cloudId ON stories(cloudId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_stories_ownerUserId ON stories(ownerUserId)")
        }


        val MIGRATION_5_6 = Migration(5, 6) { database ->
            database.execSQL(
                "ALTER TABLE stories ADD COLUMN accessRole TEXT NOT NULL DEFAULT 'OWNER'"
            )
            database.execSQL("DROP INDEX IF EXISTS index_stories_cloudId")
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_stories_cloudId_ownerUserId " +
                        "ON stories(cloudId, ownerUserId)"
            )
        }
    }
}
