package bitwheeze.golos.exchangebot.events.error;

import bitwheeze.golos.exchangebot.events.EbotEvent;
import bitwheeze.golos.exchangebot.model.cmc.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CmcErrorEvent extends EbotEvent {
    ResponseStatus status;
}
