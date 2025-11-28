package com.ufersa.EnvironmentalMonitoring.Auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import java.io.*;
import java.net.*;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthService {

    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private ServerSocket serverSocket;
    private DatagramSocket requestSocket;
    private ExecutorService threadPool;
    private Vector<String> sensorIds;
    private HashMap<String, String> clientCredentials;


    public AuthService() throws IOException {
        sensorIds = new Vector<>(){
            {
                add("SENSOR-001");
                add("SENSOR-002");
                add("SENSOR-003");
                add("SENSOR-004");
            }
        };

        clientCredentials = new HashMap<>(){
            {
                // não
                put("admin", "admin123");
            }
        };

        serverSocket = new ServerSocket(11000, 0, InetAddress.getByName("localhost"));
        requestSocket = new DatagramSocket(11000, InetAddress.getByName("localhost"));

        try {
            threadPool = Executors.newFixedThreadPool(10);

            threadPool.submit(this::tcpLoop);
            threadPool.submit(this::udpLoop);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // solicitações de sensores para geração de tokens
    private void udpLoop() {
        byte[] buffer = new byte[256];
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                requestSocket.receive(packet);
                System.out.println("Received UDP packet from " + packet.getAddress() + ":" + packet.getPort());
                String[] request = new String(packet.getData(), 0, packet.getLength()).split(" ");

                switch (request[0]){
                    case "request_token":
                        switch (request[1]){
                            case "device":
                                if (!sensorIds.contains(request[2])) {
                                    System.out.println("Unknown sensor ID: " + request[2]);
                                    requestSocket.send(new DatagramPacket(
                                            "unknown_sensor".getBytes(),
                                            "unknown_sensor".getBytes().length,
                                            packet.getAddress(),
                                            packet.getPort()
                                    ));
                                    continue;
                                }

                                String token = Jwts.builder()
                                        .setSubject(request[2])
                                        .setIssuedAt(new Date())
                                        .setExpiration(new Date(System.currentTimeMillis() + 600000)) // 10 minutos de validade
                                        .signWith(key)
                                        .compact();

                                byte[] tokenBytes = token.getBytes(); // TODO: criptografar
                                DatagramPacket responsePacket = new DatagramPacket(
                                        tokenBytes,
                                        tokenBytes.length,
                                        packet.getAddress(),
                                        packet.getPort()
                                );
                                requestSocket.send(responsePacket);
                                System.out.println("Sent token: " + token);
                                break;

                            case "client":
                                if (clientCredentials.get(request[2]) == null) {
                                    System.out.println("Unknown client: " + request[2]);
                                    requestSocket.send(new DatagramPacket(
                                            "unknown_client".getBytes(),
                                            "unknown_client".getBytes().length,
                                            packet.getAddress(),
                                            packet.getPort()
                                    ));
                                    continue;
                                }

                                if (request.length != 4 || !clientCredentials.get(request[2]).equals(request[3])) {
                                    System.out.println("Invalid client credentials: " + request[2]);
                                    requestSocket.send(new DatagramPacket(
                                            "invalid_credentials".getBytes(),
                                            "invalid_credentials".getBytes().length,
                                            packet.getAddress(),
                                            packet.getPort()
                                    ));
                                    continue;
                                }

                                token = Jwts.builder()
                                        .setSubject(request[2])
                                        .claim("role", "sensor")
                                        .setIssuedAt(new Date())
                                        .setExpiration(new Date(System.currentTimeMillis() + 600000)) // 10 minutos de validade
                                        .signWith(key)
                                        .compact();

                                tokenBytes = token.getBytes(); // TODO: criptografar
                                responsePacket = new DatagramPacket(
                                        tokenBytes,
                                        tokenBytes.length,
                                        packet.getAddress(),
                                        packet.getPort()
                                );
                                requestSocket.send(responsePacket);
                                System.out.println("Sent token: " + token);
                                break;
                        }
                        break;

                    case "sign_in":
                        if (request.length != 3){
                            System.out.println("Invalid client credentials: " + request[1]);
                            requestSocket.send(new DatagramPacket(
                                    "invalid_credentials".getBytes(),
                                    "invalid_credentials".getBytes().length,
                                    packet.getAddress(),
                                    packet.getPort()
                            ));
                            continue;
                        }
                        if (clientCredentials.containsKey(request[1])) {
                            System.out.println("Credentials already taken: " + request[1]);
                            requestSocket.send(new DatagramPacket(
                                    "credentials_already_taken".getBytes(),
                                    "credentials_already_taken".getBytes().length,
                                    packet.getAddress(),
                                    packet.getPort()
                            ));
                            continue;
                        }

                        String token = Jwts.builder()
                                .setSubject(request[1])
                                .claim("role", "client")
                                .setIssuedAt(new Date())
                                .setExpiration(new Date(System.currentTimeMillis() + 600000)) // 10 minutos de validade
                                .signWith(key)
                                .compact();

                        byte[] tokenBytes = token.getBytes(); // TODO: criptografar
                        DatagramPacket responsePacket = new DatagramPacket(
                                tokenBytes,
                                tokenBytes.length,
                                packet.getAddress(),
                                packet.getPort()
                        );
                        requestSocket.send(responsePacket);
                        System.out.println("Sent token: " + token);

                        break;
                    default:
                        System.out.println("Invalid request: " + request[0]);
                        requestSocket.send(new DatagramPacket(
                                "invalid_request".getBytes(),
                                "invalid_request".getBytes().length,
                                packet.getAddress(),
                                packet.getPort()
                        ));
                        continue;
                }
                System.out.println("Received UDP request: " + new String(packet.getData(), 0, packet.getLength()));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // conexões de servidores feitas para validar tokens
    private void tcpLoop() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                threadPool.submit(() -> processConnection(socket));
                System.out.println("Accepted TCP connection from " + socket.getRemoteSocketAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
    }

    private void processConnection(Socket socket) {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream())
        ) {
            String token;
            while ((token = reader.readLine()) != null) {
                System.out.println("Received: " + token + " from " + socket.getRemoteSocketAddress());

                try {
                    Claims claims = Jwts.parser()
                            .setSigningKey(key)
                            .build()
                            .parseClaimsJws(token)
                            .getBody();
                    String id = claims.getSubject();
                    switch ((String) claims.get("role")){
                        case "client":
                            if (!sensorIds.contains(id)) {
                                System.out.println("Invalid sensor ID in token: " + id);
                                writer.write("invalid_sensor\n");
                                writer.flush();

                                continue;
                            }
                            break;
                        case "sensor":
                            if (!clientCredentials.containsKey(id) || !clientCredentials.get(id).equals(claims.get("password"))) {
                                System.out.println("Invalid Credentials: " + id);
                                writer.write("invalid_credentials\n");
                                writer.flush();

                                continue;
                            }
                            break;
                    }

                    writer.write("valid\n");
                    writer.flush();

                    System.out.println("Token valid for sensor ID: " + id + " from " + socket.getRemoteSocketAddress());

                } catch (ExpiredJwtException e) {
                    System.out.println("Token expired from " + socket.getRemoteSocketAddress());
                    writer.write("token_expired\n");
                    writer.flush();

                } catch (SignatureException e) {
                    System.out.println("Invalid token signature from " + socket.getRemoteSocketAddress());
                    writer.write("invalid_signature\n");
                    writer.flush();

                } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
                    System.out.println("Invalid token from " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
                    writer.write("invalid_token\n");
                    writer.flush();

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            new AuthService();
            Socket teste = new Socket(InetAddress.getByName("localhost"), 11000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(teste.getInputStream()));
            OutputStreamWriter writer = new OutputStreamWriter(teste.getOutputStream());
            writer.write("invalid_token_example\n");
            writer.flush();
            String response = reader.readLine();
            System.out.println("Response for invalid token: " + response);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
