package com.notely.ui.dialog

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import java.util.concurrent.atomic.AtomicReference
import javax.swing.filechooser.FileNameExtensionFilter

class ScoreFilePicker {

    private val preferences = Preferences.userRoot()
        .node("com/notely")

    fun chooseFile(): Path? {
        if (SwingUtilities.isEventDispatchThread()) {
            return showChooser()
        }
        val selection = AtomicReference<Path?>()
        val failure = AtomicReference<Throwable?>()
        SwingUtilities.invokeAndWait {
            try {
                selection.set(showChooser())
            } catch (exception: Throwable) {
                failure.set(exception)
            }
        }
        failure.get()?.let { throw IllegalStateException("Не удалось открыть выбор файла", it) }
        return selection.get()
    }

    private fun showChooser(): Path? {
        val defaultDirectory = Paths.get(
            System.getProperty("user.home"),
            "Documents"
        )

        val savedDirectory = preferences
            .get(LAST_DIRECTORY_KEY, null)
            ?.let { runCatching { Paths.get(it) }.getOrNull() }
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
