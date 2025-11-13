package org.example;

public class RingBuffer<T> {
    private Node<T> head;
    private Node<T> tail;

    public RingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }

        head = new Node<>(null);
        tail = head;

        Node<T> current = head;
        for (int i = 1; i < capacity; i++) {
            current.setNext(new Node<>(null));
            current = current.getNext();
        }
        current.setNext(head);
    }

    public synchronized void put(T item) throws InterruptedException {
        while (tail.getNext() == head) {
            wait();
        }

        tail.setValue(item);
        tail = tail.getNext();

        notifyAll();
    }

    public synchronized T take() throws InterruptedException {
        while (head == tail) {
            wait();
        }

        T value = head.getValue();
        head.setValue(null);
        head = head.getNext();

        notifyAll();
        return value;
    }
}
