package bitwheeze.golos.exchangebot.services;

import bitwheeze.golos.exchangebot.config.CmcProperties;
import bitwheeze.golos.exchangebot.events.error.CmcErrorEvent;
import bitwheeze.golos.exchangebot.events.info.CmcLoadEvent;
import bitwheeze.golos.exchangebot.model.cmc.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CmcService {

    private final CmcProperties cmcProps;

    private final ApplicationEventPublisher publisher;
    private final Environment env;

    private final ObjectMapper mapper;

    private final WebClient webClient;

    private final PriceService priceService;

    public CmcService(CmcProperties cmcProps, ApplicationEventPublisher publisher, @Qualifier("cmcObjectMapper") ObjectMapper mapper, @Qualifier("cmcClient") WebClient webClient, Environment env, PriceService priceService) {
        this.cmcProps = cmcProps;
        this.publisher = publisher;
        this.mapper = mapper;
        this.webClient = webClient;
        this.env = env;
        this.priceService = priceService;
    }

    public void queryListingConvert() {
        log.info("Query CMC Converter ! {}", cmcProps);
        String slugs = null;
        if(cmcProps.getSlugs() != null) {
            slugs = cmcProps.getSlugs().stream().collect(Collectors.joining(","));
        }

        log.info("slugs = {}", slugs);

        try {
            long lastStart = 1;
                log.info("get proces starting at {} for quote coin {}", lastStart, cmcProps.getBaseAsset());
                final String url = getSlowServiceUri(lastStart, slugs);
                log.info("url = {}", url);
                var query = webClient
                        .get()
                        .uri(url)
                        .header("X-CMC_PRO_API_KEY", cmcProps.getApiKey())
                        .accept(MediaType.APPLICATION_JSON)
                        .attribute("start", String.valueOf(lastStart))
                        .attribute("limit", String.valueOf(cmcProps.getReadCount()))
                        .attribute("convert", cmcProps.getBaseAsset())
                        .attribute("slug", slugs);

                Mono<CmcQuotesResponse> cmcResponseMono =  query
                        .retrieve()
                        .onStatus(status -> !status.is2xxSuccessful(), resp -> {
                            log.error("error retrieving prices from cmc {}", resp.statusCode());
                            return resp.createException();
                        })
                        .bodyToMono(CmcQuotesResponse.class);

                processRepsponse(cmcResponseMono.block(), lastStart);
                publisher.publishEvent(new CmcLoadEvent());
        }catch (Exception ex) {
              publisher.publishEvent(new CmcErrorEvent(null));
        }

    }


    @Scheduled(cron = "#{@cmcProperties.cron}")
    @Transactional
    public void cronJob() {
        this.queryListingConvert();
    }

    @PostConstruct
    public void init() {
        this.queryListingConvert();
    }

    private String getSlowServiceUri(long start, String slugs) {
        String url = cmcProps.getApiUrl();
        url += "?" + "convert=" + cmcProps.getBaseAsset();
        if(slugs != null && !slugs.isBlank()) {
            url += "&slug=" + slugs;
        }
        return url;
    }


    private void processRepsponse(CmcQuotesResponse response, long lastStart) {
        log.info("got response from cmc", response);
        if (response.getStatus().getError_count() > 0) {
            log.error("Error reading cmc prices {}", response.getStatus());
            publisher.publishEvent(new CmcErrorEvent(response.getStatus()));
        }
        for (var data : response.getData().values()) {
            processData(data);
        }

    }

    private void processData(QuoteEntry data) {
        Quote quote = extractQuote(data);
        log.debug("price for {} ({}) is {}", data.getSymbol(), data.getName(), quote.getPrice());

        priceService.updatePrice(mapAsset(data.getSymbol()), quote.getPrice(), LocalDateTime.ofInstant(quote.getLast_updated(), ZoneOffset.UTC));
    }

    @SneakyThrows
    private Quote extractQuote(QuoteEntry data) {
        return data.getQuote().get(cmcProps.getBaseAsset());
    }

    private String mapAsset(String asset) {
        String mappedAsset = env.getProperty("cmc.map." + asset);
        if(null == mappedAsset) {
            return asset;
        }
        return mappedAsset;
    }
}
