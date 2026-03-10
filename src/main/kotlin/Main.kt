import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.annotation.processing.Messager

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
)

@Serializable
data class Response(
    val result: List<Update>
)

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String? = null,
)

fun main() {

    val json = Json {
        ignoreUnknownKeys = true
    }

    val responseString = """
        {
          "ok": true,
          "result": [
            {
              "update_id": 253389766,
              "message": {
                "message_id": 174,
                "from": {
                  "id": 872824494,
                  "is_bot": false,
                  "first_name": "kmrn",
                  "username": "camry0990",
                  "language_code": "ru"
                },
                "chat": {
                  "id": 872824494,
                  "first_name": "kmrn",
                  "username": "camry0990",
                  "type": "private"
                },
                "date": 1773132746,
                "text": "/start",
                "entities": [
                  {
                    "offset": 0,
                    "length": 6,
                    "type": "bot_command"
                  }
                ]
              }
            }
          ]
        }
    """.trimIndent()

    val response = json.decodeFromString<Response>(responseString)
    println(response)

}