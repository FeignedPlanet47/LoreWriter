package ru.it.lorewriter.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.it.lorewriter.domain.repository.StoryRepository
import ru.it.lorewriter.domain.usecase.CreateStoryUseCase
import ru.it.lorewriter.domain.usecase.AcceptStoryShareLinkUseCase
import ru.it.lorewriter.domain.usecase.CreateStoryShareLinkUseCase
import ru.it.lorewriter.domain.usecase.DeleteCharacterUseCase
import ru.it.lorewriter.domain.usecase.ObserveCharactersUseCase
import ru.it.lorewriter.domain.usecase.UpdateManuscriptUseCase
import ru.it.lorewriter.domain.usecase.UpdateWordDocumentUseCase
import ru.it.lorewriter.domain.usecase.UpsertCharacterUseCase
import ru.it.lorewriter.domain.usecase.DeleteConnectionUseCase
import ru.it.lorewriter.domain.usecase.DeleteNodeUseCase
import ru.it.lorewriter.domain.usecase.DeleteStoryUseCase
import ru.it.lorewriter.domain.usecase.ExportStoryJsonUseCase
import ru.it.lorewriter.domain.usecase.ExportStoryTextUseCase
import ru.it.lorewriter.domain.usecase.GetStoryGraphUseCase
import ru.it.lorewriter.domain.usecase.RemoveStoryCollaboratorUseCase
import ru.it.lorewriter.domain.usecase.UpdateStoryCollaboratorUseCase
import ru.it.lorewriter.domain.usecase.GetStoryCollaboratorsUseCase
import ru.it.lorewriter.domain.usecase.ObserveStoriesUseCase
import ru.it.lorewriter.domain.usecase.ObserveStoryGraphUseCase
import ru.it.lorewriter.domain.usecase.StoryUseCases
import ru.it.lorewriter.domain.usecase.SyncStoriesUseCase
import ru.it.lorewriter.domain.usecase.UpdateNodePositionUseCase
import ru.it.lorewriter.domain.usecase.UpsertConnectionUseCase
import ru.it.lorewriter.domain.usecase.UpsertNodeUseCase
import ru.it.lorewriter.domain.usecase.ValidateStoryUseCase

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
