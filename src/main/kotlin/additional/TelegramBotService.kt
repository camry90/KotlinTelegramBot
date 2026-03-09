package additional

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val COMMAND_START = "/start"
const val GREETING_STRING = "Hello"
const val ANSWER_MENU = "0"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
const val CALLBACK_DATA_LEARN_WORDS = "learn_words_clicked"
const val CALLBACK_DATA_STATISTICS = "statistics_clicked"
const val BASIC_URL = "https://api.telegram.org/bot"

class TelegramBotService(private val botToken: String) {

    private val client: HttpClient = HttpClient.newBuilder().build()

    fun getUpdates(updateId: Int): String {
        val urlGetUpdates = "$BASIC_URL$botToken/getUpdates?offset=$updateId"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    fun sendMessage(chatId: Long, text: String?) {
        val encodedText = URLEncoder.encode(text, "UTF-8")
        val urlSendUpdates = "$BASIC_URL$botToken/sendMessage?chat_id=$chatId&text=$encodedText"
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendUpdates)).build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

        response.body()
    }

    fun sendMenu(chatId: Long) {

        val urlSendUpdates = "$BASIC_URL$botToken/sendMessage?"
        val sendMenuBody = """
            {
              "chat_id": $chatId,
              "text": "Основное меню",
              "reply_markup": {
                "inline_keyboard": [
                  [
                    {
                      "text": "Изучить слова",
                      "callback_data": "$CALLBACK_DATA_LEARN_WORDS"
                    },
                    {
                      "text": "Статистика",
                      "callback_data": "$CALLBACK_DATA_STATISTICS"
                    }
                  ]
                ]
              }
            }
        """.trimIndent()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendUpdates))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(sendMenuBody))
            .build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

        response.body()
    }

    fun sendQuestion(chatId: Long, questions: Question?) {

        val urlSendUpdates = "$BASIC_URL$botToken/sendMessage?"
        val buttons = questions?.variants
            ?.mapIndexed { index, variant ->
                """[{"text": "$variant", "callback_data": "${CALLBACK_DATA_ANSWER_PREFIX}${index + 1}"}]"""
            }?.joinToString(",")

        val sendQuestionBody = """
          {
            "chat_id": $chatId,
            "text": "Переведите ${questions?.correctWord?.original}",
            "reply_markup": {
              "inline_keyboard": [
                $buttons,
                [
                  {
                    "text": "Выход в меню",
                    "callback_data": "${CALLBACK_DATA_ANSWER_PREFIX + ANSWER_MENU}"
                  }
                ]
              ]
            }
          }
        """.trimIndent()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendUpdates))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(sendQuestionBody))
            .build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

        response.body()

    }

    fun checkNextQuestionAndSend(
        trainer: LearnWordsTrainer,
        telegramBotService: TelegramBotService,
        chatId: Long,
    ): Question? {
        val question = trainer.getNextQuestion()

        if (question == null) {
            telegramBotService.sendMessage(chatId, "Все слова выучены")
        } else {
            telegramBotService.sendQuestion(chatId, question)
        }
        return question
    }
}

