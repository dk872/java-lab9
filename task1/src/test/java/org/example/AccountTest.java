package org.example;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void testInitialBalance() {
        Account account = new Account(1000);
        assertEquals(1000, account.getBalance());
    }

    @Test
    void testUniqueIds() {
        Account acc1 = new Account(100);
        Account acc2 = new Account(100);
        assertNotEquals(acc1.getId(), acc2.getId());
    }

    @Test
    void testDepositPositiveAmount() {
        Account account = new Account(500);
        account.deposit(200);
        assertEquals(700, account.getBalance());
    }

    @Test
    void testDepositNegativeAmount() {
        Account account = new Account(500);
        account.deposit(-100);
        assertEquals(500, account.getBalance());
    }

    @Test
    void testDepositZero() {
        Account account = new Account(500);
        account.deposit(0);
        assertEquals(500, account.getBalance());
    }

    @Test
    void testWithdrawSuccess() {
        Account account = new Account(500);
        boolean result = account.withdraw(200);
        assertTrue(result);
        assertEquals(300, account.getBalance());
    }

    @Test
    void testWithdrawInsufficientFunds() {
        Account account = new Account(100);
        boolean result = account.withdraw(200);
        assertFalse(result);
        assertEquals(100, account.getBalance());
    }

    @Test
    void testWithdrawExactBalance() {
        Account account = new Account(100);
        boolean result = account.withdraw(100);
        assertTrue(result);
        assertEquals(0, account.getBalance());
    }

    @Test
    void testWithdrawNegativeAmount() {
        Account account = new Account(500);
        boolean result = account.withdraw(-50);
        assertFalse(result);
        assertEquals(500, account.getBalance());
    }

    @Test
    void testConcurrentAccessToAccount() throws InterruptedException {
        Account account = new Account(1000);
        int threads = 100;
        CountDownLatch latch = new CountDownLatch(threads);

        try (ExecutorService service = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                service.submit(() -> {
                    try {
                        account.deposit(10);
                        account.withdraw(5);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        assertEquals(1500, account.getBalance());
    }
}