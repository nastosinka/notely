package com.notely.service

import com.notely.domain.Folder
import com.notely.repository.FolderRepository
import java.util.UUID

class FolderService(
    private val repository: FolderRepository
) {

    fun getFolders(): List<Folder> {
        return repository.findAll()
    }

    fun createFolder(
        name: String,
        emoji: String
    ): Folder {
        val normalizedName = name.trim()
        val normalizedEmoji = emoji.trim()

        require(normalizedName.isNotBlank()) {
            "Введите название папки"
        }

        require(normalizedEmoji.isNotBlank()) {
            "Выберите эмодзи"
        }

        require(normalizedName.length <= 80) {
            "Название слишком длинное"
        }

        require(
            normalizedName != "." &&
                    normalizedName != ".." &&
                    '/' !in normalizedName &&
                    '\\' !in normalizedName &&
                    '\u0000' !in normalizedName
        ) {
            "Название содержит недопустимые символы"
        }

        val folder = Folder(
            id = UUID.randomUUID().toString(),
            name = normalizedName,
            emoji = normalizedEmoji
        )

        repository.save(folder)

        return folder
    }
}
