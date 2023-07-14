package bitwheeze.golos.exchangebot.components;

import bitwheeze.golos.exchangebot.config.EbotProperties;
import bitwheeze.golos.exchangebot.config.TradingPair;
import bitwheeze.golos.exchangebot.events.info.ChangedPriceEvent;
import bitwheeze.golos.exchangebot.model.ebot.Order;
import bitwheeze.golos.exchangebot.services.CmcService;
import bitwheeze.golos.exchangebot.services.GolosService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
@RequiredArgsConstructor
public class Ebot {

    private final EbotProperties ebotProps;
    private final RelativeOrdersStrategy relativeOrders;
    private final GolosService golosService;
    private final CmcService cmcService;

    public void processTradingPairs() {
        if(ebotProps.getPairs() != null) {

            Map<TradingPair, List<Order>> orders = new HashMap<>();

            for (var pair : ebotProps.getPairs()) {
                var orderList = processPair(pair);
                if(!orderList.isEmpty()) {
                    orders.put(pair, orderList);
                }
            }

            for(var pair : orders.entrySet()) {
                golosService.createOrders(pair.getKey(), pair.getValue());
            }
        }
    }

    private List<Order> processPair(TradingPair pair) {
        switch (validatePair(pair)) {
            case RelativeOrders:
                return relativeOrders.proccessPair(pair);
        }

        return Collections.emptyList();
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
        cmcService.queryCmcQuotes();
        golosService.retrieveGlsPrice();
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
        var threshold = ebotProps.getPriceChangeThreshold();
        var change = event.getChange().abs();
        if(change.compareTo(threshold) < 0) return;
        if(ebotProps.getPairs() != null) {
            ebotProps.getPairs()
                .stream()
                .filter(pair -> event.getBase().equals(pair.getBase()) || event.getBase().equals(pair.getQuote()))
                .forEach(pair -> this.processPair(pair));
        }
    }
}
