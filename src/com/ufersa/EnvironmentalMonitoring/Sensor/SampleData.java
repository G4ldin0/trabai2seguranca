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
}
