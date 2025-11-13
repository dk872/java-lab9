package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.*;

class RingBufferTest {
    @Test
    void testConstructorInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new RingBuffer<>(0));
        assertThrows(IllegalArgumentException.class, () -> new RingBuffer<>(-5));
    }

    @Test
    void testBasicPutAndTake() throws InterruptedException {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        buffer.put("Test");
        String result = buffer.take();

        assertEquals("Test", result);
    }

    @Test
    void testFifoOrder() throws InterruptedException {
        RingBuffer<Integer> buffer = new RingBuffer<>(10);

        buffer.put(1);
        buffer.put(2);
        buffer.put(3);

        assertEquals(1, buffer.take());
        assertEquals(2, buffer.take());
        assertEquals(3, buffer.take());
    }

    @Test
    void testCircularBehavior() throws InterruptedException {
        RingBuffer<Integer> buffer = new RingBuffer<>(4);

        for (int i = 0; i < 10; i++) {
            buffer.put(i);
            assertEquals(i, buffer.take());
        }
    }

    @Test
    @Timeout(2)
    void testBlocksOnEmpty() {
        RingBuffer<String> buffer = new RingBuffer<>(5);

        Thread consumer = new Thread(() -> {
            try {
                buffer.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        consumer.start();

        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        assertEquals(Thread.State.WAITING, consumer.getState(),
                "Consumer має чекати (WAITING), бо буфер порожній");

        consumer.interrupt();
    }

    @Test
    @Timeout(2)
    void testBlocksOnFull() throws InterruptedException {
        RingBuffer<Integer> buffer = new RingBuffer<>(3);

        buffer.put(1);
        buffer.put(2);

        Thread producer = new Thread(() -> {
            try {
                buffer.put(3);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();

        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        assertEquals(Thread.State.WAITING, producer.getState());

        producer.interrupt();
    }

    @Test
    void testProducerConsumerInteraction() throws InterruptedException {
        RingBuffer<Integer> buffer = new RingBuffer<>(5);
        CountDownLatch latch = new CountDownLatch(1);

        new Thread(() -> {
            try {
                int val = buffer.take();
                if (val == 42) {
                    latch.countDown();
                }
            } catch (InterruptedException e) {
                fail("Consumer thread interrupted unexpectedly", e);
            }
        }).start();

        Thread.sleep(50);

        buffer.put(42);

        boolean success = latch.await(1, TimeUnit.SECONDS);
        assertTrue(success);
    }

    @Test
    void testHeavyConcurrencyStressTest() throws InterruptedException {
        final int CAPACITY = 100;
        final int THREAD_COUNT = 20;
        final int ITEMS_PER_THREAD = 1000;

        RingBuffer<Integer> buffer = new RingBuffer<>(CAPACITY);

        AtomicInteger totalConsumed = new AtomicInteger(0);
        AtomicInteger totalProduced = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT * 2);

        try (ExecutorService service = Executors.newFixedThreadPool(THREAD_COUNT * 2)) {

            for (int i = 0; i < THREAD_COUNT; i++) {
                service.submit(() -> {
                    try {
                        for (int j = 0; j < ITEMS_PER_THREAD; j++) {
                            buffer.put(1);
                            totalProduced.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            for (int i = 0; i < THREAD_COUNT; i++) {
                service.submit(() -> {
                    try {
                        for (int j = 0; j < ITEMS_PER_THREAD; j++) {
                            int val = buffer.take();
                            totalConsumed.addAndGet(val);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean finished = latch.await(10, TimeUnit.SECONDS);
            assertTrue(finished);
        }

        assertEquals(totalProduced.get(), totalConsumed.get());
        assertEquals(THREAD_COUNT * ITEMS_PER_THREAD, totalConsumed.get());
    }
}