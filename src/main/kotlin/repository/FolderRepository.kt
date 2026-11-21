package com.notely.repository

import com.notely.domain.Folder

interface FolderRepository {
    fun findAll(): List<Folder>

    fun save(folder: Folder)
}
