/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;
import java.net.*;
import java.io.*;
public class EchoMultiServer {
    private static ServerSocket serverSocket = null;
    public static void main(String[] args) throws IOException {
        try {
            serverSocket = new ServerSocket(3000);
        } catch (IOException e) {
            System.out.println("EchoMultiServer: could not listen on port: 3000, " + e.toString());
            System.exit(1);
        }
        System.out.println("Server conectado en el puerto 3000, esperando cliente\n");
        boolean listening = true;
        Socket clientSocket = null;
        while (listening) {
            clientSocket = serverSocket.accept();
            Runnable Cliente =new EchoMultiServerThread(clientSocket);
            Thread hilo=new Thread(Cliente);
            hilo.start();
            
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Could not close server socket." +
            e.getMessage());
        }
    }
}
