import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSpider {
    private final String baseUrl;
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final Queue<String> queue = new ConcurrentLinkedQueue<>();
    private final List<String> messages = Collections.synchronizedList(new ArrayList<>());
    private final HttpClient client;
    private final AtomicInteger activeThreads = new AtomicInteger(0);

    public WebSpider(String port) {
        this.baseUrl = "http://localhost:" + port;
        this.client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    public List<String> crawl() throws InterruptedException {
        queue.add("/");
        visited.add("/");

        List<Thread> threads = new ArrayList<>();
        int maxConcurrentThreads = 50;

        while (!queue.isEmpty() || activeThreads.get() > 0) {
            while (activeThreads.get() < maxConcurrentThreads && !queue.isEmpty()) {
                String path = queue.poll();
                if (path == null) continue;

                Thread thread = Thread.startVirtualThread(() -> {
                    activeThreads.incrementAndGet();
                    try {
                        processPath(path);
                    } finally {
                        activeThreads.decrementAndGet();
                    }
                });

                threads.add(thread);
            }

            Thread.sleep(10);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        Collections.sort(messages);
        return messages;
    }

    private void processPath(String path) {
        try {
            // Исправлено: убеждаемся, что путь начинается с "/"
            String fullPath = path.startsWith("/") ? path : "/" + path;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + fullPath))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                String body = response.body();
                String message = extractMessage(body);
                List<String> successors = extractSuccessors(body);

                synchronized (messages) {
                    messages.add(message);
                }

                // Исправлено: добавляем новые пути с правильным форматом
                for (String successor : successors) {
                    // Убеждаемся, что путь начинается с "/"
                    String cleanSuccessor = successor.startsWith("/") ? successor : "/" + successor;
                    if (visited.add(cleanSuccessor)) {
                        queue.add(cleanSuccessor);
                    }
                }

                System.out.println("Обработан путь: " + fullPath + ", сообщение: " + message);
            } else {
                System.err.println("HTTP " + response.statusCode() + " для пути: " + fullPath);
            }
        } catch (java.net.ConnectException e) {
            System.err.println("Не удалось подключиться к серверу: " + e.getMessage());
            System.err.println("Убедитесь, что сервер запущен на " + baseUrl);
        } catch (java.net.http.HttpTimeoutException e) {
            System.err.println("Таймаут для пути: " + path);
        } catch (Exception e) {
            System.err.println("Ошибка при обработке " + path + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String extractMessage(String json) {
        try {
            int start = json.indexOf("\"message\":\"") + 11;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) {
            System.err.println("Ошибка парсинга message из: " + json);
            return "ERROR_PARSING_MESSAGE";
        }
    }

    private List<String> extractSuccessors(String json) {
        List<String> successors = new ArrayList<>();
        try {
            int start = json.indexOf("\"successors\":[") + 14;
            int end = json.indexOf("]", start);
            String arrayContent = json.substring(start, end);

            String[] parts = arrayContent.split(",");
            for (String part : parts) {
                String cleaned = part.trim().replace("\"", "");
                if (!cleaned.isEmpty()) {
                    successors.add(cleaned);
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка парсинга successors из: " + json);
        }
        return successors;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Использование: java WebSpider <port>");
            System.err.println("Пример: java WebSpider 8080");
            System.exit(1);
        }

        String port = args[0];
        System.out.println("Запуск паука для сервера на порту: " + port);

        try {
            WebSpider spider = new WebSpider(port);
            List<String> result = spider.crawl();

            System.out.println("\n=== РЕЗУЛЬТАТЫ (" + result.size() + " сообщений) ===");
            for (String message : result) {
                System.out.println(message);
            }

        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}