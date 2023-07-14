package bitwheeze.golos.exchangebot.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ebot.prices")
@Data
@ToString
public class PricesProperties {
    String baseAsset = "USD";
    int maxAge = 1440;
}
