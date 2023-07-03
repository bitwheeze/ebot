package bitwheeze.golos.exchangebot.services;


import bitwheeze.golos.exchangebot.config.PricesProperties;
import bitwheeze.golos.exchangebot.events.error.CmcMissingPriceInfo;
import bitwheeze.golos.exchangebot.events.error.CmcOutdatedPriceInfo;
import bitwheeze.golos.exchangebot.events.info.ChangedPriceEvent;
import bitwheeze.golos.exchangebot.persistence.entities.PriceEntity;
import bitwheeze.golos.exchangebot.persistence.repositories.PriceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceService {
    private final PriceRepository priceRepo;
    private final ApplicationEventPublisher publisher;
    private final PricesProperties pricesProps;

    @Transactional
    public void updatePrice(String asset, BigDecimal price, LocalDateTime lastUpdated) {
        log.info("Store price of {} quote {} (lastUpdate={})", asset, price.setScale(6, RoundingMode.HALF_DOWN), lastUpdated);
        var priceEntity = priceRepo.findById(asset).orElseGet(() -> {
            var entity = new PriceEntity();
            entity.setAsset(asset);
            return entity;
        });
        if(priceEntity.getPrice() != null && !priceEntity.getPrice().setScale(6, RoundingMode.HALF_DOWN).equals(price.setScale(6, RoundingMode.HALF_DOWN))) {
            publisher.publishEvent(new ChangedPriceEvent(asset, pricesProps.getBaseAsset(), price));
        }
        priceEntity.setPrice(price);
        priceEntity.setLastUpdated(lastUpdated);
        priceRepo.save(priceEntity);
    }

    private Optional<PriceEntity> getPrice(final String asset) {
        var priceFromOpt = priceRepo.findById(asset);

        if(priceFromOpt.isEmpty()) {
            publisher.publishEvent(new CmcMissingPriceInfo(asset));
            return Optional.empty();
        }

        if (!validatePrice(priceFromOpt.get())) {
            log.warn("No price info for source symbol {}", asset);
            return Optional.empty();
        }
        return priceFromOpt;
    }

    private boolean validatePrice(PriceEntity priceFromOpt) {

        if(isExpired(priceFromOpt)) {
            return false;
        }

        return true;
    }

    private boolean isExpired(PriceEntity priceEntity) {
        var expired = priceEntity.getLastUpdated().isBefore(LocalDateTime.now(ZoneOffset.UTC).minus(pricesProps.getMaxAge(), ChronoUnit.MINUTES));
        if(expired) {
            publisher.publishEvent(new CmcOutdatedPriceInfo(priceEntity.getAsset(), priceEntity.getLastUpdated()));
        }
        return expired;
    }

    public Optional<BigDecimal> convert(BigDecimal amount, String assetFrom, String assetTo) {

        var priceFromOpt = getPrice(assetFrom);
        if(priceFromOpt.isEmpty()) return Optional.empty();

        if (assetTo.equals(pricesProps.getBaseAsset())) {
            return Optional.of(priceFromOpt.get().getPrice());
        }

        var priceToOpt = getPrice(assetTo);
        if (priceToOpt.isEmpty()) {
            return Optional.empty();
        }

        var from = priceFromOpt.get().getPrice();
        var to = priceToOpt.get().getPrice();

        if (to.equals(BigDecimal.ZERO)) {
            log.warn("target symbol {} has price equal zero", assetTo);
            return Optional.empty();
        }

        return Optional.of(amount.multiply(from).divide(to, RoundingMode.HALF_DOWN));
    }
}
