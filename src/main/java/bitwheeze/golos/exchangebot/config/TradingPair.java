package bitwheeze.golos.exchangebot.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class TradingPair {

    String account;

    @ToString.Exclude
    String key;

    String base;
    String quote;

    BigDecimal reserve = BigDecimal.valueOf(10.00);

    RelativeOrders relativeOrders;

    /** Expiration in minutes */
    int expiration = 15;
}
