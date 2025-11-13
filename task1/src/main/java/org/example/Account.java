package org.example;

import java.util.concurrent.atomic.AtomicLong;

public class Account {
    private static final AtomicLong idGenerator = new AtomicLong(0);

    private final long id;
    private final Object lock = new Object();
    private long balance;

    public Account(long initialBalance) {
        this.id = idGenerator.getAndIncrement();
        this.balance = initialBalance;
    }

    public long getId() {
        return id;
    }

    public long getBalance() {
        synchronized (lock) {
            return balance;
        }
    }

    public boolean withdraw(long amount) {
        if (amount <= 0) return false;
        synchronized (lock) {
            if (balance >= amount) {
                balance -= amount;
                return true;
            }
            return false;
        }
    }

    public void deposit(long amount) {
        if (amount <= 0) return;
        synchronized (lock) {
            balance += amount;
        }
    }

    Object getLock() {
        return lock;
    }

    @Override
    public String toString() {
        return "Account{id=" + id + ", balance=" + getBalance() + "}";
    }
}
