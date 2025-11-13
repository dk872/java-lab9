package org.example;

public class Bank {

    public void transfer(Account from, Account to, long amount) {
        if (amount <= 0 || from.getId() == to.getId()) return;

        Account first = (from.getId() < to.getId()) ? from : to;
        Account second = (from.getId() < to.getId()) ? to : from;

        synchronized (first.getLock()) {
            synchronized (second.getLock()) {
                if (from.withdraw(amount)) {
                    to.deposit(amount);
                }
            }
        }
    }
}
