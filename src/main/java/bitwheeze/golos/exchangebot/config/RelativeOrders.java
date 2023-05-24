package bitwheeze.golos.exchangebot.config;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public class RelativeOrders {
    boolean enabled;

    Mode mode;

    BigDecimal spread;
    int ordersCount;
    BigDecimal maxOrder;
    boolean maxOrderInPercent = false;

    public static enum Mode {
        Sell, Buy, Balance;
    }
}
