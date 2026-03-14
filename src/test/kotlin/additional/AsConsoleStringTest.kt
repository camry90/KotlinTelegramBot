package additional

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AsConsoleStringTest {

    @Test
    fun `test strings consecutive words`() {
        val questions = Question(
            variants = listOf("лес", "дерево", "шахматы", "сыр"),
            correctWord = Word(
                "forest",
                "лес",
                0,
            ),
            correctAnswer = "лес"
        ).asConsoleString()
        kotlin.test.assertEquals(
            "forest\n" +
                    "1 - лес\n" +
                    "2 - дерево\n" +
                    "3 - шахматы\n" +
                    "4 - сыр\n" +
                    "--------------\n" +
                    "0 - Меню",
            questions
        )
    }

    @Test
    fun `test strings consecutive words without correctWord`() {
        val questions = Question(
            variants = listOf("лес", "дерево", "шахматы", "сыр"),
            correctWord = null,
            correctAnswer = "лес"
        ).asConsoleString()
        kotlin.test.assertEquals(
            "null\n" +
                    "1 - лес\n" +
                    "2 - дерево\n" +
                    "3 - шахматы\n" +
                    "4 - сыр\n" +
                    "--------------\n" +
                    "0 - Меню",
            questions
        )
    }

    @Test
    fun `test strings consecutive words with spaces variants`() {
        val questions = Question(
            variants = listOf("     ", "     ", "     ", "     "),
            correctWord = null,
            correctAnswer = "лес"
        ).asConsoleString()
        kotlin.test.assertEquals(
            "null\n" +
                    "1 -      \n" +
                    "2 -      \n" +
                    "3 -      \n" +
                    "4 -      \n" +
                    "--------------\n" +
                    "0 - Меню",
            questions
        )
    }

    @Test
    fun `test strings consecutive words with 10 words`() {
        val questions = Question(
            variants = listOf(
                "лес",
                "дерево",
                "шахматы",
                "сыр",
                "кот",
                "собака",
                "пиво",
                "врач",
                "полотенце",
                "машина"
            ),
            correctWord = Word(
                "forest",
                "лес",
                0,
            ),
            correctAnswer = "лес"
        ).asConsoleString()
        kotlin.test.assertEquals(
            "forest\n" +
                    "1 - лес\n" +
                    "2 - дерево\n" +
                    "3 - шахматы\n" +
                    "4 - сыр\n" +
                    "5 - кот\n" +
                    "6 - собака\n" +
                    "7 - пиво\n" +
                    "8 - врач\n" +
                    "9 - полотенце\n" +
                    "10 - машина\n" +
                    "--------------\n" +
                    "0 - Меню",
            questions
        )
    }

    @Test
    fun `test with empty variants list`() {
        val questions = Question(
            variants = listOf(),
            correctWord = Word(
                "forest",
                "лес",
                0,
            ),
            correctAnswer = "лес"
        ).asConsoleString()
        kotlin.test.assertEquals(
            "forest\n" +
                    "\n" +
                    "--------------\n" +
                    "0 - Меню",
            questions
        )
    }

    @Test
    fun `test strings with special symbols in variants`() {
        val questions = Question(
            variants = listOf("ле!с", "дере@@@во", "шахмат(00000)ы", "сCcCcыр"),
            correctWord = Word(
                "forest",
                "лес",
                0,
            ),
            correctAnswer = "лес"
        ).asConsoleString()
        kotlin.test.assertEquals(
            "forest\n" +
                    "1 - ле!с\n" +
                    "2 - дере@@@во\n" +
                    "3 - шахмат(00000)ы\n" +
                    "4 - сCcCcыр\n" +
                    "--------------\n" +
                    "0 - Меню",
            questions
        )
    }

    @Test
    fun `test correct answer not included in variants`() {
        val questions = Question(
            variants = listOf("лес", "дерево", "шахматы", "сыр"),
            correctWord = Word(
                "forest",
                "лес",
                0,
            ),
            correctAnswer = "удочка"
        ).asConsoleString()
        kotlin.test.assertEquals(
            "forest\n" +
                    "1 - лес\n" +
                    "2 - дерево\n" +
                    "3 - шахматы\n" +
                    "4 - сыр\n" +
                    "--------------\n" +
                    "0 - Меню",
            questions
        )
    }
}