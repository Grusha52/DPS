package ru.nsu.chernikov;

import java.util.concurrent.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class ServerVirtual {
    private final PrivateKey issuerKey;
    private final X500Name issuerName;
    private final ExecutorService generatorPool;

    // кэш: имя → результат (ключ+сертификат), чтобы не генерить повторно
    private final ConcurrentHashMap<String, Future<Result>> cache = new ConcurrentHashMap<>();

    public ServerVirtual(PrivateKey issuerKey, X500Name issuerName, int generatorPools) {
        this.issuerKey = issuerKey;
        this.issuerName = issuerName;
        this.generatorPool = Executors.newFixedThreadPool(generatorPools);
    }

    record Result(byte[] privateKeyPem, byte[] certPem) {}

    public void start(int port) throws Exception {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server listening on " + port);

            while (true) {
                Socket client = server.accept();
                Thread.ofVirtual().start(() -> handleClient(client));
            }
        }
    }

    private void handleClient(Socket socket) {
        try (socket; InputStream in = socket.getInputStream(); OutputStream out = socket.getOutputStream()) {
            // читаем имя до нулевого байта
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            int b;
            while ((b = in.read()) != -1 && b != 0) {
                buf.write(b);
            }
            String name = buf.toString(StandardCharsets.US_ASCII);
            System.out.println("Request from client: " + name);

            // получаем или генерим
            Future<Result> future = cache.computeIfAbsent(name, n -> {
                return generatorPool.submit(() -> generateFor(n));
            });

            Result res = future.get();

            // отправляем: [lengthKey][key][lengthCert][cert]
            ByteBuffer bb = ByteBuffer.allocate(8 + res.privateKeyPem.length + res.certPem.length);
            bb.putInt(res.privateKeyPem.length).put(res.privateKeyPem);
            bb.putInt(res.certPem.length).put(res.certPem);
            out.write(bb.array());
            out.flush();

            System.out.println("Response sent to " + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Result generateFor(String name) {
        try {
            System.out.println("Generating keypair for: " + name);
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(8192);
            KeyPair kp = kpg.generateKeyPair();

            X500Name subject = new X500Name("CN=" + name);
            BigInteger serial = new BigInteger(160, new SecureRandom());
            Date notBefore = Date.from(ZonedDateTime.now().minus(1, ChronoUnit.DAYS).toInstant());
            Date notAfter = Date.from(ZonedDateTime.now().plus(365, ChronoUnit.DAYS).toInstant());

            JcaX509v3CertificateBuilder certBuilder =
                    new JcaX509v3CertificateBuilder(issuerName, serial, notBefore, notAfter, subject, kp.getPublic());

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(issuerKey);
            X509CertificateHolder holder = certBuilder.build(signer);
            X509Certificate cert = new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter()
                    .setProvider("BC").getCertificate(holder);

            byte[] keyPem = toPem(kp.getPrivate()).getBytes(StandardCharsets.US_ASCII);
            byte[] certPem = toPem(cert).getBytes(StandardCharsets.US_ASCII);

            return new Result(keyPem, certPem);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String toPem(Object obj) throws IOException {
        StringWriter sw = new StringWriter();
        try (JcaPEMWriter w = new JcaPEMWriter(sw)) {
            w.writeObject(obj);
        }
        return sw.toString();
    }

    public static PrivateKey readPrivateKeyPem(File pemFile) throws Exception {
        try (FileReader fr = new FileReader(pemFile);
             PEMParser pr = new PEMParser(fr)) {
            Object o = pr.readObject();
            JcaPEMKeyConverter conv = new JcaPEMKeyConverter().setProvider("BC");
            if (o instanceof org.bouncycastle.openssl.PEMKeyPair pk) {
                return conv.getKeyPair(pk).getPrivate();
            } else if (o instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo pi) {
                return conv.getPrivateKey(pi);
            } else {
                throw new IllegalArgumentException("Unsupported key format: " + pemFile);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: Server <port> <issuerKeyPem> <issuerDN> <generatorThreads>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        File issuerKeyFile = new File(args[1]);
        String issuerDN = args[2];

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        PrivateKey issuerKey = readPrivateKeyPem(issuerKeyFile);
        X500Name issuerName = new X500Name(issuerDN);
        int threadsCount = Integer.parseInt(args[3]);

        new ServerVirtual(issuerKey, issuerName, threadsCount).start(port);
    }
}