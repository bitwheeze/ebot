package bitwheeze.golos.exchangebot.components;

import bitwheeze.golos.exchangebot.config.EbotProperties;
import bitwheeze.golos.exchangebot.config.TradingPair;
import bitwheeze.golos.exchangebot.events.info.ChangedPriceEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
@RequiredArgsConstructor
public class Ebot {

    private final EbotProperties ebotProps;
    private final RelativeOrdersStrategy relativeOrders;

    public void processTradingPairs() {
        if(ebotProps.getPairs() != null) {
            for (var pair : ebotProps.getPairs()) {
                processPair(pair);
            }
        }
    }

    private void processPair(TradingPair pair) {
        switch (validatePair(pair)) {
            case RelativeOrders:
                relativeOrders.proccessPair(pair);
                break;
        }
    }

    private Strategy validatePair(TradingPair pair) {
        if(!RelativeOrdersStrategy.validate(pair.getRelativeOrders())) {
            return Strategy.Empty;
        }
        return Strategy.RelativeOrders;
    }

    @Scheduled(cron = "#{@ebotProperties.cron}")
    public void cronJob() {
        log.info("ebot cron job started");
        processTradingPairs();
    }

    @PostConstruct
    public void init() {
        log.info("process pairs on start");
        processTradingPairs();
    }

    public Set<String> getConfiguredAssets() {
        if(null == ebotProps.getPairs()) return Collections.emptySet();
        return ebotProps.getPairs()
                .stream()
                .flatMap( pair -> Stream.of(pair.getQuote(), pair.getBase()))
                .collect(Collectors.toSet());
    }

    @EventListener
    public void onEvent(ChangedPriceEvent event) {
        if(ebotProps.getPairs() != null) {
            ebotProps.getPairs()
                .stream()
                .filter(pair -> event.getBase().equals(pair.getBase()) || event.getBase().equals(pair.getQuote()))
                .forEach(pair -> this.processPair(pair));
        }
    }
}
