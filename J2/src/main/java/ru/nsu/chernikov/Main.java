package ru.nsu.chernikov;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java Main <numThreads> <delayMs>");
            return;
        }

        MyLinkedList list = new MyLinkedList();
        int numThreads = Integer.parseInt(args[0]);
        int delayMs = Integer.parseInt(args[1]);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        MyWorker[] workers = new MyWorker[numThreads];

        System.out.println("Запуск " + numThreads + " потоков с задержкой " + delayMs + "мс");

        // Запускаем воркеры
        for (int i = 0; i < numThreads; i++) {
            workers[i] = new MyWorker(delayMs, list);
            executor.submit(workers[i]);
        }

        // Чтение ввода
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                System.out.print("> ");
                String line = reader.readLine();

                if (line == null) {
                    System.out.println("Конец ввода, завершаем...");
                    break;
                } else if (line.isEmpty()) {
                    // Вывод текущего состояния списка
                    System.out.println("= Текущее состояние списка =");
                    int i = 1;
                    for (String item : list) {
                        System.out.println((i++) + ": " + item);
                    }
                    System.out.println("Всего элементов: " + list.size());
                    System.out.println("============================");
                } else {
                    // Добавление строки
                    if (line.length() > 80) {
                        // Разрезаем длинные строки
                        for (int i = 0; i < line.length(); i += 80) {
                            int end = Math.min(i + 80, line.length());
                            String part = line.substring(i, end);
                            list.addFirst(part);
                            System.out.println("Добавлена часть: '" + part + "'");
                        }
                    } else {
                        list.addFirst(line);
                        System.out.println("Добавлена строка: '" + line + "'");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

        // Завершаем работу
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                System.out.println("Потоки не завершились вовремя!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Выводим статистику по шагам
        System.out.println("\n=== Статистика ===");
        long totalSteps = 0;
        long totalSwaps = 0;
        for (int i = 0; i < workers.length; i++) {
            totalSteps += workers[i].getSteps();
            totalSwaps += workers[i].getSwaps();
            System.out.println("Поток " + i + ": шагов=" + workers[i].getSteps() +
                    ", перестановок=" + workers[i].getSwaps());
        }
        System.out.println("Всего шагов: " + totalSteps);
        System.out.println("Всего перестановок: " + totalSwaps);
    }
}