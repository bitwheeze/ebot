package bitwheeze.golos.exchangebot.components;

import bitwheeze.golos.exchangebot.config.EbotProperties;
import bitwheeze.golos.exchangebot.config.RelativeOrders;
import bitwheeze.golos.exchangebot.config.TradingPair;
import bitwheeze.golos.exchangebot.model.ebot.Order;
import bitwheeze.golos.exchangebot.services.CmcService;
import bitwheeze.golos.exchangebot.services.PriceService;
import bitwheeze.golos.exchangebot.services.helpers.Balances;
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
    private final ApplicationEventPublisher publisher;
    private final PriceService priceService;
    private final EbotProperties ebotProperties;

    public List<Order> proccessPair(TradingPair pair, Balances balances) {
        log.info("Processing pair {}", pair);
        return generateOrders(pair, balances);
    }

    private List<Order> generateOrders(TradingPair pair, Balances balances) {
        var list = new ArrayList<Order>();

        switch(pair.getRelativeOrders().getMode()) {
            case Sell:
            case Balance:
                list.addAll(generateSellOrders(pair, balances));
        }

        switch(pair.getRelativeOrders().getMode()) {
            case Buy:
            case Balance:
                list.addAll(generateBuyOrders(pair, balances));
        }

        return  list;
    }

    private Collection<? extends Order> generateOrders(TradingPair pair, String base, String quote, Balances balances) {
        log.info("");
        log.info("\n\n*******************************************************************");
        log.info("***** generate orders for sell {} buy {} *****", base, quote);
        log.info("*******************************************************************");
        var list = new ArrayList<Order>();

        var middlePriceOpt = getMiddlePrice(pair, base, quote);

        if(middlePriceOpt.isEmpty() || middlePriceOpt.get().equals(BigDecimal.ZERO)) {
            log.warn("no middle price available! {}/{}", base, quote);
            return Collections.emptyList();
        }

        var middlePrice = middlePriceOpt.get();
        log.info("middle price = {}", middlePrice);

        var spread = calculateSpread(pair.getRelativeOrders().getSpread(), middlePrice);
        log.info("spread {}", spread);

        var orderAmount = getStartOrderAmount(pair, base, quote);

        log.info("Available amount {} {}", base, balances.getBalance(base));

        orderAmount = balances.queryAmount(base, orderAmount);

        var orderPrice = middlePrice.add(spread);

        log.info("start order amount in {} = {}", base, orderAmount);

        for(int i = 0; i < pair.getRelativeOrders().getOrdersCount(); i++) {
            log.info("****create order");

            log.info("\torder amount in {} = {}", base, orderAmount);
            log.info("\torderPrice in {} = {}", base, orderPrice);

            if(orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            var expiration = ebotProperties.getExpiration();
            if(pair.getExpiration() > 0) {
                expiration = pair.getExpiration();
            }

            var minToReceive = orderAmount.multiply(orderPrice);
            log.info("\tmin to receive {}", minToReceive);
            Order order = createOrder(base, quote, orderAmount, minToReceive, expiration);
            log.info("\tgenerated order {}", order);
            list.add(order);

            //change price
            orderPrice = orderPrice.add(orderPrice.multiply(pair.getRelativeOrders().getOrderPriceIncreasePercent()).divide(BigDecimal.valueOf(100.00), RoundingMode.DOWN));
            //change order amount
            orderAmount = orderAmount.multiply(pair.getRelativeOrders().getOrderVolumeChangePercent()).divide(BigDecimal.valueOf(100.00), RoundingMode.DOWN);
            orderAmount = balances.queryAmount(base, orderAmount);

            log.info("\t\tnew order amount in {} = {}", base, orderAmount);
            log.info("remeining amount {} {}", base, balances.getBalance(base));
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

    private Collection<? extends Order> generateBuyOrders(TradingPair pair, Balances balances) {
        //Buy for lower prices  middlePrice + Spread * -1.0
        //We sell quote for base

        return generateOrders(pair, pair.getQuote(), pair.getBase(), balances);
    }

    private Collection<? extends Order> generateSellOrders(TradingPair pair, Balances balances) {
        //Sell for higher prices  middlePrice + Spread * 1.0
        //We sell quote for base
        return generateOrders(pair, pair.getBase(), pair.getQuote(), balances);
    }

    private BigDecimal calculateSpread(BigDecimal spreadPercent, BigDecimal middlePrice) {
        return middlePrice.multiply(spreadPercent).divide(BigDecimal.valueOf(100.0), RoundingMode.HALF_DOWN);
    }


    private Optional<BigDecimal> getMiddlePrice(TradingPair pair, String base, String quote) {
        //TODO: use other strategies
        var price = priceService.convert(BigDecimal.ONE, base, quote);
        if(price.isEmpty()) return price;

        return price;
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
