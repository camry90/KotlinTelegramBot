package additional

import java.io.File
import java.io.FileNotFoundException

const val FILE_NAME = "words.txt"

data class Word(
    val original: String,
    val translate: String,
    var correctAnswerCount: Int = 0,
    val imageHint: String? = null,
    var fileId: String? = null,
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
    val dictionary: IUserDictionary,
    val learnedAnswerCount: Int = 3,
    val questionOfWords: Int = 4
) {

    private var question: Question? = null

    fun getStatistics(): Statistics {
        val learnedCount = dictionary.getNumOfLearnedWords()
        val totalCount = dictionary.getSize()
        val percent = if (totalCount > 0) learnedCount * 100 / totalCount else 0
        return Statistics(learnedCount, totalCount, percent)
    }

    fun getNextQuestion(): Question? {
        val notLearnedList = dictionary.getUnlearnedWords()
        if (notLearnedList.isEmpty()) return null

        val correctAnswer = notLearnedList.map { it.translate }
            .random()
        val correctWord = notLearnedList.find { it.translate == correctAnswer }

        val questionWords = dictionary.getUnlearnedWords().map { it.translate }
            .plus(dictionary.getLearnedWords().map { it.translate })
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
                    val current = dictionary.getCorrectAnswersCount(word.original)
                    dictionary.setCorrectAnswersCount(word.original, current + 1)
                }
                FlagAnswer.RIGHT_ANSWER
            }

            0 -> FlagAnswer.MENU
            else -> FlagAnswer.WRONG_ANSWER

        }
    }

    fun resetProgress() {
        dictionary.resetUserProgress()
    }

}