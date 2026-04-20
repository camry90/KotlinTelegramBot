package additional

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.sql.Types

fun main() {
    updateDictionary(File(FILE_NAME))
}

fun updateDictionary(wordsFile: File) {

    DriverManager.getConnection("jdbc:sqlite:data.db").use { connection ->
        connection.createStatement().use { statement ->
            statement.executeUpdate("""CREATE TABLE IF NOT EXISTS "words" (
                 "id" integer PRIMARY KEY AUTOINCREMENT,
                          "text" varchar UNIQUE,
                          "translate" varchar,
                          "image" varchar,
                          "file_id" varchar
                )""".trimIndent())
            statement.executeUpdate("""CREATE TABLE IF NOT EXISTS "users" (
                "id" integer PRIMARY KEY AUTOINCREMENT,
                    "username" varchar,
                    "created_at" timestamp,
                    "chat_id" varchar UNIQUE
                )""".trimIndent())
            statement.executeUpdate("""CREATE TABLE IF NOT EXISTS "user_answers" (
                 "user_id" integer,
                    "word_id" integer,
                    "correct_answer_count" integer,
                    "updated_at" timestamp,
                    PRIMARY KEY (user_id, word_id),
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY(word_id) REFERENCES words(id)
                )""".trimIndent())
        }

        for (line in wordsFile.readLines()) {
            val parts = line.split("|")
            if (parts.size < 2) continue
            val originalWord = parts[0]
            val translatedWord = parts[1]
            val image = parts.getOrNull(3)?.ifEmpty { null }
            val fileId = parts.getOrNull(4)?.ifEmpty { null }

            connection.prepareStatement(
                "INSERT OR IGNORE INTO words(text, translate, image, file_id) VALUES (?, ?, ?, ?)"
            ).use { stmt ->
                stmt.setString(1, originalWord)
                stmt.setString(2, translatedWord)
                if (image != null) stmt.setString(3, image) else stmt.setNull(3, Types.VARCHAR)
                if (fileId != null) stmt.setString(4, fileId) else stmt.setNull(4, Types.VARCHAR)
                stmt.executeUpdate()
            }
        }
    }
}

