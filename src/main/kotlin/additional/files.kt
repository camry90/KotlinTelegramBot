package additional

import java.io.File

fun main() {

    val wordsFile: File = File("words.txt")
    wordsFile.createNewFile()

    for (word in wordsFile.readLines()) {
        println(word)
    }
}