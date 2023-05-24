package bitwheeze.golos.exchangebot.model.cmc;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class CmcListingResponse {
    ResponseStatus status;
    ListingConvertData[] data;
}
