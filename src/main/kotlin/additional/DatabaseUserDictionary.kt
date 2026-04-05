package additional

import java.io.Closeable
import java.sql.DriverManager

class DatabaseUserDictionary(
    private val chatId: Long,
    private val dbFilePath: String = "data.db",
    private val learningThreshold: Int = 3,
) : IUserDictionary, Closeable {

    private val connection = DriverManager.getConnection("jdbc:sqlite:$dbFilePath")

    override fun close() {
        connection.close()
    }

    override fun getSize(): Int {
        val statement = connection.createStatement()
        val rs = statement.executeQuery("SELECT COUNT(*) FROM words")
        return rs.getInt(1)
    }

    override fun getNumOfLearnedWords(): Int {
        val statement = connection.createStatement()
        val rs =
            statement.executeQuery(
                "SELECT COUNT(*) FROM user_answers WHERE user_id = ${getUserId()} AND correct_answer_count >= $learningThreshold"
            )
        return rs.getInt(1)
    }

    override fun getLearnedWords(): List<Word> {
        val statement = connection.createStatement()
        val rs = statement.executeQuery("""
                    SELECT words.*, user_answers.correct_answer_count FROM words
                    INNER JOIN user_answers ON words.id = user_answers.word_id
                    WHERE user_answers.user_id = ${getUserId()}
                    AND user_answers.correct_answer_count >= $learningThreshold
                """.trimIndent()
        )

        val result = mutableListOf<Word>()
        while (rs.next()) {
            result.add(
                Word(
                    original = rs.getString("text"),
                    translate = rs.getString("translate"),
                    correctAnswerCount = rs.getInt("correct_answer_count"),
                    imageHint = rs.getString("image")
                )
            )
        }
        return result
    }

    override fun getUnlearnedWords(): List<Word> {
        val statement = connection.createStatement()
        val rs = statement.executeQuery("""
                    SELECT words.*, user_answers.correct_answer_count FROM words
                    LEFT JOIN user_answers ON words.id = user_answers.word_id
                    AND user_answers.user_id = ${getUserId()}
                    WHERE user_answers.correct_answer_count < $learningThreshold
                    OR user_answers.correct_answer_count IS NULL
                """.trimIndent()
        )
        val result = mutableListOf<Word>()
        while (rs.next()) {
            result.add(
                Word(
                    original = rs.getString("text"),
                    translate = rs.getString("translate"),
                    correctAnswerCount = rs.getInt("correct_answer_count"),
                    imageHint = rs.getString("image")
                )
            )
        }
        return result
    }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        val statement = connection.createStatement()
        val rs = statement.executeQuery("SELECT id FROM words WHERE text = '$word'")
        val wordId = rs.getInt("id")
        val userId = getUserId()
        statement.executeUpdate(
            """
    INSERT OR REPLACE INTO user_answers (user_id, word_id, correct_answer_count, updated_at)
    VALUES ($userId, $wordId, $correctAnswersCount, datetime('now'))
    """.trimIndent()
        )
    }

    override fun resetUserProgress() {
        val statement = connection.createStatement()
        val userId = getUserId()
        statement.executeUpdate("DELETE FROM user_answers WHERE user_id = $userId")
    }

    private fun getUserId(): Int {
        val statement = connection.createStatement()
        val rs = statement.executeQuery(
            "SELECT id FROM users WHERE chat_id = $chatId"
        )
        if (rs.next()) {
            return rs.getInt("id")
        }
        statement.executeUpdate(
            "INSERT INTO users(chat_id, created_at) VALUES($chatId, datetime('now'))"
        )
        val rsNew = statement.executeQuery("SELECT last_insert_rowid()")
        return rsNew.getInt(1)
    }

    override fun getCorrectAnswersCount(word: String): Int {
        val userId = getUserId()
        val statement = connection.createStatement()
        val rs = statement.executeQuery(
            """
                        SELECT user_answers.correct_answer_count FROM user_answers
                        INNER JOIN words ON words.id = user_answers.word_id
                        WHERE words.text = '$word'
                        AND user_answers.user_id = $userId
                 """.trimIndent()
        )
        return if (rs.next()) rs.getInt("correct_answer_count") else 0
    }

    override fun saveFileId(word: String, fileId: String) {
        val statement = connection.createStatement()
        val rs = statement.executeUpdate(
            """
                UPDATE words SET file_id = '$fileId' WHERE text = '$word'
            """.trimIndent()
        )
    }
}