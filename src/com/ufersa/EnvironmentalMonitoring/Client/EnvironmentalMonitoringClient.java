package com.ufersa.EnvironmentalMonitoring.Client;

import com.ufersa.EnvironmentalMonitoring.Server.DataCenterInterface;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class EnvironmentalMonitoringClient {
    DataCenterInterface stub;
    private final String id;
    private final String password;
    private String authToken = null;
    private final String authServerHost;
    private final int authServerPort;
    private final String registryHost;
    private final int registryPort;
    private final DatagramSocket socket;
    private final ScheduledExecutorService executor;

    public EnvironmentalMonitoringClient() {
        this("admin", "admin123", "localhost", 12000, "localhost", 11000);
    }

    public EnvironmentalMonitoringClient(String id, String password, String registryHost, int registryPort, String authServerHost, int authServerPort) {
        this.id = id;
        this.password = password;
        this.registryHost = registryHost;
        this.registryPort = registryPort;
        this.authServerHost = authServerHost;
        this.authServerPort = authServerPort;
        this.executor = Executors.newScheduledThreadPool(1);

        try {
            socket = new DatagramSocket();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create UDP socket: " + e.getMessage(), e);
        }

        // Authenticate and locate before starting the periodic loop
        requestTokenBlocking();
        locateDataCenterBlocking();

        System.out.println("Client authenticated and DataCenter located, starting loop...");
        executor.scheduleAtFixedRate(this::loop, 0, 3, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.shutdownNow();
            socket.close();
            System.out.println("Client shutdown completed.");
        }));
    }

    // Bloqueia até receber um token válido (com tentativas)
    private void requestTokenBlocking() {
        String request = "request_token client " + this.id + " " + this.password;
        while (this.authToken == null) {
            try {
                byte[] reqBytes = request.getBytes(StandardCharsets.UTF_8);
                DatagramPacket req = new DatagramPacket(reqBytes, reqBytes.length, InetAddress.getByName(authServerHost), authServerPort);
                socket.send(req);

                DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
                socket.setSoTimeout(5000);
                socket.receive(response);

                String resp = new String(response.getData(), 0, response.getLength(), StandardCharsets.UTF_8);
                if (resp.equals("unknown_client") || resp.equals("invalid_credentials") || resp.equals("invalid_request")) {
                    System.out.println("Auth server responded with error: " + resp);
                    // Não faz retry infinito para credenciais inválidas
                    break;
                }

                this.authToken = resp;
                System.out.println(STR."Received auth token: \{this.authToken}");
            } catch (java.net.SocketTimeoutException e) {
                System.out.println("No response from Auth Server for token request, retrying...");
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            } catch (IOException e) {
                System.err.println(STR."Error while requesting token: \{e.getMessage()}");
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }
    }

    // Bloqueia até localizar o DataCenter via Locator Service (UDP) e então via RMI
    private void locateDataCenterBlocking() {
        String dataCenterHost = null;
        // First ask the locator service (UDP on port 10000) for the datacenter host
        while (dataCenterHost == null) {
            try {
                String locateRequest = "locate datacenter";
                byte[] requestBytes = locateRequest.getBytes(StandardCharsets.UTF_8);
                DatagramPacket req = new DatagramPacket(requestBytes, requestBytes.length, InetAddress.getByName(this.registryHost), 10000);
                socket.send(req);

                DatagramPacket resp = new DatagramPacket(new byte[256], 256);
                socket.setSoTimeout(5000);
                socket.receive(resp);
                String respStr = new String(resp.getData(), 0, resp.getLength(), StandardCharsets.UTF_8);
                if (respStr.equals("not_found") || respStr.equals("invalid")) {
                    System.out.println("Locator service responded with: " + respStr + ", retrying in 1s...");
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                    continue;
                }
                dataCenterHost = respStr;
                System.out.println("Locator answered DataCenter host: " + dataCenterHost);
            } catch (java.net.SocketTimeoutException e) {
                System.out.println("No response from Locator Service, retrying...");
            } catch (IOException e) {
                System.err.println("Error contacting Locator Service: " + e.getMessage());
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }

        // Now use the returned host to lookup the RMI registry
        while (this.stub == null) {
            try {
                this.stub = (DataCenterInterface) LocateRegistry.getRegistry(dataCenterHost, this.registryPort).lookup("DataCenter");
                System.out.println("Located DataCenter RMI at " + dataCenterHost + ":" + registryPort);
                break;
            } catch (RemoteException | NotBoundException e) {
                System.out.println("Unable to locate DataCenter via RMI (" + dataCenterHost + ":" + registryPort + "), retrying in 1s...");
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void loop() {
        Scanner in = new Scanner(System.in);
        String input = "";

        System.out.println("""
                Type report to lookup the latest report.
                Type report [sensorId] [startDate: YYYY-MM-DDTHH:MM] [endDate: YYYY-MM-DDTHH:MM] to get a specific report.
                Type exit to quit.
                """);
        while (!input.equals("exit")){
            input = in.nextLine();
            if (input.startsWith("report")) {
                String[] parts = input.split(" ");
                String sensorId = "SENSOR-001";
                String startDate = LocalDateTime.now().minusDays(1).toString();
                String endDate = LocalDateTime.now().toString();

                if (parts.length == 4) {
                    sensorId = parts[1];
                    startDate = parts[2];
                    endDate = parts[3];
                }

                try {
                    String report = stub.generateReport(this.authToken, sensorId, startDate, endDate);
                    System.out.println("Received report:\n" + report);
                } catch (RemoteException e) {
                    System.err.println("Error while requesting report: " + e.getMessage());
                }
            } else if (!input.equals("exit")) {
                System.out.println("Unknown command. Please type 'report' or 'exit'.");
            }
        }
    }

    public static void main(String[] args) {
        String id = "admin";
        String password = "admin123";
        String registryHost = "localhost";
        int registryPort = 12000;
        String authHost = "localhost";
        int authPort = 11000;

        new EnvironmentalMonitoringClient(id, password, registryHost, registryPort, authHost, authPort);
    }
}
