package bitwheeze.golos.exchangebot.services.helpers;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

@ToString
@Slf4j
public class Balances {
    private final HashMap<String, BigDecimal> balances = new HashMap<>();

    public void add(String asset, BigDecimal amount) {
        if(!balances.containsKey(asset)) {
            balances.put(asset, amount);
        } else {
            balances.put(asset, balances.get(asset).add(amount));
        }
    }

    public BigDecimal queryAmount(final String asset, final BigDecimal required) {
        var balance = getBalance(asset);
        var amount = required;
        if(BigDecimal.ZERO.compareTo(balance) >= 0 ) return BigDecimal.ZERO;

        var remaining = balance.subtract(amount);
        if(remaining.compareTo(BigDecimal.ZERO) < 0) {
            amount = amount.subtract(remaining.abs());
            remaining =  BigDecimal.ZERO;
        }
        log.info("balance of {}, current={}, required = {}, remaining = {}, available = {}", asset, balance, required, remaining, amount);
        balances.put(asset, remaining);
        return amount;
    }

    public BigDecimal getBalance(String asset) {
        var ret = balances.get(asset);
        if(ret == null) return BigDecimal.ZERO;
        return ret;
    }

    public List<String> getAssetList() {
        return balances.keySet().stream().sorted().toList();
    }
}
