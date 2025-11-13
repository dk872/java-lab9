package org.example;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RingBufferDemo {

    private static final Logger LOGGER = Logger.getLogger(RingBufferDemo.class.getName());

    public static void main(String[] args) {
        RingBuffer<String> buffer1 = new RingBuffer<>(10);
        RingBuffer<String> buffer2 = new RingBuffer<>(10);

        for (int i = 1; i <= 5; i++) {
            startProducer(i, buffer1);
        }

        for (int i = 1; i <= 2; i++) {
            startTranslator(i, buffer1, buffer2);
        }

        System.out.println("Main: Starting to read data...");

        try {
            for (int i = 1; i <= 100; i++) {
                String finalMsg = buffer2.take();
                System.out.println("MAIN (" + i + "/100): " + finalMsg);
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Main thread interrupted", e);
            Thread.currentThread().interrupt();
        }

        System.out.println("Main: Work finished. Daemons are stopping.");
    }

    private static void startProducer(int id, RingBuffer<String> buffer) {
        Thread producer = new Thread(() -> {
            try {
                while (true) {
                    String msg = "Thread #" + id + " generated message " + System.nanoTime();
                    buffer.put(msg);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        producer.setDaemon(true);
        producer.start();
    }

    private static void startTranslator(int id, RingBuffer<String> source, RingBuffer<String> dest) {
        Thread translator = new Thread(() -> {
            try {
                while (true) {
                    String received = source.take();
                    String newMsg = "Thread #" + id + " translated message [" + received + "]";
                    dest.put(newMsg);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        translator.setDaemon(true);
        translator.start();
    }
}
