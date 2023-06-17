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
    BigDecimal startOrderAmount;
    boolean maxOrderInPercent = false;
    BigDecimal orderPriceIncreasePercent = BigDecimal.valueOf(4.0);
    BigDecimal orderVolumeChangePercent = BigDecimal.valueOf(100.0);

    BigDecimal fixedPrice = BigDecimal.ZERO;

    public enum Mode {
        Sell, Buy, Balance;
    }

    public enum PriceSource {
        Cmc, Feed, Fixed;
    }
}
