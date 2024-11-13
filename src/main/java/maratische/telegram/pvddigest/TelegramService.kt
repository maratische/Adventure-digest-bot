package maratische.telegram.pvddigest


import maratische.telegram.pvddigest.model.User
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class TelegramService(
    userService: UserService,
    postService: PostService
) {
    var telegramClient: TelegramClient = TelegramClient()

    init {
        telegramClient.setSecret(SettingsUtil.loadTelegramKey())
        telegramClient.setProcessGetUpdatesItem(object : ItemProcess {
            override fun process(item: GetUpdatesItem) {
                val user = userService.getOtCreateUser(item.message?.from);
                if (user != null && item.message?.chat != null) {
                    if (item.message?.chat?.id == SettingsUtil.sourceChatId().toLong()) {
                        //работаем с нашей группой
                        postService.processMessage(item.message ?: item.edited_message ?: return, user)
                    }
                    if (item.message?.chat?.type == "private") {
                        //персональный чат
                        if (user.chatId == null) {
                            user.chatId = item.message?.chat?.id
                            userService.save(user)
                        }
                        processPrivate(item.message!!, user)
                    }
                }
                SettingsUtil.saveOffset(item.update_id)
                SettingsUtil.save()
            }
        })
    }
//                        if ((item.message?.chat?.title ?: null) == "pvd_test_chat") {
//                forwardMessage("pvd_test_channel", "pvd_test_chat", item.message?.message_id ?: 0L)
//                            telegramClient.forwardMessage(SettingsUtil.sourceChatId(), SettingsUtil.destinationChannelId(), item.message?.message_id ?: 0L)
//                        }

    fun processPrivate(messageIn: maratische.telegram.pvddigest.Message, user: User) {
        if (messageIn.text?.lowercase() == "help") {
            telegramClient.sendMessage(user.chatId.toString(), "Привет. Я бот дайджеста")
        }
    }


    @Scheduled(fixedDelay = 5000)
    fun scheduler() {
        telegramClient.getAllTelegramUpdates()
    }


}
