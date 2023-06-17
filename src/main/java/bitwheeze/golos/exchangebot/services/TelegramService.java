package bitwheeze.golos.exchangebot.services;

import bitwheeze.golos.exchangebot.events.EbotEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TelegramService {


    @EventListener
    public void eventListener(EbotEvent event) {
        log.info("Got an event! {}", event);
    }
}
