package bitwheeze.golos.exchangebot.events.info;


import bitwheeze.golos.exchangebot.events.EbotEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChangedPriceEvent extends EbotEvent {
    String base;
    String quote;
    BigDecimal price;
}
