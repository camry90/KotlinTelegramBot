package additional

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

data class Word(
    val original: String,
    val translate: String,
    val correctAnswer: Int = 0,
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
            val line = line.split("|")
            val word = Word(line[0], line[1], line[2].toIntOrNull() ?: 0)
            dictionary.add(word)
        }
        dictionary.forEach { println(it) }
    } catch (e: FileNotFoundException) {
        println("Ошибка вывода строки: ${e.message}")
    }
}