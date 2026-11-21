package com.notely.ui.dialog

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class ScoreFilePicker {

    private val preferences = Preferences.userRoot()
        .node("com/notely")

    fun chooseFile(): Path? {
        val defaultDirectory = Paths.get(
            System.getProperty("user.home"),
            "Documents"
        )

        val savedDirectory = preferences
            .get(LAST_DIRECTORY_KEY, null)
            ?.let(Paths::get)
            ?.takeIf(Files::isDirectory)

        val initialDirectory = savedDirectory
            ?: defaultDirectory.takeIf(Files::isDirectory)
            ?: Paths.get(System.getProperty("user.home"))

        val chooser = JFileChooser(initialDirectory.toFile()).apply {
            dialogTitle = "Импортировать ноты"
            fileSelectionMode = JFileChooser.FILES_ONLY
            isMultiSelectionEnabled = false
            fileFilter = FileNameExtensionFilter(
                "Ноты (PDF, JPG, PNG)",
                "pdf", "jpg", "jpeg", "png"
            )
            isAcceptAllFileFilterUsed = false
        }

        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            return null
        }

        val selectedPath = chooser.selectedFile.toPath()

        preferences.put(
            LAST_DIRECTORY_KEY,
            selectedPath.parent.toString()
        )

        return selectedPath
    }

    companion object {
        private const val LAST_DIRECTORY_KEY =
            "lastImportDirectory"
    }
}
