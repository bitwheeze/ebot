package bitwheeze.golos.exchangebot.model.cmc;

import lombok.Data;
import lombok.ToString;

import java.util.HashMap;

@Data
@ToString
public class CmcQuotesResponse {
    ResponseStatus status;
    HashMap<String, QuoteEntry> data;
}
