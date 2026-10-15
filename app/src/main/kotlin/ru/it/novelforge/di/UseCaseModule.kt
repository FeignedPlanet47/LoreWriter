package ru.it.novelforge.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.it.novelforge.domain.repository.StoryRepository
import ru.it.novelforge.domain.usecase.CreateStoryUseCase
import ru.it.novelforge.domain.usecase.AcceptStoryShareLinkUseCase
import ru.it.novelforge.domain.usecase.CreateStoryShareLinkUseCase
import ru.it.novelforge.domain.usecase.DeleteCharacterUseCase
import ru.it.novelforge.domain.usecase.ObserveCharactersUseCase
import ru.it.novelforge.domain.usecase.UpdateManuscriptUseCase
import ru.it.novelforge.domain.usecase.UpdateWordDocumentUseCase
import ru.it.novelforge.domain.usecase.UpsertCharacterUseCase
import ru.it.novelforge.domain.usecase.DeleteConnectionUseCase
import ru.it.novelforge.domain.usecase.DeleteNodeUseCase
import ru.it.novelforge.domain.usecase.DeleteStoryUseCase
import ru.it.novelforge.domain.usecase.ExportStoryJsonUseCase
import ru.it.novelforge.domain.usecase.ExportStoryTextUseCase
import ru.it.novelforge.domain.usecase.GetStoryGraphUseCase
import ru.it.novelforge.domain.usecase.RemoveStoryCollaboratorUseCase
import ru.it.novelforge.domain.usecase.UpdateStoryCollaboratorUseCase
import ru.it.novelforge.domain.usecase.GetStoryCollaboratorsUseCase
import ru.it.novelforge.domain.usecase.ObserveStoriesUseCase
import ru.it.novelforge.domain.usecase.ObserveStoryGraphUseCase
import ru.it.novelforge.domain.usecase.StoryUseCases
import ru.it.novelforge.domain.usecase.SyncStoriesUseCase
import ru.it.novelforge.domain.usecase.UpdateNodePositionUseCase
import ru.it.novelforge.domain.usecase.UpsertConnectionUseCase
import ru.it.novelforge.domain.usecase.UpsertNodeUseCase
import ru.it.novelforge.domain.usecase.ValidateStoryUseCase

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    @Singleton
    fun provideStoryUseCases(repository: StoryRepository): StoryUseCases = StoryUseCases(
        observeStories = ObserveStoriesUseCase(repository),
        createStory = CreateStoryUseCase(repository),
        deleteStory = DeleteStoryUseCase(repository),
        observeStoryGraph = ObserveStoryGraphUseCase(repository),
        getStoryGraph = GetStoryGraphUseCase(repository),
        observeCharacters = ObserveCharactersUseCase(repository),
        updateManuscript = UpdateManuscriptUseCase(repository),
        updateWordDocument = UpdateWordDocumentUseCase(repository),
        upsertCharacter = UpsertCharacterUseCase(repository),
        deleteCharacter = DeleteCharacterUseCase(repository),
        upsertNode = UpsertNodeUseCase(repository),
        deleteNode = DeleteNodeUseCase(repository),
        updateNodePosition = UpdateNodePositionUseCase(repository),
        upsertConnection = UpsertConnectionUseCase(repository),
        deleteConnection = DeleteConnectionUseCase(repository),
        createShareLink = CreateStoryShareLinkUseCase(repository),
        acceptShareLink = AcceptStoryShareLinkUseCase(repository),
        getCollaborators = GetStoryCollaboratorsUseCase(repository),
        updateCollaborator = UpdateStoryCollaboratorUseCase(repository),
        removeCollaborator = RemoveStoryCollaboratorUseCase(repository),
        syncStories = SyncStoriesUseCase(repository),
        validateStory = ValidateStoryUseCase(),
        exportText = ExportStoryTextUseCase(),
        exportJson = ExportStoryJsonUseCase()
    )
}
