package bitwheeze.golos.exchangebot.model.cmc;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

@Data
@ToString
@JsonDeserialize(using = JsonDeserializer.None.class)
public class ListingConvertData {
    //"id": 1,
    long id;
    //"name": "Bitcoin",
    String name;
    //"symbol": "BTC",
    String symbol;
    //"slug": "bitcoin",
    String slug;

    //"num_market_pairs": 10216,
    int num_market_pairs;
    //"date_added": "2010-07-13T00:00:00.000Z",
    Instant date_added;
    //    "tags": [
    //                "mineable",
    String [] tags;
    //"max_supply": 21000000,
    BigDecimal max_supply;
    //"circulating_supply": 19377306,
    BigDecimal circulating_supply;
    //"total_supply": 19377306,
    BigDecimal total_supply;
    //"infinite_supply": false,
    boolean infinite_supply;
    //"platform": null,
    @JsonDeserialize(using = ToStringDeserializer.class)
    String platform;
    //"cmc_rank": 1,
    int cmc_rank;
    //"self_reported_circulating_supply": null,
    String self_reported_circulating_supply;
    //"self_reported_market_cap": null,
    String self_reported_market_cap;
    //"tvl_ratio": null,
    //int tvl_ratio;
    //"last_updated": "2023-05-18T22:38:00.000Z",
    Instant last_updated;
    //"quote": {
        //"GLS": {
            //"price": 41795047.68734979,
    @JsonDeserialize(using = ToStringDeserializer.class)
    String quote;

}
