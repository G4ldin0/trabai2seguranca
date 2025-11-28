package com.ufersa.EnvironmentalMonitoring.Shared;

import java.time.LocalDateTime;

public record SampleData(
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
    @Override
    public String toString() {
        return STR."SampleData{sensorId='\{sensorId}', timestamp=\{timestamp}, co2=\{co2}, co=\{co}, no2=\{no2}, so2=\{so2}, pm25=\{pm25}, pm10=\{pm10}, humidity=\{humidity}, temperature=\{temperature}, noiseDb=\{noiseDb}, uvIndex=\{uvIndex}}";
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
