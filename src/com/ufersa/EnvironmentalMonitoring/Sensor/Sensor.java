package com.ufersa.EnvironmentalMonitoring.Sensor;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

public class Sensor{
    private final ThreadLocalRandom rnd;
    private String id;


    public Sensor(){
        this.rnd = ThreadLocalRandom.current();
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
}