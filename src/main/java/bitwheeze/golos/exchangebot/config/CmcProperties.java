package bitwheeze.golos.exchangebot.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "cmc")
@ToString
public class CmcProperties {
    @ToString.Exclude
    String apiKey = "b54bcf4d-1bca-4e8e-9a24-22ff2c3d462c"; //test key
    String apiUrl = "https://sandbox-api.coinmarketcap.com/v1/cryptocurrency/listings/latest"; //sandbox host

    String cron = "0 */5 * * * *";

    int readCount = 5000;

    String baseAsset = "USD";

    List<String> slugs;

}
