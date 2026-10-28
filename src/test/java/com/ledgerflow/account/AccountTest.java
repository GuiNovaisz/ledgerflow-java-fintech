package com.ledgerflow.account;

import com.ledgerflow.account.domain.Account;
import com.ledgerflow.shared.BusinessException;
import com.ledgerflow.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class AccountTest {
    @Test void decimalArithmeticIsExactAndOverdraftDoesNotChangeBalance() {
        Account a = new Account(UUID.randomUUID(), "Main");
        a.credit(new BigDecimal("0.10")); a.credit(new BigDecimal("0.20"));
        assertThat(a.getBalance()).isEqualByComparingTo("0.30");
        assertThatThrownBy(() -> a.debit(new BigDecimal("0.31"))).isInstanceOf(BusinessException.class);
        assertThat(a.getBalance()).isEqualByComparingTo("0.30");
    }
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "0.001", "1.000", "1000000000000000.00"})
    void rejectsInvalidMoney(String value) {
        assertThatThrownBy(() -> Money.positive(new BigDecimal(value))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void balanceOverflowAndInactiveAccountsCannotBeCredited() {
        Account a = new Account(UUID.randomUUID(), "Main"); a.credit(Money.MAX);
        assertThatThrownBy(() -> a.credit(new BigDecimal("0.01"))).isInstanceOf(BusinessException.class);
        a.changeStatus(Account.Status.INACTIVE);
        assertThatThrownBy(() -> a.debit(new BigDecimal("1"))).isInstanceOf(BusinessException.class);
        assertThat(a.getBalance()).isEqualByComparingTo(Money.MAX);
    }
}
