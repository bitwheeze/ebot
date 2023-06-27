package bitwheeze.golos.exchangebot.events.info;

import bitwheeze.golos.exchangebot.events.EbotEvent;
import bitwheeze.golos.exchangebot.model.ebot.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
public class NewOrderEvent extends EbotEvent {
    Order order;
}
