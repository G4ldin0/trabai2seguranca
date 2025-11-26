package com.ufersa.EnvironmentalMonitoring.Edge;

import com.ufersa.EnvironmentalMonitoring.Sensor.SampleData;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EdgeServer {
    private final Vector<SampleData> cache;
    private final DatagramSocket receiveSocket;
    private final Socket sendSocket;
    private final ScheduledExecutorService threadPool;
    private volatile boolean running;

    public EdgeServer(int port) {
        try {
            receiveSocket = new DatagramSocket(port);
            sendSocket = new Socket("localhost", 10000);
            cache = new Vector<>();
            threadPool = Executors.newScheduledThreadPool(10);
            running = true;

            System.out.println("EdgeServer iniciado na porta " + port);
            loop();
            threadPool.scheduleAtFixedRate(this::connectToCentralServer, 0, 1, TimeUnit.MINUTES);
            threadPool.scheduleAtFixedRate(this::syncCache, 0, 30, TimeUnit.SECONDS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void connectToCentralServer() {
        if (!sendSocket.isConnected()) {
            try {
                sendSocket.connect(new InetSocketAddress("localhost", 10000), 59000);
                System.out.println("Conectado ao servidor central.");
            } catch (SocketTimeoutException e){
                System.err.println("Tempo de conexão esgotado ao tentar conectar ao servidor central: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Não foi possível conectar ao servidor central: " + e.getMessage());
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
            String message = new String(data); // TODO: descriptografar


            SampleData sampleData = SampleData.fromString(message);

            System.out.println("=== MENSAGEM RECEBIDA ===");
            System.out.println("Thread: " + Thread.currentThread().getName());
            System.out.println("Tamanho: " + data.length + " bytes");
            System.out.println("Conteúdo: " + message);
            System.out.println("Timestamp: " + java.time.LocalDateTime.now());
            System.out.println("========================\n");

            // TODO: sistema de monitoramento dos ultimos dados recebidos e alertas para casos extremos
            cache.add(sampleData);

        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem: " + e.getMessage());
            System.err.println("Thread: " + Thread.currentThread().getName());
            System.err.println("Causa: " + e.getClass().getSimpleName());
        }
    }


    private void syncCache(){
        if (cache.isEmpty()) {
            return;
        }
        if (!sendSocket.isConnected()) {
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
        EdgeServer server = new EdgeServer(9000);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}
