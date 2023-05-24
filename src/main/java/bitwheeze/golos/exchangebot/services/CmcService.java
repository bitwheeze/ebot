package bitwheeze.golos.exchangebot.services;

import bitwheeze.golos.exchangebot.config.CmcProperties;
import bitwheeze.golos.exchangebot.events.CmcErrorEvent;
import bitwheeze.golos.exchangebot.model.cmc.CmcListingResponse;
import bitwheeze.golos.exchangebot.model.cmc.ListingConvertData;
import bitwheeze.golos.exchangebot.model.cmc.Quote;
import bitwheeze.golos.exchangebot.persistence.entities.CmcPriceEntity;
import bitwheeze.golos.exchangebot.persistence.repositories.CmcPriceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@Slf4j
public class CmcService {

    private final CmcProperties cmcProps;
    private final CmcPriceRepository priceRepo;
    private final ApplicationEventPublisher publisher;

    private final ObjectMapper mapper;

    private final WebClient webClient;

    public CmcService(CmcProperties cmcProps, CmcPriceRepository priceRepo, ApplicationEventPublisher publisher, @Qualifier("cmcObjectMapper") ObjectMapper mapper, @Qualifier("cmcClient") WebClient webClient) {
        this.cmcProps = cmcProps;
        this.priceRepo = priceRepo;
        this.publisher = publisher;
        this.mapper = mapper;
        this.webClient = webClient;
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

        var priceEntity = priceRepo.findById(data.getId()).orElseGet(() -> {
            var entity = new CmcPriceEntity();
            entity.setId(data.getId());
            entity.setName(data.getName());
            entity.setSymbol(data.getSymbol());
            return entity;
        });

        priceEntity.setPrice(quote.getPrice());
        priceEntity.setLastUpdated(LocalDateTime.ofInstant(quote.getLast_updated(), ZoneOffset.UTC));
        priceRepo.save(priceEntity);
    }

    @SneakyThrows
    private Quote extractQuote(ListingConvertData data) {
        var object = mapper.readTree(data.getQuote());
        var assetQuote = object.get(cmcProps.getBaseAsset());
        return mapper.treeToValue(assetQuote, Quote.class);
    }

    /*
    @Scheduled(fixedDelay = 2000)
    public void testConvert() {
        var amount = BigDecimal.valueOf(123456.13);
        var toAmount = this.convert(amount, "GLS", "BNB");
        log.info("convert {} {} to {} = {}", amount, "GLS", "BNB", toAmount.isEmpty() ? "no price data abvailable" : toAmount.get().toString());
    }
    */


    public Optional<BigDecimal> convert(BigDecimal amount, String symbolFrom, String symbolTo) {
        var priceFromOpt = priceRepo.findBySymbol(symbolFrom);
        if (priceFromOpt.isEmpty()) {
            log.warn("No price info for source symbol {}", symbolFrom);
            return Optional.empty();
        }
        if (symbolTo.equals(cmcProps.getBaseAsset())) {
            return Optional.of(priceFromOpt.get().getPrice());
        }
        var priceToOpt = priceRepo.findBySymbol(symbolTo);
        if (priceToOpt.isEmpty()) {
            log.warn("No price info for target symbol {}", symbolTo);
            return Optional.empty();
        }

        var from = priceFromOpt.get().getPrice();
        var to = priceToOpt.get().getPrice();

        if (to.equals(BigDecimal.ZERO)) {
            log.warn("target symbol {} has price equal zero", symbolTo);
            return Optional.empty();
        }

        return Optional.of(amount.multiply(from).divide(to, RoundingMode.HALF_DOWN));
    }
}
