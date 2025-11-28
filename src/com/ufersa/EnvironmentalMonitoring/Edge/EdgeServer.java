package com.ufersa.EnvironmentalMonitoring.Edge;

import com.ufersa.EnvironmentalMonitoring.Shared.SampleData;

import java.io.*;
import java.net.*;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EdgeServer {
    private final Vector<SampleData> cache;
    private final DatagramSocket receiveSocket;
    private Socket sendSocket;
    private Socket authSocket;
    private BufferedReader authReader;
    private PrintWriter authWriter;
    private final ScheduledExecutorService threadPool;
    private volatile boolean running;
    private boolean connectedToCentralServer = false;

    public EdgeServer(int port) {
        try {
            receiveSocket = new DatagramSocket(port, InetAddress.getByName("localhost"));
            sendSocket = null;
            cache = new Vector<>();
            threadPool = Executors.newScheduledThreadPool(10);
            running = true;

            System.out.println("EdgeServer iniciado na porta " + port);

            threadPool.scheduleAtFixedRate(this::connectToCentralServer, 0, 5, TimeUnit.SECONDS);
            threadPool.scheduleAtFixedRate(this::connectToauthService, 0, 5, TimeUnit.SECONDS);
            threadPool.scheduleAtFixedRate(this::syncCache, 0, 30, TimeUnit.SECONDS);
            loop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void connectToauthService(){
        if (authSocket == null){
            try {
                authSocket = new Socket();
                authSocket.connect(new InetSocketAddress("localhost", 11000), 4900);
                authReader = new BufferedReader(new InputStreamReader(authSocket.getInputStream()));
                authWriter = new PrintWriter(new OutputStreamWriter(authSocket.getOutputStream()), true);
                System.out.println("Conectado ao serviço de autenticação.");
            } catch (IOException e) {
                authSocket = null;
                System.err.println("Falha ao conectar ao serviço de autenticação: " + e.getMessage());
            }
        }
    }

    private void connectToCentralServer() {
        if (connectedToCentralServer) {
            return;
        }
        if (sendSocket == null){
            try {
                sendSocket = new Socket();
                sendSocket.connect(new InetSocketAddress("localhost", 8000), 4900);
                connectedToCentralServer = true;
                System.out.println("Conectado ao servidor central.");
            } catch (IOException e) {
                sendSocket = null;
                System.err.println("Falha ao conectar ao servidor central: " + e.getMessage());
            }
        }
    }

    private void loop() {
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        while (running) {
            try {
                receiveSocket.receive(packet);

                threadPool.submit(() -> processMessage(packet.getData()));

            } catch (IOException e) {
                if (running) {
                    System.err.println("Erro ao receber mensagem: " + e.getMessage());
                }
            }
        }
    }

    private void processMessage(byte[] data) {
        try {
            String[] message = new String(data).split(";"); // TODO: descriptografar
            System.out.println(new String(data));
            String token = message[0];
            authWriter.println(token);
            authWriter.flush();
            String authResponse = authReader.readLine();
            if (!authResponse.equals("valid")) {
                System.err.println("Token de autenticação inválido: " + token);
                return;
            }


            SampleData sampleData = SampleData.fromString(message[1]);

            System.out.println("=== MENSAGEM RECEBIDA ===");
            System.out.println("Thread: " + Thread.currentThread().getName());
            System.out.println("Tamanho: " + data.length + " bytes");
            System.out.println("Conteúdo: " + message[1]);
            System.out.println("Timestamp: " + java.time.LocalDateTime.now());
            System.out.println("========================\n");

            // TODO: sistema de monitoramento dos ultimos dados recebidos e alertas para casos extremos
            cache.add(sampleData);

        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem: " + e.getMessage());
            System.err.println("Thread: " + Thread.currentThread().getName());
            System.err.println("Causa: " + e.getClass().getSimpleName());
            e.printStackTrace();
        }
    }


    private void syncCache(){
        if (cache.isEmpty()) {
            return;
        }
        if (!connectedToCentralServer) {
            System.err.println("Não foi possível sincronizar cache: conexão com o servidor central perdida.");
            return;
        }


        try (OutputStreamWriter writer = new OutputStreamWriter(sendSocket.getOutputStream())){
            synchronized (cache) {
                // estou mandando a lista toda, eu quero isso?
                for (SampleData data : cache) {
                    writer.write(data.toString() + "\n"); // TODO: criptografar
                }
                writer.flush();
                cache.clear();
            }
            System.out.println("Cache sincronizado com o servidor central.");
        } catch (IOException e) {
            System.err.println("Erro ao sincronizar cache: " + e.getMessage());
        }

    }

    // parar o servidor de forma elegante
    public void stop() {
        running = false;

        if (receiveSocket != null && !receiveSocket.isClosed()) {
            receiveSocket.close();
        }

        if (sendSocket != null && !sendSocket.isClosed()) {
            try {
                sendSocket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar o socket de envio: " + e.getMessage());
            }
        }

        if (authSocket != null && !authSocket.isClosed()) {
            try {
                authSocket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar o socket de autenticação: " + e.getMessage());
            }
        }

        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
            }
        }

        System.out.println("EdgeServer parado.");
    }

    // Método main para testar o servidor
    public static void main(String[] args) {
        EdgeServer server = new EdgeServer(9090);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}
