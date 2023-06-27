package bitwheeze.golos.exchangebot.services;

import bitwheeze.golos.exchangebot.config.TelegramProperties;
import bitwheeze.golos.exchangebot.events.EbotEvent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
@Slf4j
public class TelegramService extends TelegramLongPollingBot {

    private final TelegramProperties props;

    @SneakyThrows
    public TelegramService(TelegramProperties props, TelegramBotsApi botsApi) {
        super(new DefaultBotOptions(), props.getBotToken());
        this.props = props;
        botsApi.registerBot(this);
    }

    @EventListener
    public void eventListener(EbotEvent event) {
        log.info("Got an event! {}", event);
        sendMessage(TelegramMessages.translate(event));
    }

    @SneakyThrows
    public void sendMessage(String message) {

        var sendMessage = new SendMessage();
        sendMessage.setChatId(props.getAdminId());
        sendMessage.setText(message);
        sendMessage.enableMarkdown(true);

        sendApiMethod(sendMessage);
    }

    @EventListener
    public void listener(EbotEvent event) {
        log.info("event {}", event);
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.info("Telegram Event receved {}", update);
    }

    @Override
    public String getBotUsername() {
        return props.getBotName();
    }
}
