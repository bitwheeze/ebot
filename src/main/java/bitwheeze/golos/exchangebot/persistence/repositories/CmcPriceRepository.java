package bitwheeze.golos.exchangebot.persistence.repositories;

import bitwheeze.golos.exchangebot.persistence.entities.CmcPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CmcPriceRepository extends JpaRepository<CmcPriceEntity, Long> {
    Optional<CmcPriceEntity> findBySymbol(String symbol);
}
