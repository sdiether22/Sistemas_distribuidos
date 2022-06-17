/* BitWelcomer.java: Brinda la aceptacion de los nuevos peers en conexion */

import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.io.IOException;
import java.util.LinkedList;

public class BitWelcomer extends Thread {
    private ServerSocket welcomeSocket = null;  // Nuevos peers
    private LinkedList<Socket> welcomeQ = null; // Lista de los peers pendientes
    private volatile boolean isStopped = false; // Para parar el hilo

    /*Creamos los peers, donde pasamos como parametro el puerto en el que se encuntra el peer*/
    public BitWelcomer(int welcomePort, final LinkedList<Socket> welcomeQ) {
        this.welcomeQ = welcomeQ;
        try {
            welcomeSocket = new ServerSocket(welcomePort);
            welcomeSocket.setSoTimeout(5000);
            System.out.println("Cliente escuchando en el puerto: " + welcomePort);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void stopThread() {
        isStopped = true;
    }

    /* run:  Aqui creamos el bucle que estara aceptando los nuevos peers */
    public void run() {
        while (!isStopped) {
            Socket peerSocket = null;
            try {
                peerSocket = welcomeSocket.accept();
            } catch (Exception ex) {
                continue;
            }
            if (peerSocket != null) {
                synchronized (welcomeQ) {
                    welcomeQ.offer(peerSocket);
                    welcomeQ.notifyAll();
                }
            }
        }
    }
}
