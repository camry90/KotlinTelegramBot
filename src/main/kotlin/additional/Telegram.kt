package additional

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {

    val botToken = args[0]
    val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates"

    var updateId = 0

    while (true) {
        Thread.sleep(2000)
        val updates: String = getUpdates(botToken, updateId)
        println(updates)

        val messageUpdateIdRegex: Regex = "\"update_id\":(\\d+)".toRegex()
        val matchResult: MatchResult? = messageUpdateIdRegex.find(updates)
        val groups = matchResult?.groups
        groups?.get(1)?.value?.let { updateId = (it).toInt() + 1 }
        println(updateId)
    }
}

fun getUpdates(botToken: String, updateId: Int): String {
    val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates?offset=$updateId"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

    return response.body()
}