package additional

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

fun main() {

    val wordsFile: File = File("words.txt")

    try {
        if (!wordsFile.exists()) {
            wordsFile.createNewFile()
        }
    } catch (e: IOException) {
        println("Ошибка при создании файла: ${e.message}")
    }

    try {
        for (word in wordsFile.readLines()) {
            println(word)
        }
    } catch (e: FileNotFoundException) {
        println("Ошибка вывода строки: ${e.message}")
    }
}