package bitwheeze.golos.exchangebot.components;

import bitwheeze.golos.exchangebot.config.EbotProperties;
import bitwheeze.golos.exchangebot.config.TradingPair;
import bitwheeze.golos.exchangebot.events.info.FillOrderEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
        for(var pair : ebotProps.getPairs()) {
            switch (validatePair(pair)) {
                case RelativeOrders:
                    relativeOrders.proccessPair(pair);
                    break;
            }
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
        return ebotProps.getPairs()
                .stream()
                .flatMap( pair -> Stream.of(pair.getQuote(), pair.getBase()))
                .collect(Collectors.toSet());
    }

    @EventListener
    public void onEvent(FillOrderEvent event) {
        var check = getConfiguredAssets().stream().anyMatch(asset -> event.getFillOrder().getOpenPays().getAsset().equals(asset));
        if(check) {
            log.info("changes in order book");
            processTradingPairs();
        }
    }
}
