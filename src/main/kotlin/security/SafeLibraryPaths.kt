package security

import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.util.UUID

class SafeLibraryPaths(libraryPath: Path) {
    val root: Path

    init {
        Files.createDirectories(libraryPath)
        require(!Files.isSymbolicLink(libraryPath)) { "Библиотека не может быть символической ссылкой" }
        root = libraryPath.toRealPath()
        require(Files.isDirectory(root, NOFOLLOW_LINKS)) { "Библиотека недоступна" }
    }

    fun validateFolderName(name: String): String {
        require(name.isNotBlank() && name == name.trim() && name.length <= 80) { "Некорректное название папки" }
        require(name != "." && name != ".." && name.none { it == '/' || it == '\\' || it == ':' || it.isISOControl() }) {
            "Недопустимые символы в названии папки"
        }
        require(!name.startsWith(".notely-")) { "Зарезервированное название папки" }
        return name
    }

    fun validateId(id: String): String {
        require(UUID.fromString(id).toString() == id) { "Некорректный идентификатор" }
        return id
    }

    fun folder(name: String): Path = child(root, validateFolderName(name))

    fun score(folder: Path, id: String): Path = child(folder, validateId(id))

    fun file(parent: Path, name: String): Path {
        require(name.isNotBlank() && name != "." && name != ".." &&
                name.none { it == '/' || it == '\\' || it.isISOControl() }) { "Недопустимое имя файла" }
        return child(parent, name)
    }

    fun directory(path: Path): Path {
        checkWithinRoot(path)
        require(Files.isDirectory(path, NOFOLLOW_LINKS)) { "Папка отсутствует или является ссылкой" }
        return path
    }

    fun regularFile(path: Path): Path {
        checkWithinRoot(path)
        require(Files.isRegularFile(path, NOFOLLOW_LINKS)) { "Файл отсутствует или является ссылкой" }
        return path
    }

    private fun child(parent: Path, name: String): Path {
        directory(parent)
        val resolved = parent.resolve(name).normalize()
        require(resolved.parent == parent.normalize()) { "Недопустимый путь" }
        require(!Files.isSymbolicLink(resolved)) { "Символические ссылки запрещены" }
        return resolved
    }

    private fun checkWithinRoot(path: Path) {
        val absolute = path.toAbsolutePath().normalize()
        require(absolute.startsWith(root)) { "Путь находится вне библиотеки" }
        var current = root
        for (segment in root.relativize(absolute)) {
            current = current.resolve(segment)
            require(!Files.isSymbolicLink(current)) { "Символические ссылки запрещены" }
        }
    }
}
