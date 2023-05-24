package bitwheeze.golos.exchangebot;

import bitwheeze.golos.exchangebot.config.EbotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@RequiredArgsConstructor
@SpringBootApplication
public class ExchangebotApplication {

    private final EbotProperties ebotProps;

    public static void main(String[] args) {
        SpringApplication.run(ExchangebotApplication.class, args);
    }

    @Bean
    public ApplicationRunner testRunner() {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) throws Exception {
                log.info("ebotProps = {}", ebotProps);
            }
        };
    }
}
