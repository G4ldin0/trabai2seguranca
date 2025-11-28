package com.ufersa.EnvironmentalMonitoring.Server;

import com.ufersa.EnvironmentalMonitoring.Shared.SampleData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DataCenter implements DataCenterInterface{
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
                Socket socket = serversocket.accept();
                threadPool.submit(() -> processConnection(socket));
            } catch (IOException e) {
                System.err.println(STR."Erro ao aceitar conexão: \{e.getMessage()}");
            }
        }
    }

    private void processConnection(Socket socket){
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                SampleData data = SampleData.fromString(line);
                if (data != null) {
                    database.add(data);
                    System.out.println(STR."""
Dados recebidos e armazenados:\s
\{data}""");
                } else {
                    System.err.println(STR."Dados inválidos recebidos: \{line}");
                }
            }
            System.out.println(STR."sai da conexão com \{socket.getRemoteSocketAddress()}");
        } catch (IOException e) {
            System.err.println(STR."Erro ao processar conexão: \{e.getMessage()}");
        }
    }

    @Override
    public String generateReport(String token, String sensorId, String startDate, String endDate) {
        StringBuilder report = new StringBuilder();
        report.append("Relatório para Sensor ID: ").append(sensorId)
              .append(" de ").append(startDate).append(" até ").append(endDate).append("\n\n");

        for (SampleData data : database) {
            if (data.sensorId().equals(sensorId) &&
                    !data.timestamp().isBefore(LocalDateTime.parse(startDate)) &&
                    !data.timestamp().isAfter(LocalDateTime.parse(endDate))) {
                report.append(data).append("\n");
            }
        }

        return report.toString();
    }

    public static void main(String[] args) {
        try {
            DataCenter dataCenter = new DataCenter();
            DataCenterInterface sklt = (DataCenterInterface) UnicastRemoteObject.exportObject(dataCenter, 12000);
            LocateRegistry.createRegistry(12000);
            LocateRegistry.getRegistry(12000).rebind("DataCenter", sklt);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

    }
}
