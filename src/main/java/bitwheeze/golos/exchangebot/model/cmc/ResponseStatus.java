package bitwheeze.golos.exchangebot.model.cmc;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ResponseStatus {
    String timestamp;
    int error_count;
    String error_message;
    int elapsed;
    int credit_count;
    String notice;
    int total_count;
}
