package additional

class DynamicMessage(
    private val data: MutableMap<Long, Long> = mutableMapOf(),
) {

    fun saveMessage(chatId: Long, messageId: Long) {
        data[chatId] = messageId
    }

    fun getMessage(chatId: Long): Long? {
        return data[chatId]
    }

}