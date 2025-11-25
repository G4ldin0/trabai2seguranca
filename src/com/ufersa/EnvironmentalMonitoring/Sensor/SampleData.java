package com.ufersa.EnvironmentalMonitoring.Sensor;

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
) {}
