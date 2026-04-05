package additional

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

fun main() {
    updateDictionary(File(FILE_NAME))
}

fun updateDictionary(wordsFile: File) {

    DriverManager.getConnection("jdbc:sqlite:data.db")
        .use { connection ->
            val statement = connection.createStatement()
            statement.executeUpdate(
                """
                      CREATE TABLE IF NOT EXISTS "words" (
                          "id" integer PRIMARY KEY AUTOINCREMENT,
                          "text" varchar UNIQUE,
                          "translate" varchar,
                          "image" varchar,
                          "file_id" varchar
                      );
              """.trimIndent()
            )
            statement.executeUpdate(
                """
                    CREATE TABLE IF NOT EXISTS "users" (
                    "id" integer PRIMARY KEY AUTOINCREMENT,
                    "username" varchar,
                    "created_at" timestamp,
                    "chat_id" varchar
                    );
                """.trimIndent()
            )
            statement.executeUpdate(
                """
                    CREATE TABLE IF NOT EXISTS "user_answers" (
                    "user_id" integer,
                    "word_id" integer,
                    "correct_answer_count" integer,
                    "updated_at" timestamp,
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY(word_id) REFERENCES words(id)
                    );
                """.trimIndent()
            )

            for (line in wordsFile.readLines()) {
                val parts = line.split("|")
                if (parts.size < 2) continue
                val originalWord = parts[0]
                val translatedWord = parts[1]
                val image = parts.getOrNull(3)?.ifEmpty { null }
                val imageValue = if (image != null) "'$image'" else "NULL"
                val fileIdCheck = parts.getOrNull(4)?.ifEmpty { null }
                val fileIdCheckValue = if (fileIdCheck != null) "'$fileIdCheck'" else "NULL"
                statement.executeUpdate("insert or ignore into words(text, translate, image, file_id) values('$originalWord', '$translatedWord', $imageValue, $fileIdCheckValue)")
            }
        }
}

