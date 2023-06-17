package bitwheeze.golos.exchangebot.components;

import bitwheeze.golos.exchangebot.config.EbotProperties;
import bitwheeze.golos.exchangebot.config.TradingPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
}
