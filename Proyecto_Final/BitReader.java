/* BitReader.java: Leemos los peers conectados */

import java.nio.ByteBuffer;
import java.io.InputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.LinkedList;

public class BitReader implements Runnable {
    private static final int INT_LEN = 4;
    private static final int MSG_BACKLOG = 10;    // Mensajes pendientes

    private InputStream inFromPeer = null;        // Mensajes entrantes
    private Queue<BitMessage> messageQ = null;    // Fila de los mensajes
    private volatile boolean isStopped = false;   // Para parar el hilo

    public BitReader(final InputStream inp, final Queue<BitMessage> queue) {
        this.inFromPeer = inp;
        this.messageQ = queue;
    }

    public void stopThread() {
        this.isStopped = true;
    }

    public void run() {
        byte[] lenBuf = new byte[INT_LEN];
        while (!isStopped) {
            // Leemos el tamaño del mensaje
            int numRead = 0;
            try {
                numRead = inFromPeer.read(lenBuf, 0, INT_LEN);
            } catch (IOException ex) {
                //ex.printStackTrace();
            }
            if (numRead != INT_LEN) {
                throw new RuntimeException("error: el hilo de lectura no esta alineado");
            }
            // NOTE: Codificamos utilizando ByteBuffer
            ByteBuffer buf = ByteBuffer.wrap(lenBuf);    
            int msgLen = buf.getInt();

            // Leemos el mensaje
            byte[] rcvData = new byte[INT_LEN + msgLen];
            
            // (I) Copiamos la longitud del mensaje
            for (int i = 0; i < INT_LEN; ++i) {
                rcvData[i] = lenBuf[i];
            }

            // (II) Leemos el resto del mensaje desde inFromPeer
            try {
                // Continuamos con la lectura, hasta el final del mensaje
                for (numRead = 0; numRead < msgLen; ) {
                    numRead += inFromPeer.read(rcvData, INT_LEN + numRead, msgLen - numRead);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (numRead != msgLen) {
                System.err.format("warning: El tamaño del mensaje era %d pero solo se obtuvo %d\n", msgLen, numRead);
            }

            BitMessage msg = BitMessage.unpack(rcvData);

            // Añadimos el mensaje a la fila, esperando un posible retraso 
            synchronized (messageQ) {
                while (messageQ.size() >= MSG_BACKLOG) {
                    try {
                        messageQ.wait();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                messageQ.offer(msg);
            }
        }
    }
}
