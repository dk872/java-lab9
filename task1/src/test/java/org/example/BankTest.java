package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class BankTest {

    private Bank bank;

    @BeforeEach
    void setUp() {
        bank = new Bank();
    }

    @Test
    void testSuccessfulTransfer() {
        Account from = new Account(1000);
        Account to = new Account(500);

        bank.transfer(from, to, 200);

        assertEquals(800, from.getBalance());
        assertEquals(700, to.getBalance());
    }

    @Test
    void testTransferInsufficientFunds() {
        Account from = new Account(100);
        Account to = new Account(500);

        bank.transfer(from, to, 200);

        assertEquals(100, from.getBalance());
        assertEquals(500, to.getBalance());
    }

    @Test
    void testTransferNegativeAmount() {
        Account from = new Account(1000);
        Account to = new Account(500);

        bank.transfer(from, to, -100);

        assertEquals(1000, from.getBalance());
        assertEquals(500, to.getBalance());
    }

    @Test
    void testTransferToSelf() {
        Account account = new Account(1000);
        bank.transfer(account, account, 100);

        assertEquals(1000, account.getBalance());
    }

    @Test
    void testTransferZero() {
        Account from = new Account(1000);
        Account to = new Account(500);

        bank.transfer(from, to, 0);

        assertEquals(1000, from.getBalance());
        assertEquals(500, to.getBalance());
    }

    @Test
    void testDeadlockSafety() throws InterruptedException {
        final int ITERATIONS = 10_000;
        final Account acc1 = new Account(100_000);
        final Account acc2 = new Account(100_000);

        CountDownLatch latch = new CountDownLatch(2);

        try (ExecutorService service = Executors.newFixedThreadPool(2)) {
            service.submit(() -> {
                try {
                    for (int i = 0; i < ITERATIONS; i++) {
                        bank.transfer(acc1, acc2, 1);
                    }
                } finally {
                    latch.countDown();
                }
            });

            service.submit(() -> {
                try {
                    for (int i = 0; i < ITERATIONS; i++) {
                        bank.transfer(acc2, acc1, 1);
                    }
                } finally {
                    latch.countDown();
                }
            });

            boolean finished = latch.await(5, TimeUnit.SECONDS);
            assertTrue(finished);
        }

        assertEquals(200_000, acc1.getBalance() + acc2.getBalance());
    }

    @Test
    void testConcurrentRandomTransfers() throws InterruptedException {
        int numAccounts = 10;
        int numThreads = 20;
        int numTransfers = 1000;

        Account[] accounts = new Account[numAccounts];
        long initialTotal = 0;

        for (int i = 0; i < numAccounts; i++) {
            accounts[i] = new Account(1000);
            initialTotal += accounts[i].getBalance();
        }

        CountDownLatch latch = new CountDownLatch(numThreads);

        try (ExecutorService service = Executors.newFixedThreadPool(numThreads)) {
            for (int i = 0; i < numThreads; i++) {
                service.submit(() -> {
                    try {
                        for (int j = 0; j < numTransfers; j++) {
                            int fromIdx = (int) (Math.random() * numAccounts);
                            int toIdx = (int) (Math.random() * numAccounts);
                            bank.transfer(accounts[fromIdx], accounts[toIdx], 5);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean finished = latch.await(10, TimeUnit.SECONDS);
            assertTrue(finished, "Test timed out - possible deadlock or performance issue.");
        }

        long finalTotal = 0;
        for (Account acc : accounts) {
            finalTotal += acc.getBalance();
        }

        assertEquals(initialTotal, finalTotal);
    }
}