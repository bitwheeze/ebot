package bitwheeze.golos.exchangebot.services;

import bitwheeze.golos.exchangebot.config.EbotProperties;
import bitwheeze.golos.exchangebot.config.PricesProperties;
import bitwheeze.golos.exchangebot.config.TradingPair;
import bitwheeze.golos.exchangebot.events.info.FillOrderEvent;
import bitwheeze.golos.exchangebot.events.info.NewOrderEvent;
import bitwheeze.golos.exchangebot.model.ebot.Order;
import bitwheeze.golos.goloslib.*;
import bitwheeze.golos.goloslib.model.Asset;
import bitwheeze.golos.goloslib.model.op.LimitOrderCancel;
import bitwheeze.golos.goloslib.model.op.LimitOrderCreate;
import bitwheeze.golos.goloslib.model.op.Operation;
import bitwheeze.golos.goloslib.model.op.virtual.FillOrder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class GolosService {
    private final DatabaseApi dbApi;
    private final NetworkBroadcastApi netApi;
    private final MarketHistoryApi api;
    private final WitnessApi witnessApi;
    private final EventApi eventApi;
    private final EbotProperties ebotProps;
    private final TransactionFactory transactionFactory;
    private final PricesProperties pricesProperties;
    private final PriceService priceService;
    private final ApplicationEventPublisher publisher;
    private long orderId = new Date().getTime();

    private long currentBlock = 0;

    public void createOrders(TradingPair pair, List<Order> orderList) {
        final var builder = transactionFactory.getBuidler();
        List<Operation> ops = new ArrayList<>();
        orderList.stream()
                .peek(order -> publisher.publishEvent(new NewOrderEvent(order)))
                .map(order -> {
                    var orderCreate = buildLimitOrderCreate(order, pair.getAccount());
                    return orderCreate;
                })
                .filter(op -> op != null)
                .forEach(orderCreate -> ops.add(orderCreate));

        if(ops.isEmpty()) return;
        ops.forEach(order -> builder.add(order));
        var tr = builder.buildAndSign(new String [] {pair.getKey()});
        netApi.broadcastTransaction(tr).block().orElseThrow();
        try {
            //wait till transaction is accepted
            Thread.sleep(9000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private LimitOrderCreate buildLimitOrderCreate(Order order, String account) {

        var defAssetSell = this.getAssetDefintion(order.getAssetToSell());
        var defAssetReceive = this.getAssetDefintion(order.getAssetToReceive());

        if(defAssetSell.isEmpty()) {
            log.warn("unknown golos asset {}", order.getAssetToSell());
            return null;
        }

        if(defAssetReceive.isEmpty()) {
            log.warn("unknown golos asset {}", order.getAssetToReceive());
            return null;
        }

        var amountToSell = Asset.builder()
                                .asset(order.getAssetToSell())
                                .value(order.getAmountToSell().setScale(defAssetSell.get().getPrecision(), RoundingMode.DOWN))
                                .build();

        if(amountToSell.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("zerror amount to sell! {}", amountToSell);
            return null;
        }

        var amountToReceive = Asset.builder()
                .asset(order.getAssetToReceive())
                .value(order.getMinToReceive().setScale(defAssetReceive.get().getPrecision(), RoundingMode.DOWN))
                .build();

        if(amountToReceive.getValue().compareTo(BigDecimal.ZERO) <= 0 || amountToSell.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("not enough amount for an order {}/{}", amountToSell, amountToReceive);
            return null;
        }

        var limitOrder = new LimitOrderCreate();
        limitOrder.setOrderid(getOrderId());
        limitOrder.setOwner(account);
        limitOrder.setAmountToSell(amountToSell);
        limitOrder.setMinToReceive(amountToReceive);
        limitOrder.setFillOrKill(false);
        limitOrder.setExpiration(LocalDateTime.ofInstant(order.getExpiration(), ZoneOffset.UTC));

        return limitOrder;
    }

    private synchronized long  getOrderId() {
         return orderId++;
    }

    public void cancelOrder(long id) {

    }

    @PostConstruct
    public void init() {
        retrieveGlsPrice();
        initScanner();
    }

    private void initScanner() {
        var props = dbApi.getDynamicGlobalProperties().block().orElseThrow();
        this.currentBlock = props.getHeadBlockNumber();
    }


    @Scheduled(cron = "#{@ebotGolosProperties.feedCron}")
    public void retrieveGlsPrice() {
        log.info("retrieve current GLS/GOLOS feed");
        var medianPrice = witnessApi.getGetCurrentMedianHistoryPrice().block().orElseThrow();
        var base = medianPrice.getBase().getValue();
        var quote = medianPrice.getQuote().getValue();
        if(medianPrice.getBase().getAsset().equals(pricesProperties.getBaseAsset())) {
            base = quote;
            quote = medianPrice.getBase().getValue();
        }
        quote = quote.divide(base, RoundingMode.HALF_DOWN);
        priceService.updatePrice("GBG", quote, LocalDateTime.now(ZoneOffset.UTC));
    }

    public void closeAllOpenOrders(TradingPair pair, String base, String quote) {
        final var builder = transactionFactory.getBuidler();
        List<Operation> ops = new ArrayList<>();
        api.getOpenOrders(pair.getAccount(), pair.getBase(), pair.getQuote())
                .block()
                .orElseThrow()
                .stream()
                .filter(openOrder -> openOrder.getAsset1().getAsset().equals(base))
                .map(openOrder -> {
                    var cancelOp = new LimitOrderCancel();
                    cancelOp.setOwner(openOrder.getSeller());
                    cancelOp.setOrderid(openOrder.getOrderid());
                    return cancelOp;
                })
                .forEach(op -> ops.add(op));

        if(ops.isEmpty()) {
            return;
        }
        ops.forEach(op -> builder.add(op));
        var tr = builder.buildAndSign(new String [] {pair.getKey()});
        netApi.broadcastTransaction(tr).block().orElseThrow();

    }

    public Map<String,BigDecimal> getAccBalances(String account) {
        var balances = new HashMap<String,BigDecimal>();
        var acc = dbApi.getAccount(account).block().orElseThrow();
        balances.put("GOLOS", acc.getBalance().getValue());
        balances.put("GBG", acc.getSbdBalance().getValue());

        var uiaList = dbApi.getAccountsBalances(new String [] {account}).block().orElseThrow();

        uiaList
                .get(0)
                .keySet()
                .forEach(asset -> balances.put(asset, uiaList.get(0).get(asset).getBalance().getValue()));
        return balances;
    }

    @Cacheable("assets")
    public Optional<AssetDefinition> getAssetDefintion(String asset) {
        if("GOLOS".equals(asset) || "GBG".equals(asset)) {
            var def = new AssetDefinition();
            def.setPrecision(3);
            return Optional.of(def);
        }
        var assetsList = dbApi.getAssets("", new String [] {asset}, "", 1, null).block().orElseThrow();
        if(assetsList.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(assetsList.get(0));
    }

    @Scheduled(fixedDelay = 9000)
    public void scan() {
        var props = dbApi.getDynamicGlobalProperties().block().orElseThrow();
        while(this.currentBlock++ < props.getHeadBlockNumber()) {
            var eventList = eventApi.getEventsInBlock(this.currentBlock, true).block().orElseThrow();
            for(var event : eventList) {
                var op = event.getOp().getOp();
                if(op instanceof FillOrder fillOrder) {
                    publisher.publishEvent(new FillOrderEvent(fillOrder));
                }
            }
        }
    }
}
