package additional

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {

    val botService = TelegramBotService(args[0])

    var updateId = 0
    var chatId: Long = 0
    val messageUpdateIdRegex: Regex = "\"update_id\":(\\d+)".toRegex()
    val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
    val messageChatIdRegex: Regex = "\"chat\":\\{\"id\":(\\d+)".toRegex()
    val dataRegex: Regex = "\"data\":\"(.+?)\"".toRegex()

    val trainer = LearnWordsTrainer(3, 4)

    while (true) {
        Thread.sleep(2000)
        val updates: String = botService.getUpdates(updateId)
        println(updates)

        val matchResultUpdateId: MatchResult? = messageUpdateIdRegex.find(updates)
        val groupsUpdateId = matchResultUpdateId?.groups
        groupsUpdateId?.get(1)?.value?.let { updateId = (it).toInt() + 1 }
        println(updateId)

        val matchResultText: MatchResult? = messageTextRegex.find(updates)
        val groupsText = matchResultText?.groups
        val text = groupsText?.get(1)?.value
        println(text)

        val matchResultChatId: MatchResult? = messageChatIdRegex.find(updates)
        val groupsChatId = matchResultChatId?.groups
        groupsChatId?.get(1)?.value?.let { chatId = (it).toLong() }

        val dataMessage = dataRegex.find(updates)

        if (text.equals("Hello", ignoreCase = true)) {
            botService.sendMessage(chatId, text)
        }

        if (text.equals("Menu", ignoreCase = true)) {
            botService.sendMenu(chatId)
        }
    }
}
