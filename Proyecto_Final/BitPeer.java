/* BitPeer.java: Establecemos la conexion de los peers */

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Queue;
import java.util.Random;

import util.lib.BitLibrary;

public class BitPeer {
    private static final int HANDSHAKE_SIZE = 68;
    private InetSocketAddress peerAddrPort;
    private String peerIDstring;
    private byte[] peerID;
    private long lastUsed;
    private boolean[] remoteBitfield = null;
    private Socket peerSocket = null;
    private BufferedOutputStream outToPeer = null;
    private BufferedInputStream inFromPeer = null;
    private BitReader reader = null;
    private Queue<BitMessage> messageQ = null;

    public HashSet<Integer> outstandingRequests = null;
    public boolean localIsChoked;       // cliente choked
    public boolean remoteIsChoked;      // peer choked
    public boolean localIsInterested;   // cliente interesado
    public boolean remoteIsInterested;  // peer interesado

    /* Constructor para el peer desde la linea de comandos del tracker */
    public BitPeer(InetAddress peerAddr, int peerPort) {
        this.peerAddrPort = new InetSocketAddress(peerAddr, peerPort);
        String stringToHash = getIP().toString() + String.valueOf(getPort());
        peerID = BitLibrary.getSHA1(stringToHash);
        peerIDstring = BitLibrary.bytesToHex(peerID);
        this.lastUsed = System.currentTimeMillis();
        this.outstandingRequests = new HashSet<Integer>();

        // peers choked y sin interes
        this.localIsChoked = true;
        this.localIsInterested = false;
        this.remoteIsChoked = true;
        this.remoteIsInterested = false;
    }

    /* Constructor para recibir el peer en un socket tomando su direccion y puerto */
    public BitPeer(Socket peerSocket) {
        this.peerSocket = peerSocket;
        this.peerAddrPort = new InetSocketAddress(peerSocket.getInetAddress(), 
                                                  peerSocket.getPort());
        String stringToHash = getIP().toString() + String.valueOf(getPort());
        peerID = BitLibrary.getSHA1(stringToHash);
        peerIDstring = BitLibrary.bytesToHex(peerID);
        this.outstandingRequests = new HashSet<Integer>();

        // peers choked y sin interes
        this.localIsChoked = true;
        this.localIsInterested = false;
        this.remoteIsChoked = true;
        this.remoteIsInterested = false;
        try {
            this.inFromPeer = new BufferedInputStream(
                              new DataInputStream(peerSocket.getInputStream()));
            this.outToPeer = new BufferedOutputStream(
                             new DataOutputStream(peerSocket.getOutputStream()));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        // Solo inicia el lector una vez que el handshake esta completado

        this.lastUsed = System.currentTimeMillis();
    }

    /* Conexion a un peer */
    public int connect() {
        // Inicializa el flujo de E/S
        try {
            peerSocket = new Socket(getIP(), getPort());
            outToPeer = new BufferedOutputStream(
                        new DataOutputStream(peerSocket.getOutputStream()));
            inFromPeer = new BufferedInputStream(
                         new DataInputStream(peerSocket.getInputStream()));
        } catch (IOException ex) {
            System.err.println("error: fallo la conexion con el peer " + getIP());
            return -1;
        }

        this.lastUsed = System.currentTimeMillis();

        return 0;
    }

    /* Actualiza la marca de tiempo */
    public void updateLastUsed() {
        lastUsed = System.currentTimeMillis();
    }

    /* Indica el ultimo uso */
    public long getLastUsed() {
        return lastUsed;
    }

    /* Cerrar el hilo de lectura y el socket */
    public void close() {
        if (reader != null) {
            reader.stopThread();
        }
        if (peerSocket != null) {
            try {
                peerSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public boolean[] getBitfield() {
        return this.remoteBitfield;
    }

    public void setBitfield(boolean[] remoteBitfield) {
        this.remoteBitfield = remoteBitfield;
    }

    public void addToBitfield(int index) {
        remoteBitfield[index] = true;
    }

    public boolean hasPiece(int index) {
        return this.remoteBitfield[index];
    }

    /* Devolver el mensaje fuera de la lista */
    public BitMessage getNextMessage() {
        BitMessage msg = null;
        if (messageQ == null) {
            return null;
        }
        synchronized (messageQ) {
            msg = messageQ.poll();
            messageQ.notifyAll();    // Notificamos al hilo de lectura que hay un nuevo espacio
        }
        return msg;
    }

    /* Devolvemos el indice del fregmento que se tiene en el peer */
    public int getRarePiece(boolean[] clientHas) {
        /* Retornamos un -1 si no existe el fregmento */
        if (remoteBitfield == null || clientHas == null) {
            return -1;
        }

        int[] rarePieces = new int[clientHas.length];
        int j = 0;
        for (int i = 0; i < clientHas.length; ++i) {
            if (!clientHas[i] && remoteBitfield[i]) {
                rarePieces[j++] = i;
            }
        }
        if (j == 0) {
            return -1;
        }
        Random random = new Random(System.currentTimeMillis());
        return rarePieces[random.nextInt(j)];

    }

    /* Escribimos los bytes en el socket */
    public int write(byte[] sendData, int offset, int len) {
        int numWritten = 0;
        if (outToPeer == null) {
            return 0;
        }

        try {
            outToPeer.write(sendData, offset, len);
            outToPeer.flush();
        } catch (IOException ex) {
            //ex.printStackTrace();//La conexion fallo, uno peer se cayo
        }
        return numWritten;
    }

    /* Abrimos el socket del peer y enviamos el mensaje handshake, se retorna el 0 si se logro y un -1 si fallo */
    public int sendHandshake(String encoded) {
        // Abrimos la conexion del socket
        try {
            // enviamos el handshake
            byte[] handshakeMsg = generateHandshake(encoded);
            outToPeer.write(handshakeMsg, 0, handshakeMsg.length);
            outToPeer.flush();
        } catch (IOException ex) {
            System.err.println("error: No se pudo iniciar la conexion");
            return -1;
        }
        return 0;
    }

    /* recibimos, verificamos y respendemos al handshake */
    public int receiveHandshake(String encoded) {
        if (inFromPeer == null || outToPeer == null) {
            System.err.println("error: receiveHandshake encontro un socket nulo");
            return -1;
        }

        // Leemos el mensaje handshake y lo comparamos
        byte[] peerHandshakeMsg = new byte[HANDSHAKE_SIZE];
        try {
            // Bucle para leer el handshake completo
            int numRead = 0;
            while (numRead < HANDSHAKE_SIZE) {
                numRead += inFromPeer.read(peerHandshakeMsg, numRead, 
                                           HANDSHAKE_SIZE - numRead);
            }
        } catch (IOException ex) {
            System.err.println("error: No se logro leer todo el handshake");
            return -1;
        }

        byte[] myHandshakeMsg = generateHandshake(encoded);
        if (myHandshakeMsg.length != peerHandshakeMsg.length) {
            return -1;
        }
        for (int i = 0; i < myHandshakeMsg.length - 20; ++i) {
            if (peerHandshakeMsg[i] != myHandshakeMsg[i]) {
                System.err.println("error: El peer " + getIP() 
                                   + " tiene un archivo .torrent incorrecto");
                return -1;
            }
        }

        // Inicializamos el lector para leer desde el socket
        this.messageQ = new LinkedList<BitMessage>();
        this.reader = new BitReader(inFromPeer, messageQ);
        Thread t = new Thread(reader);
        t.start();

        return 0;
    }

    public byte[] generateHandshake(String encoded) {
        ByteBuffer handshakeMsg = ByteBuffer.allocate(HANDSHAKE_SIZE);

        // Construimos un mensaje handshake de 48 bytes
        // (I) byte=19 seguido del protocolo BitTorrent
        byte b = 19;
        handshakeMsg.put(b);
        try {
            handshakeMsg.put("BitTorrent protocol".getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return null;
        }
        // (II) Extencion de 8 bytes
        byte[] pad = new byte[8];
        for (int i = 0; i < 8; ++i) {
            pad[i] = 0;
        }
        handshakeMsg.put(pad);
        // (III) Codificacion SHA1 de 20 bytes 
        handshakeMsg.put(BitLibrary.getSHA1(encoded));
        // (IV) ID de peer de 20 bytes (codificacion de IP y puerto)
        handshakeMsg.put(peerID);
        handshakeMsg.flip();    // Preparando para escribir

        return handshakeMsg.array();
    }

    public Socket getSocket() {
        return peerSocket;
    }

    public InetAddress getIP() {
        return peerAddrPort.getAddress();
    }

    public int getPort() {
        return peerAddrPort.getPort();
    }

    public String getHostName() {
        return peerAddrPort.getHostName();
    }
}
