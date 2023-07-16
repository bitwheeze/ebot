package bitwheeze.golos.exchangebot.services;

import bitwheeze.golos.exchangebot.events.EbotEvent;
import bitwheeze.golos.exchangebot.events.info.AvailableAmountEvent;
import bitwheeze.golos.exchangebot.events.info.ChangedPriceEvent;
import bitwheeze.golos.exchangebot.events.info.FillOrderEvent;
import bitwheeze.golos.exchangebot.events.info.NewOrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramMessages {

    private final PriceService priceService;

    public String translate(EbotEvent event) {
        if(event instanceof NewOrderEvent e) {
            return transalte(e);
        } else if(event instanceof FillOrderEvent fillOrderEvent) {
            return transalte(fillOrderEvent);
        } else if (event instanceof ChangedPriceEvent changedPriceEvent) {
            return transalte(changedPriceEvent);
        } else if (event instanceof AvailableAmountEvent amountEvent) {
            return translate(amountEvent);
        }

        return event.toString();
    }

    private String translate(AvailableAmountEvent event) {
        if(event.getBalances().isEmpty()) return "No balances!";
        var msg = new StringBuilder("```");
        var balances = event.getBalances();
        balances.keySet().stream().sorted().forEach(account -> {
            msg.append("\n").append(account).append("\n-------------------------------\n");
            var accBalances = balances.get(account);
            var sum = BigDecimal.ZERO;
            for(String asset : accBalances.getAssetList()) {
                msg.append(String.format("%16s %-14s\n", accBalances.getBalance(asset).setScale(5, RoundingMode.HALF_DOWN), asset));
                var usd = priceService.convert(accBalances.getBalance(asset), asset, "USD");
                if(usd.isPresent()) {
                    sum = sum.add(usd.get());
                }
            }
            msg.append("-------------------------------\n");
            msg.append(String.format("%16s %-14s\n", sum.setScale(5, RoundingMode.HALF_DOWN), "USD"));

        });
        msg.append("```");
        log.debug("Message: \n{}", msg.toString());
        return msg.toString();
    }

    private static String transalte(ChangedPriceEvent event) {
        var arrow = event.getChange().compareTo(BigDecimal.ZERO) > 0 ? "↑" : event.getChange().compareTo(BigDecimal.ZERO) < 0 ? "↓": "";
        var message = String.format("Changed price of %s, new price %s %s (%s %s%%)", event.getBase(), event.getPrice().setScale(6, RoundingMode.HALF_DOWN), event.getQuote(), arrow, event.getChange().setScale(2, RoundingMode.HALF_DOWN));
        return message;
    }

    private static String transalte(FillOrderEvent fillOrderEvent) {
        var fo = fillOrderEvent.getFillOrder();
        var message = String.format("Filled order of %s, sell=%s, receive=%s from=%s, fee=%s",
                fo.getOpenOwner(), fo.getOpenPays(), fo.getCurrentPays(), fo.getCurrentOwner(), fo.getOpenTradeFee());
        return message;
    }

    private static String transalte(NewOrderEvent event) {
        var order = event.getOrder();
        var message = String.format("New order, sell %s %s, receive %s %s, price=%s"
                , order.getAmountToSell().setScale(6, RoundingMode.HALF_DOWN)
                , order.getAssetToSell()
                , order.getMinToReceive().setScale(6, RoundingMode.HALF_DOWN)
                , order.getAssetToReceive()
                , order.getMinToReceive().divide(order.getAmountToSell(), RoundingMode.HALF_DOWN).setScale(6, RoundingMode.HALF_DOWN)
        );
        return message;
    }
}
