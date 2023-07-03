package bitwheeze.golos.exchangebot.events.info;


import bitwheeze.golos.exchangebot.events.EbotEvent;
import bitwheeze.golos.goloslib.model.op.virtual.FillOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FillOrderEvent extends EbotEvent {
    FillOrder fillOrder;
}
