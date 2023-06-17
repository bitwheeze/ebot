package bitwheeze.golos.exchangebot.services;

import bitwheeze.golos.exchangebot.config.CmcProperties;
import bitwheeze.golos.exchangebot.events.error.CmcErrorEvent;
import bitwheeze.golos.exchangebot.model.cmc.CmcListingResponse;
import bitwheeze.golos.exchangebot.model.cmc.ListingConvertData;
import bitwheeze.golos.exchangebot.model.cmc.Quote;
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
        log.info("Starting NON-BLOCKING Controller!");

        long lastStart = 1;
        while (lastStart > 0) {
            log.info("get proces starting at {} for base coin {}", lastStart, cmcProps.getBaseAsset());
            final String url = getSlowServiceUri(lastStart);
            log.info("url = {}", url);
            Mono<CmcListingResponse> cmcResponseMono = webClient
                    .get()
                    .uri(url)
                    .header("X-CMC_PRO_API_KEY", cmcProps.getApiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .attribute("start", String.valueOf(lastStart))
                    .attribute("limit", String.valueOf(cmcProps.getReadCount()))
                    .attribute("convert", cmcProps.getBaseAsset())
                    .retrieve()
                    .bodyToMono(CmcListingResponse.class);

            lastStart = processRepsponse(cmcResponseMono.block(), lastStart);
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

    private String getSlowServiceUri(long start) {
        String url = cmcProps.getApiUrl();
        url += "?" + "start=" + start + "&limit=" + cmcProps.getReadCount() + "&convert=" + cmcProps.getBaseAsset();
        return url;
    }


    private long processRepsponse(CmcListingResponse response, long lastStart) {
        if (response.getStatus().getError_count() > 0) {
            log.error("Error reading cmc prices {}", response.getStatus());
            publisher.publishEvent(new CmcErrorEvent(response.getStatus()));
            return -1;
        }
        int count = response.getData().length;
        log.info("got next {} entries from cmc", count);
        for (var data : response.getData()) {
            processData(data);
        }

        return count < cmcProps.getReadCount() ? -1 : lastStart + count;
    }

    private void processData(ListingConvertData data) {
        Quote quote = extractQuote(data);
        log.debug("price for {} ({}) is {}", data.getSymbol(), data.getName(), quote.getPrice());

        priceService.updatePrice(mapAsset(data.getSymbol()), quote.getPrice(), LocalDateTime.ofInstant(quote.getLast_updated(), ZoneOffset.UTC));
    }

    @SneakyThrows
    private Quote extractQuote(ListingConvertData data) {
        var object = mapper.readTree(data.getQuote());
        var assetQuote = object.get(cmcProps.getBaseAsset());
        return mapper.treeToValue(assetQuote, Quote.class);
    }

    private String mapAsset(String asset) {
        String mappedAsset = env.getProperty("cmc.map." + asset);
        if(null == mappedAsset) {
            return asset;
        }
        return mappedAsset;
    }
}
