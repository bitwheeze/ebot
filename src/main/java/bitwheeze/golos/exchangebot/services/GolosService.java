package bitwheeze.golos.exchangebot.services;

import bitwheeze.golos.exchangebot.config.EbotProperties;
import bitwheeze.golos.exchangebot.model.ebot.Order;
import bitwheeze.golos.goloslib.GolosApiReactive;
import bitwheeze.golos.goloslib.TransactionFactory;
import bitwheeze.golos.goloslib.model.Asset;
import bitwheeze.golos.goloslib.model.op.LimitOrderCreate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GolosService {

    private final GolosApiReactive api;
    private final EbotProperties ebotProps;
    private final TransactionFactory transactionFactory;

    public void createOrders(List<Order> orderList) {

    }

    private LimitOrderCreate buildLimitOrderCreate(Order order, String account, int expirationInMinutes) {
        var amountToSell = Asset.builder()
                                .asset(order.getAssetToSell())
                                .value(order.getAmountToSell())
                                .build();

        var amountToReceive = Asset.builder()
                .asset(order.getAssetToReceive())
                .value(order.getMinToReceive())
                .build();


        var limitOrder = new LimitOrderCreate();

        limitOrder.setOwner(account);
        limitOrder.setAmountToSell(amountToSell);
        limitOrder.setMinToReceive(amountToReceive);
        limitOrder.setFillOrKill(false);
        limitOrder.setExpiration(LocalDateTime.now(ZoneOffset.UTC).now().plus(expirationInMinutes, ChronoUnit.MINUTES));

        return limitOrder;
    }

    public void cancelOrder(long id) {

    }


}
