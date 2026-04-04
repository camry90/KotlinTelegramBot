package additional

import java.io.File
import java.io.FileNotFoundException

class FileUserDictionary(
    private val fileName: String = FILE_NAME,
    private val learningThreshold: Int = 3,
): IUserDictionary {

    private val dictionary = try {
        loadDictionary()
    } catch (e: Exception) {
        throw IllegalArgumentException("Некорректный файл")
    }

    private fun loadDictionary(): MutableList<Word> {
        val wordsFile = File(fileName)
        if (!wordsFile.exists()) {
            File(FILE_NAME).copyTo(wordsFile)
        }

        val dictionary: MutableList<Word> = mutableListOf()

        try {
            for (line in wordsFile.readLines()) {
                val parts = line.split("|")
                if (parts.size < 2) continue
                val correct = parts.getOrNull(2)?.toIntOrNull() ?: 0
                val image = parts.getOrNull(3)?.ifEmpty { null }
                val fileIdCheck = parts.getOrNull(4)?.ifEmpty { null }
                val word = Word(parts[0], parts[1], correct, imageHint = image, fileId = fileIdCheck)
                dictionary.add(word)
            }

            if (fileName != FILE_NAME) {
                val masterWords = loadMasterDictionary()
                var updated = false
                dictionary.replaceAll { word ->
                    if (word.imageHint == null) {
                        val master = masterWords.find { it.original == word.original }
                        if (master?.imageHint != null) {
                            updated = true
                            word.copy(imageHint = master.imageHint)
                        } else word
                    } else word
                }
                if (updated) {
                    val string = dictionary.joinToString(separator = "\n") {
                        "${it.original}|${it.translate}|${it.correctAnswerCount}|${it.imageHint ?: ""}|${it.fileId ?: ""}"
                    }
                    File(fileName).writeText(string)
                }
            }
        } catch (e: FileNotFoundException) {
            println("Ошибка вывода строки: ${e.message}")
        }
        return dictionary
    }

    private fun loadMasterDictionary(): MutableList<Word> {
        val result = mutableListOf<Word>()
        try {
            for (line in File(FILE_NAME).readLines()) {
                val parts = line.split("|")
                if (parts.size < 2) continue
                val image = parts.getOrNull(3)?.ifEmpty { null }
                result.add(Word(parts[0], parts[1], imageHint = image))
            }
        } catch (e: FileNotFoundException) {
            println("Мастер-словарь не найден: ${e.message}")
        }
        return result
    }

    private fun saveDictionary() {
        val string = dictionary.joinToString(separator = "\n") { it ->
            "${it.original}|${it.translate}|${it.correctAnswerCount}|${it.imageHint ?: ""}|${it.fileId ?: ""}"
        }
        File(fileName).writeText(string)
    }

    override fun resetUserProgress() {
        dictionary.forEach { it.correctAnswerCount = 0 }
        saveDictionary()
    }

    override fun getSize(): Int = dictionary.size

    override fun getNumOfLearnedWords(): Int {
        val numberOfWords = dictionary.filter { it.correctAnswerCount >= learningThreshold }.size
        return numberOfWords
    }

    override fun getLearnedWords(): List<Word> = dictionary.filter { it.correctAnswerCount >= learningThreshold }

    override fun getUnlearnedWords(): List<Word> = dictionary.filter { it.correctAnswerCount < learningThreshold }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        dictionary.find { it.original == word }?.correctAnswerCount = correctAnswersCount
        saveDictionary()
    }

    override fun getCorrectAnswersCount(word: String): Int {
        return dictionary.find { it.original == word }?.correctAnswerCount ?: 0
    }

    override fun saveFileId(word: String, fileId: String) {
        dictionary.find { it.original == word }?.fileId = fileId
        saveDictionary()
    }

}