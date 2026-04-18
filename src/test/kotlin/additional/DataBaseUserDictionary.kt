package additional

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import additional.DatabaseUserDictionary
import org.junit.jupiter.api.BeforeEach
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.assertEquals

class DatabaseUserDictionaryTest {

    private lateinit var db: DatabaseUserDictionary
    private lateinit var connection: Connection

    @BeforeEach
    fun setUp() {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        connection.createStatement().use { stmt ->
            stmt.executeUpdate("""CREATE TABLE IF NOT EXISTS "words" (
                "id" integer PRIMARY KEY AUTOINCREMENT,
                "text" varchar UNIQUE,
                "translate" varchar,
                "image" varchar,
                "file_id" varchar
            )""")
            stmt.executeUpdate("""CREATE TABLE IF NOT EXISTS "users" (
                "id" integer PRIMARY KEY AUTOINCREMENT,
                "username" varchar,
                "created_at" timestamp,
                "chat_id" varchar UNIQUE
            )""")
            stmt.executeUpdate("""CREATE TABLE IF NOT EXISTS "user_answers" (
                "user_id" integer,
                "word_id" integer,
                "correct_answer_count" integer,
                "updated_at" timestamp,
                PRIMARY KEY (user_id, word_id),
                FOREIGN KEY (user_id) REFERENCES users(id),
                FOREIGN KEY (word_id) REFERENCES words(id)
            )""")
        }
        db = DatabaseUserDictionary(chatId = 12345L, connection = connection)
    }

    @Test
    fun `should throw on SQL injection in setCorrectAnswersCount`() {
        assertThrows<IllegalArgumentException> {
            db.setCorrectAnswersCount("'; DROP TABLE words; --", 1)
        }
    }

    @Test
    fun `should throw on SQL injection in getCorrectAnswersCount`() {
        assertThrows<IllegalArgumentException> {
            db.getCorrectAnswersCount("' OR '1'='1")
        }
    }

    @Test
    fun `should throw on SQL injection in saveFileId`() {
        assertThrows<IllegalArgumentException> {
            db.saveFileId("'; DROP TABLE words", "FileId")
        }
    }
}