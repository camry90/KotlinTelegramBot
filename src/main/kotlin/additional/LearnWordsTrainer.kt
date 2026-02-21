package additional

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

enum class FlagAnswer {
    RIGHT_ANSWER,
    MENU,
    WRONG_ANSWER,
}

data class Statistics(
    val learnedCount: Int,
    val totalCount: Int,
    val percent: Int,
)

data class Question(
    val variants: List<String>,
    val correctWord: Word?,
    val correctAnswer: String,
)

class LearnWordsTrainer {

    private var question: Question? = null
    val dictionary = loadDictionary()

    fun getStatistics(): Statistics {
        val learnedCount = dictionary.filter { it.correctAnswerCount >= LEARNED_NUMBER }.size
        val totalCount = dictionary.count()
        val percent = if (totalCount > 0) learnedCount * 100 / totalCount else 0
        return Statistics(learnedCount, totalCount, percent)
    }

    fun getNextQuestion(): Question? {
        val notLearnedList = dictionary.filter { it.correctAnswerCount < LEARNED_NUMBER }
        if (notLearnedList.isEmpty()) return null

        val correctAnswer = notLearnedList.map { it.translate }
            .random()
        val correctWord = notLearnedList.find { it.translate == correctAnswer }

        val questionWords = dictionary.map { it.translate }
            .distinct()
            .filter { it != correctAnswer }
            .shuffled()
            .take(OPTIONS_NUMBER - 1)

        val options = (questionWords + correctAnswer).shuffled()
        question = Question(variants = options, correctWord, correctAnswer)
        return question
    }

    fun checkAnswer(userAnswerInput: Int?): FlagAnswer {
        val correctAnswerId = question?.variants?.indexOf(question?.correctAnswer)

        return when (userAnswerInput) {
            correctAnswerId?.plus(1) -> {
                val indexWord = dictionary.indexOf(question?.correctWord)
                val updatedWord =
                    question?.correctWord?.let { word ->
                        val updatedWord = word.copy(correctAnswerCount = word.correctAnswerCount + 1)
                        dictionary[dictionary.indexOf(word)] = updatedWord
                    }
                saveDictionary(dictionary)
                FlagAnswer.RIGHT_ANSWER
            }

            0 -> FlagAnswer.MENU
            else -> FlagAnswer.WRONG_ANSWER

        }
    }

    private fun loadDictionary(): MutableList<Word> {

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

    private fun saveDictionary(dictionary: MutableList<Word>) {
        val string = dictionary.joinToString(separator = "\n") { it ->
            "${it.original}|${it.translate}|${it.correctAnswerCount}"
        }
        File("words.txt").writeText(string)
    }
}