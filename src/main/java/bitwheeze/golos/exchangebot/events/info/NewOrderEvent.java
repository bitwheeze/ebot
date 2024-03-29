package bitwheeze.golos.exchangebot.events.info;

import bitwheeze.golos.exchangebot.events.EbotEvent;
import bitwheeze.golos.exchangebot.model.ebot.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NewOrderEvent extends EbotEvent {
    List<Order> orderList;
}
