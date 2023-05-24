package bitwheeze.golos.exchangebot.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
public class CmcPriceEntity {

    @Id
    long id;
    String symbol;
    String name;
    @Column(precision = 40, scale = 20)
    BigDecimal price;
    LocalDateTime lastUpdated;
}
