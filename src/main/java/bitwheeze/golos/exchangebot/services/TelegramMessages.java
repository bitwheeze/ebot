package bitwheeze.golos.exchangebot.services;

import bitwheeze.golos.exchangebot.events.EbotEvent;
import bitwheeze.golos.exchangebot.events.info.FillOrderEvent;
import bitwheeze.golos.exchangebot.events.info.NewOrderEvent;

import java.math.RoundingMode;

public class TelegramMessages {

    public static String translate(EbotEvent event) {
        if(event instanceof NewOrderEvent e) {
            return transalte(e);
        } else if(event instanceof FillOrderEvent fillOrderEvent) {
            return transalte(fillOrderEvent);
        }
        return event.toString();
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
