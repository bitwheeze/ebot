package bitwheeze.golos.exchangebot.components;

import bitwheeze.golos.exchangebot.config.EbotProperties;
import bitwheeze.golos.exchangebot.config.TradingPair;
import bitwheeze.golos.exchangebot.events.info.AvailableAmountEvent;
import bitwheeze.golos.exchangebot.events.info.ChangedPriceEvent;
import bitwheeze.golos.exchangebot.model.ebot.Order;
import bitwheeze.golos.exchangebot.services.CmcService;
import bitwheeze.golos.exchangebot.services.GolosService;
import bitwheeze.golos.exchangebot.services.helpers.Balances;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher publisher;

    public void processTradingPairs(String asset) {


        if(ebotProps.getPairs() == null)  return;

        golosService.closeAllOpenOrders(ebotProps.getPairs(), asset);

        var balances = getBalances();

        publisher.publishEvent(new AvailableAmountEvent(balances));

        log.info("all balances = {}", balances);

        Map<TradingPair, List<Order>> orders = new HashMap<>();

        for (var pair : ebotProps.getPairs()) {

            if(asset != null && !(pair.getBase().equals(asset) || pair.getQuote().equals(asset))) {
                continue;
            }

            var orderList = processPair(pair, balances.get(pair.getAccount()));
            if(!orderList.isEmpty()) {
                orders.put(pair, orderList);
            }
        }

        for(var pair : orders.entrySet()) {
            golosService.createOrders(pair.getKey(), pair.getValue());
        }

        log.info("remaining balances {}", balances);

    }

    private HashMap<String, Balances> getBalances() {
        var balances = new HashMap<String, Balances>();
        for (var pair : ebotProps.getPairs()) {
            if(!balances.containsKey(pair.getAccount())) {
                balances.put(pair.getAccount(), golosService.getAccBalances(pair.getAccount()));
            }
        }
        return balances;
    }

    private List<Order> processPair(TradingPair pair, Balances balances) {
        switch (validatePair(pair)) {
            case RelativeOrders:
                return relativeOrders.proccessPair(pair, balances);
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
        processTradingPairs(null);
    }

    @PostConstruct
    public void init() {
        log.info("process pairs on start");
        cmcService.queryCmcQuotes();
        golosService.retrieveGlsPrice();
        processTradingPairs(null);
    }

    public Set<String> getConfiguredAssets() {
        if(null == ebotProps.getPairs()) return Collections.emptySet();
        return ebotProps.getPairs()
                .stream()
                .flatMap( pair -> Stream.of(pair.getQuote(), pair.getBase()))
                .collect(Collectors.toSet());
    }
    //@EventListener
    public void onEvent(ChangedPriceEvent event) {
        var threshold = ebotProps.getPriceChangeThreshold();
        var change = event.getChange().abs();
        if(change.compareTo(threshold) < 0) return;
        processTradingPairs(event.getBase());
    }
}
