package additional

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Update(
    @SerialName("update_id")
    val updateId: Long,
    @SerialName("message")
    val message: Message? = null,
    @SerialName("callback_query")
    val callbackQuery: CallbackQuery? = null,
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String,
    @SerialName("chat")
    val chat: Chat? = null,
)

@Serializable
data class Response(
    val result: List<Update>
)

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String? = null,
    @SerialName("message")
    val message: Message? = null,
)

@Serializable
data class Chat(
    @SerialName("id")
    val id: Long,
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id")
    val chatId: Long?,
    @SerialName("text")
    val text: String?,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup? = null,
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboard>>,
)

@Serializable
data class InlineKeyboard(
    @SerialName("callback_data")
    val callbackData: String,
    @SerialName("text")
    val text: String,
)

fun main(args: Array<String>) {

    val json = Json {
        ignoreUnknownKeys = true
    }
    val botService = TelegramBotService(args[0])
    var lastUpdateId = 0L
    val trainer = LearnWordsTrainer(3, 4)
    var currentQuestion: Question? = null

    while (true) {
        Thread.sleep(1000)
        val responseString: String = botService.getUpdates(lastUpdateId)
        println(responseString)

        val response: Response = json.decodeFromString<Response>(responseString)
        val updates = response.result
        val firstUpdate = updates.firstOrNull() ?: continue
        val updateId = firstUpdate.updateId
        lastUpdateId = updateId + 1

        val message = firstUpdate.message?.text
        val chatId = firstUpdate.message?.chat?.id ?: firstUpdate.callbackQuery?.message?.chat?.id
        val data = firstUpdate.callbackQuery?.data


        when {
            message == GREETING_STRING -> {
                botService.sendMessage(json, chatId, message)
            }

            message == COMMAND_START -> {
                botService.sendMenu(json, chatId)
            }

            data == CALLBACK_DATA_STATISTICS -> {
                val statistics = trainer.getStatistics()
                botService.sendMessage(
                    json,
                    chatId,
                    "Выучено ${statistics.learnedCount} из ${statistics.totalCount} | ${statistics.percent}%\n"
                )
            }

            data == CALLBACK_DATA_LEARN_WORDS -> {
                currentQuestion = botService.checkNextQuestionAndSend(trainer, botService, chatId, json)
            }

            data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true -> {
                val index = data.substringAfterLast(CALLBACK_DATA_ANSWER_PREFIX).toIntOrNull()
                when (trainer.checkAnswer(index)) {
                    FlagAnswer.RIGHT_ANSWER -> {
                        botService.sendMessage(json, chatId, "Правильно!")
                        currentQuestion = botService.checkNextQuestionAndSend(trainer, botService, chatId, json)
                    }

                    FlagAnswer.WRONG_ANSWER -> {
                        botService.sendMessage(
                            json,
                            chatId,
                            "Неправильно! ${currentQuestion?.correctWord?.original} - это ${currentQuestion?.correctAnswer}"
                        )
                        currentQuestion = botService.checkNextQuestionAndSend(trainer, botService, chatId, json)
                    }

                    FlagAnswer.MENU -> botService.sendMenu(json, chatId)
                }
            }
        }
    }
}
