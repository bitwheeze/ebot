package bitwheeze.golos.exchangebot.components;

import bitwheeze.golos.exchangebot.config.TradingPair;
import bitwheeze.golos.exchangebot.services.CmcService;
import bitwheeze.golos.exchangebot.services.GolosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RelativeOrdersStrategy {

    private final CmcService cmcService;
    private final GolosService golosService;

    public void proccessPair(TradingPair pair) {
        log.info("Processing pair {}", pair);
        closeOpenOrders(pair);
        createOrders(pair);
    }

    private void createOrders(TradingPair pair) {
        log.info("create new orders " + pair);

    }

    private void closeOpenOrders(TradingPair pair) {
        log.info("close all open orders " + pair);
    }
}
