package additional

import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

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
        val urlSendUpdates = "$BOT_FILE_URL$botToken/sendMessage?"

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
                        InlineKeyboard(callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$CALLBACK_DATA_MENU", text = "Меню")
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

    fun checkNextQuestionAndSend(
        trainer: LearnWordsTrainer,
        telegramBotService: TelegramBotService,
        chatId: Long?,
        json: Json,
    ): Question? {
        val question = trainer.getNextQuestion()

        if (question == null) {
            telegramBotService.sendMessage(json, chatId, "Все слова выучены")
        } else {
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
        val body: InputStream = response.body()
        body.copyTo(File(fileName).outputStream(), 16 * 1024)
    }
}