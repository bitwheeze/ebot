package bitwheeze.golos.exchangebot.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "ebot")
@Data
@ToString
public class EbotProperties {
    String cron = "0 * * * * *";
    List<TradingPair> pairs;
    int expiration = 15;
}
