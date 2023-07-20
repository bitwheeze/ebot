package bitwheeze.golos.exchangebot.services;

import bitwheeze.golos.exchangebot.config.EbotProperties;
import bitwheeze.golos.exchangebot.config.PricesProperties;
import bitwheeze.golos.exchangebot.config.TradingPair;
import bitwheeze.golos.exchangebot.events.BlockchainErrorEvent;
import bitwheeze.golos.exchangebot.events.info.FillOrderEvent;
import bitwheeze.golos.exchangebot.events.info.NewOrderEvent;
import bitwheeze.golos.exchangebot.model.ebot.Order;
import bitwheeze.golos.exchangebot.services.helpers.Balances;
import bitwheeze.golos.goloslib.*;
import bitwheeze.golos.goloslib.model.Asset;
import bitwheeze.golos.goloslib.model.exception.BlockchainError;
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
import java.util.stream.Collectors;

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
                .filter(order -> order.getAmountToSell().compareTo(BigDecimal.ZERO) > 0)
                .map(order -> {
                    var orderCreate = buildLimitOrderCreate(order, pair.getAccount());
                    return orderCreate;
                })
                .filter(op -> op != null)
                .peek( order -> log.info("limit order create {}", order))
                .forEach(orderCreate -> ops.add(orderCreate));

        if(ops.isEmpty()) return;
        ops.forEach(order -> builder.add(order));
        var tr = builder.buildAndSign(new String [] {pair.getKey()});
        try {
            netApi.broadcastTransaction(tr).block().orElseThrow();
            log.info("transaction send to blockchain!");
            publisher.publishEvent(new NewOrderEvent(orderList));
        } catch (BlockchainError error) {
            log.error("Exception while sending transaction to blockchain! {}", error.getError());
            publisher.publishEvent(new BlockchainErrorEvent(error.getError()));
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

    @PostConstruct
    public void init() {
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
        log.info("median price = {}", medianPrice);
        var base = medianPrice.getBase();
        var quote = medianPrice.getQuote();
        if(base.getAsset().equals("GOLOS")) {
            base = quote;
            quote = medianPrice.getBase();
        }
        var gbgPriceInGolos = quote.getValue().divide(base.getValue(), RoundingMode.HALF_DOWN);
        log.info("price of 1 {} = {}", base.getAsset(), gbgPriceInGolos);
        final var golosPriceInBaseOpt = priceService.convert(gbgPriceInGolos, "GOLOS", pricesProperties.getBaseAsset());
        if(golosPriceInBaseOpt.isEmpty()) {
            log.warn("No GOLOS price available!");
            return;
        }
        log.info("price of 1 {} in {} = {}", base.getAsset(), pricesProperties.getBaseAsset(), golosPriceInBaseOpt);
        priceService.updatePrice("GBG", golosPriceInBaseOpt.get(), LocalDateTime.now(ZoneOffset.UTC));
        log.info("test 1 GBG in GOLOS = {}", priceService.convert(BigDecimal.ONE, "GBG", "GOLOS"));
        log.info("test 1 GOLOS in {} = {}", pricesProperties.getBaseAsset(), priceService.convert(BigDecimal.ONE, "GOLOS", pricesProperties.getBaseAsset()));
        log.info("test 1 GOLOS in GBG = {}", priceService.convert(BigDecimal.ONE, "GOLOS", "GBG"));
    }

    public void closeAllOpenOrders(List<TradingPair> pairList, String asset) {



        final var builder = transactionFactory.getBuidler();
        Set<LimitOrderCancel> ops = new HashSet<>();
        pairList.stream()
                .filter(p -> asset != null?p.getBase().equals(asset) || p.getQuote().equals(asset):true)
                .forEach(pair -> {
            api.getOpenOrders(pair.getAccount(), pair.getBase(), pair.getQuote())
                    .block()
                    .orElseThrow()
                    .stream()
                    .peek(openOrder -> {log.info("open order id={}, expiration={}", openOrder.getId(), openOrder.getExpiration());})
                    .map(openOrder -> {
                        var cancelOp = new LimitOrderCancel();
                        cancelOp.setOwner(openOrder.getSeller());
                        cancelOp.setOrderid(openOrder.getOrderid());
                        return cancelOp;
                    })
                    .distinct()
                    .forEach(op -> ops.add(op));
        });
        if(ops.isEmpty()) {
            return;
        }
        log.info("cancel orders {}", ops);
        ops.forEach(op -> builder.add(op));
        var keys = pairList.stream().map(pair -> pair.getKey()).distinct().collect(Collectors.toList());
        var tr = builder.buildAndSign(keys.toArray(new String [keys.size()]));
        try {
            netApi.broadcastTransaction(tr).block().orElseThrow();
            log.info("transaction send to blockchain!");
        } catch (BlockchainError error) {
            log.error("Exception while sending transaction to blockchain! {}", error.getError());
            publisher.publishEvent(new BlockchainErrorEvent(error.getError()));
        }

    }

    public Balances getAccBalances(String account) {
        var balances = new Balances();
        var acc = dbApi.getAccount(account).block().orElseThrow();

        balances.add("GOLOS", acc.getBalance().getValue());
        balances.add("GOLOS", acc.getMarketBalance().getValue());

        balances.add("GBG", acc.getSbdBalance().getValue());
        balances.add("GBG", acc.getMarketSbdBalance().getValue());

        var uiaList = dbApi.getAccountsBalances(new String [] {account}).block().orElseThrow();
        log.info("uiaList {}", uiaList);
        uiaList
                .get(0)
                .keySet()
                .forEach(asset -> {
                    var inf = uiaList.get(0).get(asset);
                    balances.add(asset, inf.getBalance().getValue());
                    balances.add(asset, inf.getMarketBalance().getValue());
                });

        log.info("balances = {}", balances);
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
