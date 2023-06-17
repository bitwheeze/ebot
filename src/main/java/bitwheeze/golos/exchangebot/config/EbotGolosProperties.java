package bitwheeze.golos.exchangebot.config;


import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ebot.golos")
@Data
@ToString
public class EbotGolosProperties {
    String feedCron = "0 */12 * * * *";
}
