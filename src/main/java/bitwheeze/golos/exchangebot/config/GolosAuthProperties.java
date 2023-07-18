package bitwheeze.golos.exchangebot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "golos.auth")
public class GolosAuthProperties {
    String account;
    String key;
}
