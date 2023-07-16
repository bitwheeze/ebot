package bitwheeze.golos.exchangebot.services.helpers;

import lombok.ToString;

import java.math.BigDecimal;
import java.util.HashMap;

@ToString
public class Balances {
    private final HashMap<String, BigDecimal> balances = new HashMap<>();

    public void add(String asset, BigDecimal amount) {
        if(!balances.containsKey(asset)) {
            balances.put(asset, amount);
        } else {
            balances.put(asset, balances.get(asset).add(amount));
        }
    }

    public BigDecimal queryAmount(String asset, BigDecimal amount) {
        var balance = getBalance(asset);
        if(BigDecimal.ZERO.compareTo(balance) >= 0 ) return BigDecimal.ZERO;

        var remaining = balance.subtract(amount);
        if(remaining.compareTo(BigDecimal.ZERO) < 0) {
            amount = amount.subtract(remaining.abs());
            remaining =  BigDecimal.ZERO;
        }
        balances.put(asset, remaining);
        return amount;
    }

    public BigDecimal getBalance(String asset) {
        var ret = balances.get(asset);
        if(ret == null) return BigDecimal.ZERO;
        return ret;
    }
}
