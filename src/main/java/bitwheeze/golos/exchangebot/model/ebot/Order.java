package bitwheeze.golos.exchangebot.model.ebot;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@ToString
public class Order {
    long id;
    BigDecimal amountToSell;
    String assetToSell;

    BigDecimal minToReceive;
    String assetToReceive;

    boolean fillOrKill;

    Instant expiration;
}
