package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class BankTestRunner {

    public static final int NUM_ACCOUNTS = 100;
    public static final int MAX_INITIAL_BALANCE = 10_000;
    public static final int NUM_TRANSACTIONS = 100_000;
    public static final int MAX_TRANSFER_AMOUNT = 50;
    public static final int NUM_THREADS = 50;

    public static void main(String[] args) throws InterruptedException {
        Bank bank = new Bank();
        List<Account> accounts = createAccounts();

        long initialTotal = totalBalance(accounts);
        System.out.println("Initial bank balance: " + initialTotal);

        executeTransfers(bank, accounts);

        long finalTotal = totalBalance(accounts);
        System.out.println("Final bank balance:   " + finalTotal);

        if (initialTotal == finalTotal) {
            System.out.println("\nSUCCESS: Balance remained unchanged.");
        } else {
            System.out.println("\nERROR: Balance mismatch!");
        }
    }

    private static List<Account> createAccounts() {
        Random random = new Random();
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < NUM_ACCOUNTS; i++) {
            accounts.add(new Account(random.nextInt(MAX_INITIAL_BALANCE)));
        }
        return accounts;
    }

    private static void executeTransfers(Bank bank, List<Account> accounts) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(NUM_TRANSACTIONS);

        System.out.println("Starting " + NUM_TRANSACTIONS + " transactions...");

        try (ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS)) {
            for (int i = 0; i < NUM_TRANSACTIONS; i++) {
                pool.submit(() -> {
                    try {
                        ThreadLocalRandom rnd = ThreadLocalRandom.current();
                        Account from = accounts.get(rnd.nextInt(NUM_ACCOUNTS));
                        Account to = accounts.get(rnd.nextInt(NUM_ACCOUNTS));
                        long amount = rnd.nextLong(1, MAX_TRANSFER_AMOUNT + 1);
                        bank.transfer(from, to, amount);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
        }

        System.out.println("All transactions finished.");
    }

    private static long totalBalance(List<Account> accounts) {
        long sum = 0;
        for (Account acc : accounts) {
            sum += acc.getBalance();
        }
        return sum;
    }
}
