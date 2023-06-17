package bitwheeze.golos.exchangebot.events.error;

import bitwheeze.golos.exchangebot.events.EbotEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@ToString
@AllArgsConstructor
public class CmcOutdatedPriceInfo extends EbotEvent {
    String asset;
    LocalDateTime lastUpdated;
}
