package repository.filesystem

import com.notely.repository.filesystem.FileFolderRepository
import com.notely.repository.filesystem.FileScoreRepository
import com.notely.domain.Folder
import com.notely.domain.Score
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FileStorageSafetyTest {
    private lateinit var temporaryDirectory: Path

    @BeforeTest
    fun setUp() {
        temporaryDirectory = Files.createTempDirectory("notely-safety-")
    }

    @AfterTest
    fun tearDown() {
        Files.walk(temporaryDirectory).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    private fun library(): Path = temporaryDirectory.resolve("Notely Library")
    private fun folder(name: String = "Скрипка") = Folder(UUID.randomUUID().toString(), name, "🎻")

    @Test
    fun `folder rejects traversal and absolute paths`() {
        val repository = FileFolderRepository(library())
        for (name in listOf("../outside", "sub/directory", "/tmp/outside", "..", ".notely-pending-1")) {
            assertFailsWith<IllegalArgumentException> { repository.save(folder(name)) }
        }
        assertEquals(emptyList(), repository.findAll())
    }

    @Test
    fun `folder creation preserves existing data on conflict`() {
        val repository = FileFolderRepository(library())
        val original = folder()
        repository.save(original)
        val metadata = library().resolve("Скрипка/.notely-folder.json")
        val before = Files.readString(metadata)
        assertFailsWith<IllegalArgumentException> { repository.save(folder()) }
        assertEquals(before, Files.readString(metadata))
    }

    @Test
    fun `old name based directories remain readable`() {
        val repository = FileFolderRepository(library())
        val original = folder()
        repository.save(original)
        assertEquals(listOf(original), repository.findAll())
    }

    @Test
    fun `broken folder metadata does not prevent loading other folders`() {
        val repository = FileFolderRepository(library())
        val original = folder()
        repository.save(original)
        val broken = library().resolve("Повреждённая")
        Files.createDirectory(broken)
        Files.writeString(broken.resolve(".notely-folder.json"), "not json")
        assertEquals(listOf(original), repository.findAll())
    }

    @Test
    fun `symlink folder cannot be opened`() {
        val repository = FileFolderRepository(library())
        val outside = temporaryDirectory.resolve("external")
        Files.createDirectory(outside)
        Files.createSymbolicLink(library().resolve("Ссылка"), outside)
        assertTrue(repository.findAll().isEmpty())
        assertFailsWith<IllegalArgumentException> { repository.save(folder("Ссылка")) }
    }

    @Test
    fun `import preserves original document and its bytes`() {
        val path = library()
        val folderRepository = FileFolderRepository(path)
        val savedFolder = folder()
        folderRepository.save(savedFolder)
        val source = temporaryDirectory.resolve("score.pdf")
        Files.write(source, "%PDF-test".toByteArray())
        val scoreRepository = FileScoreRepository(path)
        val imported = scoreRepository.importFile(savedFolder, source)
        assertEquals(listOf(imported), scoreRepository.findAll(savedFolder))
        assertTrue(Files.readAllBytes(scoreRepository.getFilePath(savedFolder, imported)).contentEquals(Files.readAllBytes(source)))
    }

    @Test
    fun `malicious score metadata cannot escape library`() {
        val path = library()
        val folderRepository = FileFolderRepository(path)
        val savedFolder = folder()
        folderRepository.save(savedFolder)
        val scoreRepository = FileScoreRepository(path)
        val scoreId = UUID.randomUUID().toString()
        val directory = path.resolve("Скрипка").resolve(scoreId)
        Files.createDirectory(directory)
        Files.writeString(directory.resolve("original.pdf"), "%PDF-test")
        val malicious = Score(scoreId, savedFolder.id, "Bad", "bad.pdf", "../../outside.pdf")
        Files.writeString(directory.resolve(".notely-score.json"), Json.encodeToString(malicious))
        assertTrue(scoreRepository.findAll(savedFolder).isEmpty())
        assertFailsWith<IllegalArgumentException> { scoreRepository.getFilePath(savedFolder, malicious) }
    }

    @Test
    fun `pending directories are never presented as scores`() {
        val path = library()
        val folderRepository = FileFolderRepository(path)
        val savedFolder = folder()
        folderRepository.save(savedFolder)
        val pending = path.resolve("Скрипка/.notely-pending-abcd")
        Files.createDirectory(pending)
        Files.writeString(pending.resolve("original.pdf"), "incomplete")
        assertTrue(FileScoreRepository(path).findAll(savedFolder).isEmpty())
        assertTrue(Files.exists(pending.resolve("original.pdf")))
    }
}
