package bitwheeze.golos.exchangebot.components;

import bitwheeze.golos.exchangebot.config.RelativeOrders;
import bitwheeze.golos.exchangebot.config.TradingPair;
import bitwheeze.golos.exchangebot.model.ebot.Order;
import bitwheeze.golos.exchangebot.services.CmcService;
import bitwheeze.golos.exchangebot.services.GolosService;
import bitwheeze.golos.exchangebot.services.PriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class RelativeOrdersStrategy {

    private final CmcService cmcService;
    private final GolosService golosService;
    private final ApplicationEventPublisher publisher;
    private final PriceService priceService;

    public void proccessPair(TradingPair pair) {
        log.info("Processing pair {}", pair);
        closeOpenOrders(pair);
        createOrders(pair);
    }

    private void createOrders(TradingPair pair) {
        log.info("create new orders " + pair);
        List<Order> orderList = generateOrders(pair);
        log.info("created orders {} {}", orderList.size(), orderList);
        golosService.createOrders(pair, orderList);
    }

    private List<Order> generateOrders(TradingPair pair) {
        var list = new ArrayList<Order>();

        switch(pair.getRelativeOrders().getMode()) {
            case Sell:
            case Balance:
                list.addAll(generateSellOrders(pair));
        }

        switch(pair.getRelativeOrders().getMode()) {
            case Buy:
            case Balance:
                list.addAll(generateBuyOrders(pair));
        }

        return  list;
    }

    private Collection<? extends Order> generateOrders(TradingPair pair, String base, String quote, BigDecimal availableAmountBase, BigDecimal availableAmountQuote) {

        log.info("***** generate orders for sell {} buy {} *****", base, quote);
        var list = new ArrayList<Order>();

        var middlePriceOpt = getMiddlePrice(pair, base, quote, availableAmountBase, availableAmountQuote);

        if(middlePriceOpt.isEmpty() || middlePriceOpt.get().equals(BigDecimal.ZERO)) {
            log.warn("no middle price available! {}/{}", base, quote);
            return Collections.emptyList();
        }

        var middlePrice = middlePriceOpt.get();
        log.info("middle price = {}", middlePrice);

        var spread = calculateSpread(pair.getRelativeOrders().getSpread(), middlePrice);
        log.info("spread {}", spread);

        var availableAmount = availableAmountBase;
        var orderAmount = getStartOrderAmount(pair, base, quote);


        if(orderAmount.compareTo(availableAmount) > 0) {
            log.info("available amount less then required, set order amount to available");
            orderAmount = availableAmount;
        }

        var orderPrice = middlePrice.add(spread);

        log.info("start order amount in {} = {}", base, orderAmount);

        for(int i = 0; i < pair.getRelativeOrders().getOrdersCount(); i++) {
            log.info("****create order");

            log.info("\torder amount in {} = {}", base, orderAmount);
            log.info("\tavaillableAmount in {} = {}", base, availableAmount);
            log.info("\torderPrice in {} = {}", base, orderPrice);

            if(availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            var minToReceive = orderAmount.multiply(orderPrice);
            log.info("\tmin to receive {}", minToReceive);
            Order order = createOrder(base, quote, orderAmount, minToReceive, pair.getExpiration());
            log.info("\tgenerated order {}", order);
            list.add(order);

            //reduce availableAmount
            availableAmount = availableAmount.subtract(orderAmount);
            log.info("\t\tnew availlableAmount in {} = {}", base, availableAmount);
            //change price
            orderPrice = orderPrice.add(orderPrice.multiply(pair.getRelativeOrders().getOrderPriceIncreasePercent()).divide(BigDecimal.valueOf(100.00), RoundingMode.DOWN));
            //change order amount
            orderAmount = orderAmount.multiply(pair.getRelativeOrders().getOrderVolumeChangePercent()).divide(BigDecimal.valueOf(100.00), RoundingMode.DOWN);
            log.info("\t\tnew order amount in {} = {} ({})", base, orderAmount, orderAmount.compareTo(availableAmount));
            //check order Amount against available amount

            if(orderAmount.compareTo(availableAmount) > 0) {
                log.info("\t\tavailable amount less then required, set order amount to available");
                orderAmount = availableAmount;
            }

            if(orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }

        return list;
    }

    private BigDecimal getStartOrderAmount(TradingPair pair, String base, String quote) {
        var startAmount = pair.getRelativeOrders().getStartOrderAmount();
        if(quote.equals(pair.getBase())) {
            var amountInBase = priceService.convert(startAmount, quote, base);
            if (amountInBase.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return amountInBase.get();
        }
        return startAmount;
    }

    @NotNull
    private static Order createOrder(String base, String quote, BigDecimal orderAmount, BigDecimal minToReceive, int expiration) {
        var order = new Order();
        order.setAmountToSell(orderAmount);
        order.setAssetToSell(base);
        order.setAssetToReceive(quote);
        order.setMinToReceive(minToReceive);
        order.setExpiration(Instant.now().plus(expiration, ChronoUnit.MINUTES));
        return order;
    }

    private BigDecimal getAvialableAmount(TradingPair pair, String asset) {
        var availableAmount = BigDecimal.ZERO;
        var balances = golosService.getAccBalances(pair.getAccount());
        if(!balances.containsKey(asset)) {
            return availableAmount;
        }

        availableAmount = balances.get(asset);

        log.info("asset amount in {} = {}", asset,availableAmount);

        var reserve = availableAmount.multiply(pair.getReserve()).divide(BigDecimal.valueOf(100.00), RoundingMode.HALF_DOWN);
        availableAmount = availableAmount.subtract(reserve);

        log.info("available amount in {} = {}", asset,availableAmount);

        if(availableAmount.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }

        return availableAmount;
    }

    private Collection<? extends Order> generateBuyOrders(TradingPair pair) {
        //Buy for lower prices  middlePrice + Spread * -1.0
        //We sell quote for base

        var availableAmountQuote = getAvialableAmount(pair, pair.getQuote());
        var availableAmountBase = getAvialableAmount(pair, pair.getBase());

        return generateOrders(pair, pair.getQuote(), pair.getBase(), availableAmountQuote, availableAmountBase);
    }

    private Collection<? extends Order> generateSellOrders(TradingPair pair) {
        //Sell for higher prices  middlePrice + Spread * 1.0
        //We sell quote for base
        var availableAmountQuote = getAvialableAmount(pair, pair.getQuote());
        var availableAmountBase = getAvialableAmount(pair, pair.getBase());

        return generateOrders(pair, pair.getBase(), pair.getQuote(), availableAmountBase, availableAmountQuote);
    }

    private BigDecimal calculateSpread(BigDecimal spreadPercent, BigDecimal middlePrice) {
        return middlePrice.multiply(spreadPercent).divide(BigDecimal.valueOf(100.0), RoundingMode.HALF_DOWN);
    }


    private Optional<BigDecimal> getMiddlePrice(TradingPair pair, String base, String quote, BigDecimal availableAmountBase, BigDecimal availableAmountQuote) {
        //TODO: use other strategies
        var price = priceService.convert(BigDecimal.ONE, base, quote);
        if(price.isEmpty()) return price;
        var quoteInBase = priceService.convert(availableAmountQuote, quote, base);

        if(quoteInBase.isPresent()) {
            if(availableAmountBase.compareTo(quoteInBase.get()) < 0) {
                var spread = calculateSpread(pair.getRelativeOrders().getSpread(), price.get());
                var newPrice = price.get().add(spread);
                return Optional.of(newPrice);
            }
        }

        return price;
    }

    private void closeOpenOrders(TradingPair pair) {
        log.info("close all open orders " + pair);
        golosService.closeAllOpenOrders(pair);
    }

    public static boolean validate(RelativeOrders config) {
        //TODO: replace with validation
        log.info("validate config {}", config);
        if(config.getStartOrderAmount() == null || config.getStartOrderAmount().equals(BigDecimal.ZERO)) {
            log.warn("MaxOrderAmount is empty!");
            return false;
        }
        return true;
    }
}
