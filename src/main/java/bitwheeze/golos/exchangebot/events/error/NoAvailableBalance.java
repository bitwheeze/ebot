package bitwheeze.golos.exchangebot.events.error;

import bitwheeze.golos.exchangebot.events.EbotEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
public class NoAvailableBalance extends EbotEvent {
    String asset;
}
