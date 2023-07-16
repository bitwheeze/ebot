package bitwheeze.golos.exchangebot.services.helpers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BalancesTest {

    @Test
    void add() {
        Balances balances = new Balances();
        balances.add("GOLOS",  BigDecimal.valueOf(123.45));
        Assertions.assertEquals(BigDecimal.valueOf(123.45), balances.getBalance("GOLOS"));
        balances.add("GOLOS",  BigDecimal.valueOf(3.55));
        Assertions.assertTrue(BigDecimal.valueOf(127).compareTo(balances.getBalance("GOLOS")) == 0);
    }

    @Test
    void queryAmount() {
        Balances balances = new Balances();
        balances.add("GOLOS",  BigDecimal.valueOf(123.45));
        Assertions.assertEquals(BigDecimal.valueOf(123.45), balances.getBalance("GOLOS"));
        balances.add("GOLOS",  BigDecimal.valueOf(3.55));
        Assertions.assertTrue(BigDecimal.valueOf(127).compareTo(balances.getBalance("GOLOS")) == 0);

        //query some amount less then available
        Assertions.assertEquals(BigDecimal.valueOf(12.00), balances.queryAmount("GOLOS", BigDecimal.valueOf(12.00)));
        Assertions.assertTrue(BigDecimal.valueOf(115).compareTo(balances.getBalance("GOLOS")) == 0);

        //Check query more then available
        Assertions.assertTrue(BigDecimal.valueOf(115.00).compareTo(balances.queryAmount("GOLOS", BigDecimal.valueOf(120.00))) == 0);
        Assertions.assertTrue(BigDecimal.ZERO.compareTo(balances.getBalance("GOLOS")) == 0);

        //Check query by zero amount
        Assertions.assertTrue(BigDecimal.valueOf(0).compareTo(balances.queryAmount("GOLOS", BigDecimal.valueOf(120.00))) == 0);
        Assertions.assertTrue(BigDecimal.ZERO.compareTo(balances.getBalance("GOLOS")) == 0);

        //query non existing asset
        Assertions.assertTrue(BigDecimal.valueOf(0).compareTo(balances.queryAmount("GBG", BigDecimal.valueOf(120.00))) == 0);
        Assertions.assertTrue(BigDecimal.ZERO.compareTo(balances.getBalance("GBG")) == 0);

    }

    @Test
    void getBalance() {
        Balances balances = new Balances();
        Assertions.assertEquals(BigDecimal.ZERO, balances.getBalance("NOTEXISTING"));
        balances.add("GOLOS",  BigDecimal.valueOf(123.45));
        Assertions.assertEquals(BigDecimal.valueOf(123.45), balances.getBalance("GOLOS"));
    }
}