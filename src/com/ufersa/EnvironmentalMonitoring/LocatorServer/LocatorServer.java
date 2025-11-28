package com.ufersa.EnvironmentalMonitoring.LocatorServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;

public class LocatorServer {
    HashMap<String, InetAddress> edgeServers;
    DatagramSocket socket;

    public LocatorServer() throws IOException {
        edgeServers = new HashMap<>(){
            {
                put("location1", InetAddress.getLoopbackAddress());
                put("location2", InetAddress.getLoopbackAddress());
                put("location3", InetAddress.getLoopbackAddress());
                put("datacenter", InetAddress.getLoopbackAddress());
        }};
        socket = new DatagramSocket(10000);
        System.out.println("Locator Server is running on port 10000...");
        loop();
    }

    private void loop() throws IOException {
        while (true){
            DatagramPacket p = new DatagramPacket(new byte[256], 256);
            socket.receive(p);
            String verifyRequest = new String(p.getData(), 0, p.getLength());
            if (!(verifyRequest.split(" ").length == 2 && verifyRequest.split(" ")[0].equals("locate"))) {
                socket.send(new DatagramPacket("invalid".getBytes(), "invalid".length(), p.getAddress(), p.getPort()));
                System.out.println("debug: invalid locate request received");
                continue;
            }
            String edgeServerId = verifyRequest.split(" ")[1];
            InetAddress edgeServerAddress = edgeServers.get(edgeServerId);
            if (edgeServerAddress != null) {
                socket.send(new DatagramPacket(edgeServerAddress.getHostAddress().getBytes(), edgeServerAddress.getHostAddress().length(), p.getAddress(), p.getPort()));
            } else {
                socket.send(new DatagramPacket("not_found".getBytes(), "not_found".length(), p.getAddress(), p.getPort()));
                System.out.println("debug: edge server not found for id " + edgeServerId);
            }
        }
    }

    public static void main(String[] args) {
        try {
            new LocatorServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
