package additional

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

data class Word(
    val original: String,
    val translate: String,
    var correctAnswerCount: Int = 0,
)

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

class LearnWordsTrainer(
    private val fileName: String = "word.txt",
    val learnedAnswerCount: Int = 3,
    val questionOfWords: Int = 4
) {

    private var question: Question? = null
    private val dictionary = loadDictionary()

    fun getStatistics(): Statistics {
        val learnedCount = dictionary.filter { it.correctAnswerCount >= learnedAnswerCount }.size
        val totalCount = dictionary.count()
        val percent = if (totalCount > 0) learnedCount * 100 / totalCount else 0
        return Statistics(learnedCount, totalCount, percent)
    }

    fun getNextQuestion(): Question? {
        val notLearnedList = dictionary.filter { it.correctAnswerCount < learnedAnswerCount }
        if (notLearnedList.isEmpty()) return null

        val correctAnswer = notLearnedList.map { it.translate }
            .random()
        val correctWord = notLearnedList.find { it.translate == correctAnswer }

        val questionWords = dictionary.map { it.translate }
            .distinct()
            .filter { it != correctAnswer }
            .shuffled()
            .take(questionOfWords - 1)

        val options = (questionWords + correctAnswer).shuffled()
        question = Question(variants = options, correctWord, correctAnswer)
        return question
    }

    fun checkAnswer(userAnswerInput: Int?): FlagAnswer {
        val correctAnswerId = question?.variants?.indexOf(question?.correctAnswer)

        return when (userAnswerInput) {
            correctAnswerId?.plus(1) -> {
                question?.correctWord?.let { word ->
                    val updatedWord = word.copy(correctAnswerCount = word.correctAnswerCount + 1)
                    dictionary[dictionary.indexOf(word)] = updatedWord
                }
                saveDictionary()
                FlagAnswer.RIGHT_ANSWER
            }

            0 -> FlagAnswer.MENU
            else -> FlagAnswer.WRONG_ANSWER

        }
    }

    private fun loadDictionary(): MutableList<Word> {
        val wordsFile = File(fileName)
        if (!wordsFile.exists()) {
            File("words.txt").copyTo(wordsFile)
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

    private fun saveDictionary() {
        val string = dictionary.joinToString(separator = "\n") { it ->
            "${it.original}|${it.translate}|${it.correctAnswerCount}"
        }
        File(fileName).writeText(string)
    }

    fun resetProgress() {
        dictionary.forEach { it.correctAnswerCount = 0 }
        saveDictionary()
    }
}