package bitwheeze.golos.exchangebot.events;

import bitwheeze.golos.goloslib.model.exception.NodeError;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class BlockchainErrorEvent extends EbotEvent {
    NodeError error;
}
