package com.ufersa.EnvironmentalMonitoring.Sensor;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;

public class Sensor{
    private final ThreadLocalRandom rnd;
    private final String id;
    private final DatagramSocket socket;

    public Sensor(String id){
        this.id = id;
        this.rnd = ThreadLocalRandom.current();

        try (ScheduledExecutorService executor = Executors.newScheduledThreadPool(1)){
            socket = new DatagramSocket(8080);

            executor.scheduleAtFixedRate(this::loop, 0, 3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (SocketException e) {
            System.err.println("Failed to create socket: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void loop(){
        try {
            SampleData data = captureData();
            String dataStr = data.toString(); // TODO: criptografar
            socket.send(new DatagramPacket(dataStr.getBytes(StandardCharsets.UTF_8), 1024,
                    java.net.InetAddress.getByName("localhost"), 9090));
        } catch (Exception e) {
            throw new RuntimeException(e);
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
        new Sensor("SENSOR-001");
    }
}