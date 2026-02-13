package additional

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

data class Word(
    val original: String,
    val translate: String,
    val correctAnswerCount: Int = 0,
)

fun main() {

    val wordsFile: File = File("words.txt")

    try {
        if (!wordsFile.exists()) {
            wordsFile.createNewFile()
        }
    } catch (e: IOException) {
        println("Ошибка при создании файла: ${e.message}")
    }

    val dictionary: MutableList<Word> = mutableListOf()

    try {
        for (line in wordsFile.readLines()) {
            val parts = line.split("|")
            if (parts.size < 2) continue
            val correct = parts.getOrNull(2)?.toIntOrNull() ?: 0
            val word = Word(parts[0], parts[1], correct)
            dictionary.add(word)
        }
        dictionary.forEach { println(it) }
    } catch (e: FileNotFoundException) {
        println("Ошибка вывода строки: ${e.message}")
    }
}