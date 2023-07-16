package bitwheeze.golos.exchangebot.events.info;

import bitwheeze.golos.exchangebot.events.EbotEvent;
import bitwheeze.golos.exchangebot.services.helpers.Balances;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Data
@RequiredArgsConstructor
public class AvailableAmountEvent extends EbotEvent {
    private final Map<String, Balances> balances;
}
