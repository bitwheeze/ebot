package bitwheeze.golos.exchangebot.events.info;


import bitwheeze.golos.exchangebot.events.EbotEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
@AllArgsConstructor
public class ChangedPriceEvent extends EbotEvent {
    String base;
    String quote;
    BigDecimal price;
}
