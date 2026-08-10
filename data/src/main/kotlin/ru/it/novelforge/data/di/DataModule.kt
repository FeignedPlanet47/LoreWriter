package ru.it.novelforge.data.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.it.novelforge.data.local.NovelDatabase
import ru.it.novelforge.data.local.StoryDao
import ru.it.novelforge.data.repository.StoryRepositoryImpl
import ru.it.novelforge.domain.repository.StoryRepository

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NovelDatabase =
        Room.databaseBuilder(
            context,
            NovelDatabase::class.java,
            "novel_forge.db"
        )
            .addMigrations(
                NovelDatabase.MIGRATION_1_2,
                NovelDatabase.MIGRATION_2_3
            )
            .build()

    @Provides
    fun provideStoryDao(database: NovelDatabase): StoryDao = database.storyDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindStoryRepository(impl: StoryRepositoryImpl): StoryRepository
}
