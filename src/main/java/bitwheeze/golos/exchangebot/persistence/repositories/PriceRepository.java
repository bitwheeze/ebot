package bitwheeze.golos.exchangebot.persistence.repositories;

import bitwheeze.golos.exchangebot.persistence.entities.PriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRepository extends JpaRepository<PriceEntity, String> {
    //Optional<PriceEntity> findBySymbol(String symbol);
}
