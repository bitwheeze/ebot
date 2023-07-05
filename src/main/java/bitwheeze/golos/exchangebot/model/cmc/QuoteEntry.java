package bitwheeze.golos.exchangebot.model.cmc;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@ToString
public class QuoteEntry {
    long id;
    String name;
    String symbol;
    String slug;
    int numMarketPairs;
    Instant dateAdded;
    Tag[] tags;
    BigDecimal maxSupply;
    BigDecimal circulatingSupply;
    BigDecimal totalSupply;
    int isActive;
    boolean infiniteSupply;
    int cmcRank;
    int isFiat;
    Map<String, Quote> quote;
}
