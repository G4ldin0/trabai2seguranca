package com.ufersa.EnvironmentalMonitoring.Sensor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;

public class Sensor{
    private final ThreadLocalRandom rnd;
    private final String id;
    private final DatagramSocket socket;

    private final ByteArrayOutputStream bytes;
    private final ObjectOutputStream objStream;

    public Sensor(String id){
        this.id = id;
        this.rnd = ThreadLocalRandom.current();

        try (ScheduledExecutorService executor = Executors.newScheduledThreadPool(1)){
            socket = new DatagramSocket(8080);
            bytes = new ByteArrayOutputStream();
            objStream = new ObjectOutputStream(bytes);
            objStream.flush();

            executor.scheduleAtFixedRate(this::loop, 0, 3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (SocketException e) {
            System.err.println("Failed to create socket: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e){
            System.err.println("Failed to create object stream: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void loop(){
        try {
            SampleData data = captureData();
            System.out.println(data);
            objStream.writeObject(data);
            objStream.flush();
            byte[] serializedData = new byte[2046];
            System.arraycopy(bytes.toByteArray(), 0, serializedData, 0, bytes.size());
            bytes.reset();
            socket.send(new DatagramPacket(serializedData, 1024,
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