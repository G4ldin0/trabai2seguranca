package com.ufersa.EnvironmentalMonitoring.Server;

import com.ufersa.EnvironmentalMonitoring.Shared.SampleData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DataCenter {
    private final ScheduledExecutorService threadPool;
    private final Vector<SampleData> database;
    private final ServerSocket serversocket;

    public DataCenter(){
        database = new Vector<>();
        threadPool = Executors.newScheduledThreadPool(10);

        try {
            serversocket = new ServerSocket(8000, 0, InetAddress.getByName("localhost"));
            threadPool.submit(this::loop);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void loop(){
        while (!serversocket.isClosed()){
            try {
                var socket = serversocket.accept();
                threadPool.submit(() -> processConnection(socket));
            } catch (IOException e) {
                System.err.println("Erro ao aceitar conexão: " + e.getMessage());
            }
        }
    }

    private void processConnection(Socket socket){
        try (socket;
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                SampleData data = SampleData.fromString(line);
                if (data != null) {
                    database.add(data);
                    System.out.println("Dados recebidos e armazenados: \n" + data);
                } else {
                    System.err.println("Dados inválidos recebidos: " + line);
                }
            }
            System.out.println("sai da conexão com " + socket.getRemoteSocketAddress());
        } catch (IOException e) {
            System.err.println("Erro ao processar conexão: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new DataCenter();
    }
}
