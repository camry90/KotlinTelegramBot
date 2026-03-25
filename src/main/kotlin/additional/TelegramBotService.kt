package additional

import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Random

const val COMMAND_START = "/start"
const val GREETING_STRING = "Hello"
const val CALLBACK_DATA_MENU = "0"
const val CALLBACK_DATA_RESET = "reset_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
const val CALLBACK_DATA_LEARN_WORDS = "learn_words_clicked"
const val CALLBACK_DATA_STATISTICS = "statistics_clicked"
const val BASIC_URL = "https://api.telegram.org/bot"
const val BOT_FILE_URL = "https://api.telegram.org/file/bot"

class TelegramBotService(private val botToken: String) {
    
    private val client: HttpClient = HttpClient.newBuilder().build()

    fun getUpdates(updateId: Long): String {
        val urlGetUpdates = "$BASIC_URL$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    fun sendMessage(json: Json, chatId: Long?, text: String?) {
        val urlSendUpdates = "$BASIC_URL$botToken/sendMessage?"

        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = text,
        )

        val requestBodyString = json.encodeToString(requestBody)
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendUpdates))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

        response.body()
    }

    fun sendMenu(json: Json, chatId: Long?) {
        val urlSendUpdates = "$BASIC_URL$botToken/sendMessage?"
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = "Основное меню",
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyboard(
                            callbackData = CALLBACK_DATA_LEARN_WORDS, text = "Изучить слова"
                        ),
                        InlineKeyboard(callbackData = CALLBACK_DATA_STATISTICS, text = "Статистика")
                    ),
                    listOf(
                        InlineKeyboard(callbackData = CALLBACK_DATA_RESET, text = "Сбросить прогресс"),
                    )
                )
            )
        )

        val requestBodyString = json.encodeToString(requestBody)
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendUpdates))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

        response.body()
    }

    fun sendQuestion(json: Json, chatId: Long?, questions: Question?) {

        val urlSendUpdates = "$BASIC_URL$botToken/sendMessage?"

        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = questions?.correctWord?.original,
            replyMarkup = ReplyMarkup(
                listOf(
                    questions?.variants?.mapIndexed { index, word ->
                        InlineKeyboard(callbackData = "$CALLBACK_DATA_ANSWER_PREFIX${index + 1}", text = word)
                    } ?: listOf(), listOf(
                        InlineKeyboard(callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$CALLBACK_DATA_MENU", text = "Меню"),
                    ),
                )

            )
        )

        val requestBodyString = json.encodeToString(requestBody)
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendUpdates))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

        response.body()
    }

    fun checkNextQuestionAndSend(
        trainer: LearnWordsTrainer,
        telegramBotService: TelegramBotService,
        chatId: Long,
        json: Json,
    ): Question? {
        val question = trainer.getNextQuestion()

        if (question == null) {
            telegramBotService.sendMessage(json, chatId, "Все слова выучены")
        } else {
            val correctWord = question.correctWord
            if (correctWord?.imageHint != null) {
                if (correctWord.fileId != null) {
                    sendPhotoById(correctWord.fileId.toString(), chatId, hasSpoiler = true, json)
                } else {
                    val file = File("images/${correctWord.imageHint}")
                    val response = sendPhoto(file, chatId, hasSpoiler = true)
                    val sendPhotoResponse: SendPhotoResponse = json.decodeFromString(response)
                    val fileId = sendPhotoResponse.result?.photo?.last()?.fileId
                    correctWord.fileId = fileId
                    trainer.saveDictionary()
                }
            }
            telegramBotService.sendQuestion(json, chatId, question)
        }
        return question
    }

    fun getFile(fileId: String, json: Json): String {
        val urlGetFile = "$BASIC_URL$botToken/getFile"
        val requestBody = GetFileRequest(fileId = fileId)
        val requestBodyString = json.encodeToString(requestBody)
        val client: HttpClient = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(urlGetFile))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String> = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        )
        return response.body()
    }

    fun downloadFile(filePath: String, fileName: String) {
        val urlGetFile = "$BOT_FILE_URL$botToken/$filePath"
        println(urlGetFile)
        val request = HttpRequest
            .newBuilder()
            .uri(URI.create(urlGetFile))
            .GET()
            .build()

        val response: HttpResponse<InputStream> = HttpClient
            .newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofInputStream());

        println("status code: " + response.statusCode());
        response.body().use { body ->
            File(fileName).outputStream().use { output ->
                body.copyTo(output, 16 * 1024)
            }
        }
    }

    fun sendPhoto(file: File, chatId: Long, hasSpoiler: Boolean = false): String {
        val data: MutableMap<String, Any> = LinkedHashMap()
        data["chat_id"] = chatId.toString()
        data["photo"] = file
        data["has_spoiler"] = hasSpoiler
        val boundary: String = BigInteger(35, Random()).toString()

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$BASIC_URL$botToken/sendPhoto"))
            .postMultipartFormData(boundary, data)
            .build()
        val client: HttpClient = HttpClient.newBuilder().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendPhotoById(fileId: String, chatId: Long, hasSpoiler: Boolean = false, json: Json): String {

        val requestBody = SendPhotoRequest(
            chatId = chatId,
            photo = fileId,
            hasSpoiler = hasSpoiler,
        )
        val requestBodyString = json.encodeToString(requestBody)

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$BASIC_URL$botToken/sendPhoto"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val client: HttpClient = HttpClient.newBuilder().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }
}

private fun HttpRequest.Builder.postMultipartFormData(boundary: String, data: Map<String, Any>): HttpRequest.Builder {
    val byteArrays = ArrayList<ByteArray>()
    val separator = "--$boundary\r\nContent-Disposition: form-data; name=".toByteArray(StandardCharsets.UTF_8)

    for (entry in data.entries) {
        byteArrays.add(separator)
        when (entry.value) {
            is File -> {
                val file = entry.value as File
                val path = Path.of(file.toURI())
                val mimeType = Files.probeContentType(path)
                byteArrays.add(
                    "\"${entry.key}\"; filename=\"${path.fileName}\"\r\nContent-Type: $mimeType\r\n\r\n".toByteArray(
                        StandardCharsets.UTF_8
                    )
                )
                byteArrays.add(Files.readAllBytes(path))
                byteArrays.add("\r\n".toByteArray(StandardCharsets.UTF_8))
            }

            else -> byteArrays.add("\"${entry.key}\"\r\n\r\n${entry.value}\r\n".toByteArray(StandardCharsets.UTF_8))
        }
    }
    byteArrays.add("--$boundary--".toByteArray(StandardCharsets.UTF_8))

    this.header("Content-Type", "multipart/form-data;boundary=$boundary")
        .POST(HttpRequest.BodyPublishers.ofByteArrays(byteArrays))
    return this
}