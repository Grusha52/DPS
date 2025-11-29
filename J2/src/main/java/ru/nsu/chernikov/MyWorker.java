package ru.nsu.chernikov;

public class MyWorker implements Runnable {
    private final int delayMs;
    private final MyLinkedList list;
    private long steps = 0;
    private long swaps = 0;
    private volatile boolean running = true;

    MyWorker(int delayms, MyLinkedList list) {
        this.delayMs = delayms;
        this.list = list;
    }

    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // Делаем только один шаг сортировки
                int swapsInThisStep = list.bubbleSortStep();
                steps++;
                swaps += swapsInThisStep;

                // Задержка между шагами
                Thread.sleep(delayMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                steps++;
            }
        }
    }

    public void stop() {
        running = false;
    }

    public long getSteps() {
        return steps;
    }

    public long getSwaps() {
        return swaps;
    }
}