package bitwheeze.golos.exchangebot.events;

import bitwheeze.golos.exchangebot.model.cmc.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CmcErrorEvent {
    ResponseStatus status;
}
