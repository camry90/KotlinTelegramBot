package additional

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LearnWordsTrainerTest {

    @Test
    fun `test statistics with 4 words of 7`() {
        val trainer = LearnWordsTrainer("src/test/4_words_of_7.txt")
        kotlin.test.assertEquals(
            Statistics(learnedCount = 4, totalCount = 7, percent = 57),
            trainer.getStatistics()
        )
    }

    @Test
    fun `test statistics with corrupted file`() {
        val trainer = LearnWordsTrainer("src/test/corrupted_file.txt")
        kotlin.test.assertEquals(
            Statistics(learnedCount = 0, totalCount = 3, percent = 0),
            trainer.getStatistics()
        )
    }

    @Test
    fun `test getNextQuestion() with 5 unlearned words`() {
        val trainer = LearnWordsTrainer("src/test/5_unlearned_words.txt")
        val question = trainer.getNextQuestion()
        assertTrue(question?.variants?.size == 4)
        assertTrue(question?.variants?.contains(question.correctAnswer) == true)
    }

    @Test
    fun `test getNextQuestion() with 1 unlearned words`() {
        val trainer = LearnWordsTrainer("src/test/1_unlearned_word.txt")
        val question = trainer.getNextQuestion()
        assertTrue(question?.variants?.size == 4)
        assertTrue(question?.variants?.contains(question.correctAnswer) == true)
    }

    @Test
    fun `test getNextQuestion() with all words learned`() {
        val trainer = LearnWordsTrainer("src/test/all_words_learned.txt")
        val question = trainer.getNextQuestion()
        assertNull(question)
    }

    @Test
    fun `test checkAnswer() with true`() {
        val trainer = LearnWordsTrainer("src/test/check_answer.txt")
        val nextQuestions = trainer.getNextQuestion()
        val flagTrue = FlagAnswer.RIGHT_ANSWER
        val correctAnswerId = nextQuestions?.variants?.indexOf(nextQuestions.correctAnswer)
        val question = trainer.checkAnswer(userAnswerInput = correctAnswerId?.plus(1) )
        assertTrue(question == flagTrue)
    }

    @Test
    fun `test checkAnswer() with false`() {
        val trainer = LearnWordsTrainer("src/test/check_answer.txt")
        val nextQuestions = trainer.getNextQuestion()
        val flagFalse = FlagAnswer.WRONG_ANSWER
        val wrongAnswerId = nextQuestions?.variants?.indexOf(nextQuestions.correctAnswer)
        val question = trainer.checkAnswer(userAnswerInput = wrongAnswerId)
        assertTrue(question == flagFalse)
    }

    @Test
    fun `test resetProgress() with 2 words in dictionary`() {
        val trainer = LearnWordsTrainer("src/test/reset_progress.txt")
        trainer.resetProgress()
        kotlin.test.assertEquals(
            Statistics(learnedCount = 0, totalCount = 2, percent = 0),
            trainer.getStatistics()
        )
    }
}