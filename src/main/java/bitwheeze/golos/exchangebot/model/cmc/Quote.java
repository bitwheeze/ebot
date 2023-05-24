package bitwheeze.golos.exchangebot.model.cmc;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@ToString
public class Quote {
    //"price": 720.1089043133962,
    BigDecimal price;
    //"volume_24h": 2098912685080.8955,
    BigDecimal volume_24h;
    //"volume_change_24h": -15.6338,
    BigDecimal volume_change_24h;
    //"percent_change_1h": -0.24406968,
    BigDecimal percent_change_1h;
    //"percent_change_24h": 3.7132916,
    BigDecimal percent_change_24h;
    //"percent_change_7d": 9.92370595,
    BigDecimal percent_change_7d;
    //"percent_change_30d": -11.5768376,
    BigDecimal percent_change_30d;
    //"percent_change_60d": 17.64114709,
    BigDecimal percent_change_60d;
    //"percent_change_90d": 17.45163821,
    BigDecimal percent_change_90d;
    //"market_cap": 37328876125850.055,
    BigDecimal market_cap;
    //"market_cap_dominance": 2.1313,
    BigDecimal market_cap_dominance;
    //"fully_diluted_market_cap": 72010890431335.39,
    BigDecimal fully_diluted_market_cap;
    //"tvl": null,
    @JsonDeserialize(using = ToStringDeserializer.class)
    String tvl;
    //"last_updated": "2023-05-18T22:39:00.000Z"
    Instant last_updated;
}
