package additional

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

const val LEARNED_NUMBER = 3
const val OPTIONS_NUMBER = 4

data class Word(
    val original: String,
    val translate: String,
    val correctAnswerCount: Int = 0,
)

fun loadDictionary(): MutableList<Word> {

    val wordsFile = File("words.txt")

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
    } catch (e: FileNotFoundException) {
        println("Ошибка вывода строки: ${e.message}")
    }
    return dictionary
}

fun main() {

    val dictionary = loadDictionary()

    while (true) {
        println(
            "Меню: \n" +
                    "1 – Учить слова\n" +
                    "2 – Статистика\n" +
                    "0 – Выход"
        )

        val userChoice = readlnOrNull()?.toIntOrNull()

        when (userChoice) {
            1 -> {
                val notLearnedList = dictionary.filter { it.correctAnswerCount < LEARNED_NUMBER }
                if (notLearnedList.isNotEmpty()) {
                    val questionWords = notLearnedList.map { it.translate }
                        .shuffled()
                        .take(OPTIONS_NUMBER)

                    val correctWord = notLearnedList.random()
                    val correctAnswer = correctWord.translate
                    val options = (questionWords.filter { it != correctAnswer } + correctAnswer).shuffled()

                    println("${correctWord.original}:")
                    options.forEachIndexed { index, option ->
                        println("${index + 1} - $option")
                    }

                    val userAnswer = readlnOrNull()?.toIntOrNull()

                } else {
                    println("Все слова в словаре выучены")
                }
                continue
            }

            2 -> {
                val learnedCount = dictionary.filter { it.correctAnswerCount >= LEARNED_NUMBER }.size
                val totalCount = dictionary.count()
                val percent = if (totalCount > 0) learnedCount * 100 / totalCount else 0
                println("Выучено $learnedCount из $totalCount | $percent%\n")
                continue
            }

            0 -> break
            else -> println("Введите 1, 2 или 0")
        }
    }
}