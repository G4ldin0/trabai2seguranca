package com.ufersa.EnvironmentalMonitoring.Sensor;
import com.ufersa.EnvironmentalMonitoring.Shared.SampleData;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Sensor{
    private final ThreadLocalRandom rnd;
    private final String id;
    private final DatagramSocket socket;
    private final ScheduledExecutorService executor;
    private final String location;
    private String edgeServerAddress;
    private String authToken = null;
    private final String authServerAddress = "localhost";

    public Sensor(String id, String location){
        this.location = location;
        this.id = id;
        this.rnd = ThreadLocalRandom.current();
        executor = Executors.newScheduledThreadPool(1);
        try {
            socket = new DatagramSocket();
            requestToken();
            requestEdgeServer();
            System.out.println("Starting data capture loop...");
            executor.scheduleAtFixedRate(this::loop, 0, 3, TimeUnit.SECONDS);
            executor.scheduleAtFixedRate(this::requestToken, 10, 10, TimeUnit.MINUTES);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                executor.shutdownNow();
                socket.close();
                System.out.println("Sensor shutdown completed.");
            }));
        } catch (SocketException e) {
            System.err.println("Failed to create socket: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void requestToken(){
        this.authToken = null;
        String request = "request_token device " + this.id;
        try {
            socket.send(
                    new DatagramPacket(
                            request.getBytes(StandardCharsets.UTF_8),
                            request.getBytes().length,
                            java.net.InetAddress.getByName(authServerAddress),
                            11000
                    )
            );
            DatagramPacket response = new DatagramPacket(new byte[256], 256);
            socket.setSoTimeout(5000);
            socket.receive(response);
            String responseStr = new String(response.getData(), 0, response.getLength());
            this.authToken = responseStr;
            System.out.println("Received new auth token: " + this.authToken);
        } catch (SocketTimeoutException e) {
            System.out.println("No response from Auth Server for token request.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void requestEdgeServer(){
        String locationTesting = this.location;
        while (true) {
            try {
                byte[] requestBytes = ("locate " + locationTesting).getBytes(StandardCharsets.UTF_8);
                DatagramPacket request = new DatagramPacket(
                        requestBytes,
                        requestBytes.length,
                        java.net.InetAddress.getByName("localhost"),
                        10000
                );
                socket.send(request);
                DatagramPacket response = new DatagramPacket(new byte[256], 256);
                socket.setSoTimeout(5000);
                socket.receive(response);
                String responseStr = new String(response.getData(), 0, response.getLength());
                if (responseStr.equals("not_found") || responseStr.equals("invalid")){
                    System.out.println("Edge Server not found for location: " + location);
                    locationTesting = "location1"; // testar outra location
                    continue;
                }
                this.edgeServerAddress = responseStr;
                System.out.println("Edge Server found at address: " + edgeServerAddress);
                break;
            } catch (SocketTimeoutException e) {
                System.out.println("No response from Locator Server, retrying...");
            } catch (IOException e) {
                System.err.println("Error requesting Edge Server: " + e.getMessage());
            }
        }
    }

    private void loop(){
        if (this.authToken == null) {
            System.out.println("No auth token available, skipping data send.");
            return;
        }

        try {
            SampleData data = captureData();
            System.out.println("captured data: \n" + data);
            String dataStr = data.toString(); // TODO: criptografar
            String message = this.authToken + ";" + dataStr;
            socket.send(new DatagramPacket(message.getBytes(StandardCharsets.UTF_8), message.getBytes().length,
                    java.net.InetAddress.getByName(edgeServerAddress), 9090));
        } catch (Exception e) {
            System.err.println("erro ao enviar dados: " + e.getMessage());
        }
    }

    private SampleData captureData(){
        double temperature = Math.round(rnd.nextDouble(-10.0, 50.0) * 10.0) / 10.0; // °C
        double humidity = Math.round(rnd.nextDouble(0.0, 100.0) * 10.0) / 10.0; // %
        double co2 = Math.round(rnd.nextDouble(400.0, 2000.0) * 10.0) / 10.0; // ppm
        double co = Math.round(rnd.nextDouble(0.0, 10.0) * 100.0) / 100.0; // ppm
        double no2 = Math.round(rnd.nextDouble(0.0, 200.0) * 100.0) / 100.0; // ppb
        double so2 = Math.round(rnd.nextDouble(0.0, 500.0) * 100.0) / 100.0; // ppb
        double pm25 = Math.round(rnd.nextDouble(0.0, 500.0) * 10.0) / 10.0; // µg/m³
        double pm10 = Math.round(rnd.nextDouble(0.0, 600.0) * 10.0) / 10.0; // µg/m³
        double noiseDb = Math.round(rnd.nextDouble(20.0, 120.0) * 10.0) / 10.0; // dB
        double uvIndex = Math.round(rnd.nextDouble(0.0, 11.0) * 10.0) / 10.0; // index
        LocalDateTime timestamp = LocalDateTime.now();

        return new SampleData(
                this.id,
                timestamp,
                co2,
                co,
                no2,
                so2,
                pm25,
                pm10,
                humidity,
                temperature,
                noiseDb,
                uvIndex
        );
    }

    public static void main(String[] args) {
        new Sensor("SENSOR-001", "location1");
    }
}