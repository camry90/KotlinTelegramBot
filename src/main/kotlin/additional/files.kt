package additional

data class Word(
    val original: String,
    val translate: String,
    val correctAnswerCount: Int = 0,
)

fun Question.asConsoleString(): String {

    val variants = this.variants
        .mapIndexed { index, variant -> "${index + 1} - $variant" }
        .joinToString("\n")
    return this.correctWord?.original + "\n" + variants + "\n" + "--------------\n" + "0 - Меню"
}

fun main() {

    val trainer = LearnWordsTrainer(3, 4)

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
                while (true) {

                    val question = trainer.getNextQuestion()

                    if (question == null) {
                        println("Все слова выучны")
                        break
                    }


                    println(question.asConsoleString())

                    val userAnswerInput = readlnOrNull()?.toIntOrNull()

                    when (trainer.checkAnswer(userAnswerInput)) {
                        FlagAnswer.RIGHT_ANSWER -> println("Правильно!")
                        FlagAnswer.MENU -> break
                        FlagAnswer.WRONG_ANSWER -> println("Неправильно! ${question.correctWord?.original} - это ${question.correctAnswer}")
                    }
                }
            }

            2 -> {
                val statistics = trainer.getStatistics()
                println("Выучено ${statistics.learnedCount} из ${statistics.totalCount} | ${statistics.percent}%\n")
            }

            0 -> break
            else -> println("Введите 1, 2 или 0")
        }
    }
}