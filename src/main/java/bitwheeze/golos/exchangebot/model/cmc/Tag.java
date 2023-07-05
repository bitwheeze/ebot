package bitwheeze.golos.exchangebot.model.cmc;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class Tag {
    String slug;
    String name;
    String category;
}
