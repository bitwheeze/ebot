package bitwheeze.golos.exchangebot.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

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

    RelativeOrders relativeOrders;
}
