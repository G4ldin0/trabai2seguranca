package com.ufersa.EnvironmentalMonitoring.Sensor;

import java.io.Serializable;
import java.time.LocalDateTime;

public class SampleData implements Serializable {
    public String sensorId;
    public LocalDateTime timestamp;
    public double co2;
    public double co;
    public double no2;
    public double so2;
    public double pm25;
    public double pm10;
    public double humidity;
    public double temperature;
    public double noiseDb;
    public double uvIndex;

    public SampleData(
            String sensorId,
            LocalDateTime timestamp,
            double co2,
            double co,
            double no2,
            double so2,
            double pm25,
            double pm10,
            double humidity,
            double temperature,
            double noiseDb,
            double uvIndex
    ) {
        this.sensorId = sensorId;
        this.timestamp = timestamp;
        this.co2 = co2;
        this.co = co;
        this.no2 = no2;
        this.so2 = so2;
        this.pm25 = pm25;
        this.pm10 = pm10;
        this.humidity = humidity;
        this.temperature = temperature;
        this.noiseDb = noiseDb;
        this.uvIndex = uvIndex;
    }

    @Override
    public String toString() {
        return "SampleData{" +
                "sensorId='" + sensorId + '\'' +
                ", timestamp=" + timestamp +
                ", co2=" + co2 +
                ", co=" + co +
                ", no2=" + no2 +
                ", so2=" + so2 +
                ", pm25=" + pm25 +
                ", pm10=" + pm10 +
                ", humidity=" + humidity +
                ", temperature=" + temperature +
                ", noiseDb=" + noiseDb +
                ", uvIndex=" + uvIndex +
                '}';
    }

    public static SampleData fromString(String dataStr) {
        String[] parts = dataStr.replace("SampleData{", "")
                .replace("}", "")
                .split(", ");
        String sensorId = parts[0].split("=")[1];
        LocalDateTime timestamp = LocalDateTime.parse(parts[1].split("=")[1]);
        double co2 = Double.parseDouble(parts[2].split("=")[1]);
        double co = Double.parseDouble(parts[3].split("=")[1]);
        double no2 = Double.parseDouble(parts[4].split("=")[1]);
        double so2 = Double.parseDouble(parts[5].split("=")[1]);
        double pm25 = Double.parseDouble(parts[6].split("=")[1]);
        double pm10 = Double.parseDouble(parts[7].split("=")[1]);
        double humidity = Double.parseDouble(parts[8].split("=")[1]);
        double temperature = Double.parseDouble(parts[9].split("=")[1]);
        double noiseDb = Double.parseDouble(parts[10].split("=")[1]);
        double uvIndex = Double.parseDouble(parts[11].split("=")[1]);

        return new SampleData(
                sensorId,
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
