package additional

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {

    val botToken = args[0]
    val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates"

    var updateId = 0
    val messageUpdateIdRegex: Regex = "\"update_id\":(\\d+)".toRegex()
    val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()

    while (true) {
        Thread.sleep(2000)
        val updates: String = getUpdates(botToken, updateId)
        println(updates)

        val matchResultUpdateId: MatchResult? = messageUpdateIdRegex.find(updates)
        val groupsUpdateId = matchResultUpdateId?.groups
        groupsUpdateId?.get(1)?.value?.let { updateId = (it).toInt() + 1 }
        println(updateId)

        val matchResultText: MatchResult? = messageTextRegex.find(updates)
        val groupsText = matchResultText?.groups
        val text = groupsText?.get(1)?.value
        println(text)
    }
}

fun getUpdates(botToken: String, updateId: Int): String {
    val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates?offset=$updateId"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

    return response.body()
}