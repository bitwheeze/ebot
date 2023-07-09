package bitwheeze.golos.exchangebot.services;

import bitwheeze.golos.exchangebot.events.EbotEvent;
import bitwheeze.golos.exchangebot.events.info.ChangedPriceEvent;
import bitwheeze.golos.exchangebot.events.info.FillOrderEvent;
import bitwheeze.golos.exchangebot.events.info.NewOrderEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TelegramMessages {

    public static String translate(EbotEvent event) {
        if(event instanceof NewOrderEvent e) {
            return transalte(e);
        } else if(event instanceof FillOrderEvent fillOrderEvent) {
            return transalte(fillOrderEvent);
        } else if (event instanceof ChangedPriceEvent changedPriceEvent) {
            return transalte(changedPriceEvent);
        }
        return event.toString();
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
