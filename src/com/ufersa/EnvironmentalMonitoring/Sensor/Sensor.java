package com.ufersa.EnvironmentalMonitoring.Sensor;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
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

    public Sensor(String id){
        this.id = id;
        this.rnd = ThreadLocalRandom.current();
        executor = Executors.newScheduledThreadPool(1);
        try {
            socket = new DatagramSocket();
            executor.scheduleAtFixedRate(this::loop, 0, 3, TimeUnit.SECONDS);
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

    private void loop(){
        try {
            SampleData data = captureData();
            System.out.println("captured data: \n" + data);
            String dataStr = data.toString(); // TODO: criptografar
            socket.send(new DatagramPacket(dataStr.getBytes(StandardCharsets.UTF_8), dataStr.getBytes().length,
                    java.net.InetAddress.getByName("localhost"), 9090));
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
        new Sensor("SENSOR-001");
    }
}