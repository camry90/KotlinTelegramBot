package additional

import java.io.Closeable
import java.sql.Connection
import java.sql.DriverManager

class DatabaseUserDictionary(
    private val chatId: Long,
    private val dbFilePath: String = "data.db",
    private val learningThreshold: Int = 3,
    connection: Connection? = null,
) : IUserDictionary, Closeable {

    private val connection = connection ?: DriverManager.getConnection("jdbc:sqlite:$dbFilePath")
    private val logger = java.util.logging.Logger.getLogger(DatabaseUserDictionary::class.java.name)

    override fun close() {
        connection.close()
    }

    override fun getSize(): Int {
        return connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT COUNT(*) FROM words").use { rs ->
                rs.getInt(1)
            }
        }
    }

    override fun getNumOfLearnedWords(): Int {
        return connection.prepareStatement(
            "SELECT COUNT(*) FROM user_answers WHERE user_id = ? AND correct_answer_count >= ?"
        ).use { stmt ->
            stmt.setInt(1, getUserId())
            stmt.setInt(2, learningThreshold)
            stmt.executeQuery().use { rs ->
                rs.getInt(1)
            }
        }
    }

    override fun getLearnedWords(): List<Word> {
        return connection.prepareStatement(
            """
            SELECT words.*, user_answers.correct_answer_count FROM words
            INNER JOIN user_answers ON words.id = user_answers.word_id
            WHERE user_answers.user_id = ?
            AND user_answers.correct_answer_count >= ?
        """.trimIndent()
        ).use { stmt ->
            stmt.setInt(1, getUserId())
            stmt.setInt(2, learningThreshold)
            stmt.executeQuery().use { rs ->
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
                result
            }
        }
    }

    override fun getUnlearnedWords(): List<Word> {
        return connection.prepareStatement(
            """
            SELECT words.*, user_answers.correct_answer_count FROM words
            LEFT JOIN user_answers ON words.id = user_answers.word_id
            AND user_answers.user_id = ?
            WHERE user_answers.correct_answer_count < ?
            OR user_answers.correct_answer_count IS NULL
        """.trimIndent()
        ).use { stmt ->
            stmt.setInt(1, getUserId())
            stmt.setInt(2, learningThreshold)
            stmt.executeQuery().use { rs ->
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
                result
            }
        }
    }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        val validatedWord = validateWord(word)
        val wordId = connection.prepareStatement("SELECT id FROM words WHERE text = ?").use { stmt ->
            stmt.setString(1, validatedWord)
            stmt.executeQuery().use { rs ->
                rs.getInt("id")
            }
        }
        val userId = getUserId()
        connection.prepareStatement(
            """
    INSERT OR REPLACE INTO user_answers (user_id, word_id, correct_answer_count, updated_at)
    VALUES (?, ?, ?, datetime('now'))
""".trimIndent()
        ).use { stmt ->
            stmt.setInt(1, userId)
            stmt.setInt(2, wordId)
            stmt.setInt(3, correctAnswersCount)
            stmt.executeUpdate()
        }
    }

    override fun resetUserProgress() {
        val userId = getUserId()
        connection.prepareStatement("DELETE FROM user_answers WHERE user_id = ?").use { stmt ->
            stmt.setInt(1, userId)
            stmt.executeUpdate()
        }
    }

    private fun getUserId(): Int {
        val existingId = connection.prepareStatement(
            "SELECT id FROM users WHERE chat_id = ?"
        ).use { stmt ->
            stmt.setLong(1, chatId)
            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.getInt("id") else null
            }
        }

        if (existingId != null) return existingId

        connection.prepareStatement(
            "INSERT INTO users(chat_id, created_at) VALUES(?, datetime('now'))"
        ).use { stmt ->
            stmt.setLong(1, chatId)
            stmt.executeUpdate()
        }

        return connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT last_insert_rowid()").use { rs ->
                rs.getInt(1)
            }
        }
    }

    override fun getCorrectAnswersCount(word: String): Int {
        val userId = getUserId()
        val validatedWord = validateWord(word)
        return connection.prepareStatement("""
        SELECT user_answers.correct_answer_count FROM user_answers
        INNER JOIN words ON words.id = user_answers.word_id
        WHERE words.text = ?
        AND user_answers.user_id = ?
    """.trimIndent()).use { stmt ->
            stmt.setString(1, validatedWord)
            stmt.setInt(2,userId )
            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.getInt("correct_answer_count") else 0
            }
        }
    }

    override fun saveFileId(word: String, fileId: String) {
        val validatedWord = validateWord(word)
        connection.prepareStatement(
            """
            UPDATE words SET file_id = ? WHERE text = ?
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1,fileId )
            stmt.setString(2, validatedWord)
            stmt.executeUpdate()
        }
    }

    private fun validateWord(word: String): String {
        logIfSuspicious(word)
        val allowedPattern = Regex("^[a-zA-Zа-яА-Я0-9\\s\\-]+$")
        if (!allowedPattern.matches(word)) {
            throw IllegalArgumentException("Недопустимые символы")
        }
        if (word.length > 100) {
            throw IllegalArgumentException("Слишком длинное слово")
        }
        return word.trim()
    }

    private fun logIfSuspicious(input: String) {
        val suspiciousPatterns = listOf(
            "'", "\"", ";", "--", "/*", "*/",
            "union", "select", "drop", "delete", "insert", "update"
        )

        val containsSuspicious = suspiciousPatterns.any {
            input.lowercase().contains(it)
        }

        if (containsSuspicious) {
            logger.warning("Подозрительный ввод: $input, chatId: $chatId")
        }
    }
}