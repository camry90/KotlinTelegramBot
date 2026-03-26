package additional

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

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
    val text: String? = null,
    @SerialName("chat")
    val chat: Chat? = null,
    @SerialName("document")
    val document: Document? = null,
)

@Serializable
data class Response(
    val result: List<Update> = emptyList(),
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

@Serializable
data class Document(
    @SerialName("file_name")
    val fileName: String,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long,
)

@Serializable
data class GetFileRequest(
    @SerialName("file_id")
    val fileId: String,
)

@Serializable
data class GetFileResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("result")
    val result: TelegramFile? = null,
)

@Serializable
data class SendPhotoResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName("result")
    val result: SendPhotoResult? = null,
)

@Serializable
data class SendPhotoResult(
    @SerialName("photo")
    val photo: List<PhotoSize>
)

@Serializable
data class PhotoSize(
    @SerialName("file_id")
    val fileId: String,
)

@Serializable
data class TelegramFile(
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long,
    @SerialName("file_path")
    val filePath: String,
)

@Serializable
data class SendPhotoRequest(
    @SerialName("chat_id")
    val chatId: Long?,
    @SerialName("photo")
    val photo: String?,
    @SerialName("has_spoiler")
    val hasSpoiler: Boolean = false,
)

fun main(args: Array<String>) {

    val json = Json {
        ignoreUnknownKeys = true
    }
    val botService = TelegramBotService(args[0])
    var lastUpdateId = 0L
    val trainers = HashMap<Long, LearnWordsTrainer>()
    val questions = HashMap<Long, Question?>()

    while (true) {
        Thread.sleep(1000)
        val responseString: String = botService.getUpdates(lastUpdateId)
        println(responseString)

        val response: Response = json.decodeFromString<Response>(responseString)
        if (response.result.isEmpty()) continue
        val sortedUpdates = response.result.sortedBy { it.updateId }
        sortedUpdates.forEach { handleUpdate(it, json, botService, trainers, questions) }
        lastUpdateId = sortedUpdates.last().updateId + 1

    }
}

fun handleUpdate(
    update: Update,
    json: Json,
    botService: TelegramBotService,
    trainers: HashMap<Long, LearnWordsTrainer>,
    questions: HashMap<Long, Question?>,
) {

    val message = update.message?.text
    val chatId = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id ?: return
    val data = update.callbackQuery?.data
    val currentQuestion: Question? = questions[chatId]
    val trainer = trainers.getOrPut(chatId) { LearnWordsTrainer("$chatId.txt") }
    val messageDocument = update.message?.document

    if( messageDocument != null ) {
        val jsonResponse = botService.getFile( messageDocument.fileId, json )
        println(jsonResponse)
        val response: GetFileResponse = json.decodeFromString(jsonResponse)
        response.result?.let {
            botService.downloadFile(it.filePath, it.fileUniqueId)
            trainer.readFile(it.fileUniqueId)
        }
    }

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
            questions[chatId] = botService.checkNextQuestionAndSend(trainer, botService, chatId, json)
        }

        data == CALLBACK_DATA_RESET -> {
            trainer.resetProgress()
            botService.sendMessage(json, chatId, "Прогресс сброшен")
            botService.sendMenu(json, chatId)
        }

        data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true -> {
            val index = data.substringAfterLast(CALLBACK_DATA_ANSWER_PREFIX).toIntOrNull()
            when (trainer.checkAnswer(index)) {
                FlagAnswer.RIGHT_ANSWER -> {
                    botService.sendMessage(json, chatId, "Правильно!")
                    questions[chatId] = botService.checkNextQuestionAndSend(trainer, botService, chatId, json)
                }

                FlagAnswer.WRONG_ANSWER -> {
                    botService.sendMessage(
                        json,
                        chatId,
                        "Неправильно! ${currentQuestion?.correctWord?.original} - это ${currentQuestion?.correctAnswer}"
                    )
                    questions[chatId] = botService.checkNextQuestionAndSend(trainer, botService, chatId, json)
                }

                FlagAnswer.MENU -> botService.sendMenu(json, chatId)

            }
        }
    }
}