package ru.nsu.chernikov;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Client {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java ru.nsu.chernikov.Client <serverHost> <port> <name> <outPrefix> [--delay seconds] [--exit-before-read]");
            System.out.println("Example: java ru.nsu.chernikov.Client localhost 9999 alice out/alice 5");
            System.exit(1);
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String name = args[2];
        String outPrefix = args[3];

        // delay = 0 если 5-го аргумента нет
        int delay = (args.length >= 5) ? Integer.parseInt(args[4]) : 0;
        // exitBeforeRead = true если указан 6-й аргумент
        boolean exitBeforeRead = (args.length >= 6) && args[5].equalsIgnoreCase("exit");

        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            // отправляем имя + 0
            out.write(name.getBytes(StandardCharsets.US_ASCII));
            out.write(0);
            out.flush();

            if (exitBeforeRead) {
                System.out.println("Exiting before reading response (crashing).");
                return;
            }

            if (delay > 0) {
                System.out.println("Delaying " + delay + " seconds before reading response...");
                Thread.sleep(delay * 1000L);
            }

            // читаем ключ
            byte[] tmp4 = in.readNBytes(4);
            if (tmp4.length < 4) throw new EOFException("No response");
            int keyLen = ByteBuffer.wrap(tmp4).getInt();
            byte[] keyPem = in.readNBytes(keyLen);

            byte[] tmp42 = in.readNBytes(4);
            if (tmp42.length < 4) throw new EOFException("No cert length");
            int certLen = ByteBuffer.wrap(tmp42).getInt();
            byte[] certPem = in.readNBytes(certLen);

            try (FileOutputStream fk = new FileOutputStream(outPrefix + ".key")) {
                fk.write(keyPem);
            }
            try (FileOutputStream fc = new FileOutputStream(outPrefix + ".crt")) {
                fc.write(certPem);
            }
            System.out.println("Saved " + outPrefix + ".key and " + outPrefix + ".crt");
        }
    }
}