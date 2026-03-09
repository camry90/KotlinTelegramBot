package additional

fun main(args: Array<String>) {

    val botService = TelegramBotService(args[0])

    var updateId = 0
    var chatId: Long = 0
    val messageUpdateIdRegex: Regex = "\"update_id\":(\\d+)".toRegex()
    val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
    val messageChatIdRegex: Regex = "\"chat\":\\{\"id\":(\\d+)".toRegex()
    val dataRegex: Regex = "\"data\":\"(.+?)\"".toRegex()

    val trainer = LearnWordsTrainer(3, 4)
    var currentQuestion: Question? = null

    while (true) {
        Thread.sleep(2000)
        val updates: String = botService.getUpdates(updateId)
        println(updates)

        val matchResultUpdateId: MatchResult? = messageUpdateIdRegex.find(updates)
        val groupsUpdateId = matchResultUpdateId?.groups
        groupsUpdateId?.get(1)?.value?.let { updateId = (it).toInt() + 1 }

        val matchResultText: MatchResult? = messageTextRegex.find(updates)
        val groupsText = matchResultText?.groups
        val text = groupsText?.get(1)?.value

        val matchResultChatId: MatchResult? = messageChatIdRegex.find(updates)
        val groupsChatId = matchResultChatId?.groups
        groupsChatId?.get(1)?.value?.let { chatId = (it).toLong() }

        val matchResultData: MatchResult? = dataRegex.find(updates)
        val groupsData = matchResultData?.groups
        val dataMessage = groupsData?.get(1)?.value

        when {
            text == GREETING_STRING -> {
                botService.sendMessage(chatId, text)
            }

            text == COMMAND_START -> {
                botService.sendMenu(chatId)
            }

            dataMessage == CALLBACK_DATA_STATISTICS -> {
                val statistics = trainer.getStatistics()
                botService.sendMessage(
                    chatId,
                    "Выучено ${statistics.learnedCount} из ${statistics.totalCount} | ${statistics.percent}%\n"
                )
            }

            dataMessage == CALLBACK_DATA_LEARN_WORDS -> {
                currentQuestion = botService.checkNextQuestionAndSend(trainer, botService, chatId)
            }

            dataMessage?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true -> {
                val index = dataMessage.substringAfterLast(CALLBACK_DATA_ANSWER_PREFIX).toIntOrNull()
                when (trainer.checkAnswer(index)) {
                    FlagAnswer.RIGHT_ANSWER -> {
                        botService.sendMessage(chatId, "Правильно!")
                        currentQuestion = botService.checkNextQuestionAndSend(trainer, botService, chatId)
                    }

                    FlagAnswer.WRONG_ANSWER -> {
                        botService.sendMessage(
                            chatId,
                            "Неправильно! ${currentQuestion?.correctWord?.original} - это ${currentQuestion?.correctAnswer}"
                        )
                        currentQuestion = botService.checkNextQuestionAndSend(trainer, botService, chatId)
                    }

                    FlagAnswer.MENU -> botService.sendMenu(chatId)
                }
            }
        }

    }
}
