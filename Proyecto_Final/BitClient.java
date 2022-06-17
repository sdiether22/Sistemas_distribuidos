/* BitClient.java:  Cliente para el protocolo BitTorrent */

import java.nio.file.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import util.bencode.*;
import util.lib.BitLibrary;   // Funciones para BitTorrent

/* Manejo de las conexiones BitTorrent */
public class BitClient {
    //private static final String TRNT_DIR = "./test/torrents/";
    private static final String TRNT_DIR = "./torrents/";
    //private static final String DNLD_DIR = "./test/downloads/";
    private static final String DNLD_DIR = "./recibidos/";
    //private static final String UPLD_DIR = "./test/uploads/";
    private static final String UPLD_DIR = "./archivos/";
    private static final int MAX_UNCHOKED = 4;         // Soporte de 4 a la vez
    private static final int SHA_LENGTH = 20;
    private static final int INT_LEN = 4; 
    private static boolean _DEBUG = false;             // Debugging flag
    private static String encoded;                     // Bencoded .torrent file
    private static String infoBencoded;                // Bencoded info dict
    private static int fileLength = -1;                // Tamaño del archivo
    private static int pieceLength = -1;               // Tamaño de fragmento
    private static int numPieces = -1;                 // num de fragmentos en el archivo
    private static Random random = null;               // Solicitud de fragmentos al azar
    private static boolean[] localBitfield = null;     // Fragmentos que tiene el cliente
    private static String savePath = null;             // Guardamos la ubicacion
    private static RandomAccessFile file = null;       // Archivo para transferir
    private static String[] pieces = null;             // SHA1 de los fragmentos
    private static String trackerURL = null;           // URL del tracker
    private static boolean isSeeder = false;           // Saber si el cliente tiene todo el archivo
    private static boolean runSlowly = false;          // Retardo para las pruebas
    private static int welcomePort = 6789;             // Puerto donde escucha
    private static BitWelcomer welcomer = null;        // Nuevos peers
    private static LinkedList<Socket> welcomeQ = null; // Contactos pendientes de los peers
    private static ArrayList<BitPeer> peerList = null; // Peers conectados
    private static int numUnchoked = -1;

    public static void main(String[] args) {
        ByteBuffer lenBuf = ByteBuffer.allocate(INT_LEN);
        BitMessage unchoke = new BitMessage(BitMessage.MessageType.UNCHOKE);
        // Obtencion de la configuracion del cliente desde la linea de comandos
        if (parseArgs(args) == -1) {
            return;
        }

        // Analisis del archivo .torrent
        if (initClient() == -1) {
            return;
        }
        // Inicializacion: fileLength, pieceLength, file, pieces, welcomer, infoBencoded
        logOutput(BitLibrary.getTimeString() + ": INFO .TORRENT ANALIZADA ");
        logOutput("\t   UBICACION DEL ARCHIVO " + savePath);
        logOutput("\t   LONGITUD DEL ARCHIVO " + fileLength);
        logOutput("\t   LONGITUD DE FRAGMENTOS " + pieceLength);
        logOutput("\t   FRAGMENTOS DEL CLIENTE  " + BitLibrary.getBitString(localBitfield));
        logOutput(BitLibrary.getTimeString() 
                  + ": ESCUCHANDO EN EL PUERTO " + welcomePort);

        // Abrimos la conexion y enviamos los mensajes handshake a todos los peers
        Iterator<BitPeer> it = peerList.iterator();
        while (it.hasNext()) {
            BitPeer peer = it.next();
            if (peer.connect() == -1) {
                it.remove();
                continue;
            }
            peer.sendHandshake(infoBencoded);
            BitMessage bfmsg = new BitMessage(BitMessage.MessageType.BITFIELD,
                                       BitLibrary.booleanToBits(localBitfield));
            sendMessage(peer, bfmsg);
            logOutput(BitLibrary.getTimeString() + ": mensaje HANDSHAKE iniciado");
            peer.receiveHandshake(infoBencoded);
            logOutput(BitLibrary.getTimeString() + ": mensaje HANDSHAKE completado");
        }

        // peers maximos (4)
        if (peerList.size() <= MAX_UNCHOKED) {
            for (BitPeer peer : peerList) {
                sendMessage(peer, unchoke);
                peer.remoteIsChoked = false;
            }
            numUnchoked = peerList.size();
        } else {
            Set<Integer> toUnchoke 
                    = BitLibrary.getRandomSet(MAX_UNCHOKED, 0, peerList.size());
            for (Integer i : toUnchoke) {
                sendMessage(peerList.get(i), unchoke);
                peerList.get(i).remoteIsChoked = false;
            }
            numUnchoked = MAX_UNCHOKED;
        }

        while (true) {
            // Aceptamos la conexion de nuevos peers
            synchronized (welcomeQ) {
                while (welcomeQ.isEmpty() && peerList.isEmpty()) {
                    try {
                        logOutput(BitLibrary.getTimeString() 
                                  + ": ESPERANDO POR PEERS");
                        welcomeQ.wait();
                    } catch (InterruptedException ex) {
                    }
                }
                // Despejando la fila para aceptar nuevos peers
                while (!welcomeQ.isEmpty()) {
                    Socket peerSocket = welcomeQ.poll();
                    BitPeer peer = new BitPeer(peerSocket);
                    
                    if (peer.receiveHandshake(infoBencoded) == 0) {
                        // Lo añadimos a la peerList
                        logOutput(BitLibrary.getTimeString() + ": PEER AGREGADO EN "
                                  + peer.getIP());
                        peerList.add(peer);
                        // Completamos el handshake
                        peer.sendHandshake(infoBencoded);
                        logOutput(BitLibrary.getTimeString() 
                                  + ": HANDSHAKE COMPLETADO CON "+peer.getIP());
                        // Enviamos el campo de bits
                        BitMessage bitfieldMsg 
                               = new BitMessage(BitMessage.MessageType.BITFIELD,
                                       BitLibrary.booleanToBits(localBitfield));
                        sendMessage(peer, bitfieldMsg);
                        // Unchoke si hay spots disponibles
                        if (numUnchoked < MAX_UNCHOKED) {
                            peer.remoteIsChoked = false;
                            sendMessage(peer, unchoke);
                            numUnchoked++;
                        }
                    }
                    
                }
            }

            // Proceso de un mensaje para cada peer
            for (BitPeer peer : peerList) {
                BitMessage msg = peer.getNextMessage();
                if (msg == null) {
                    continue;
                }

                // Analisis del tipo de mensaje y proceso en consecuencia
                logOutput(BitLibrary.getTimeString() + ": TIPO DE MENSAJE RECIBIDO "
                                     + msg.getType() + " DESDE " + peer.getIP());
                peer.updateLastUsed();
                if (msg.getType() == BitMessage.MessageType.KEEP_ALIVE) {
                    // Actualizando la ultima vez de uso
                } else if (msg.getType() == BitMessage.MessageType.CHOKE) {
                    logDebug("Mensaje CHOKE");
                    peer.localIsChoked = true;
                } else if (msg.getType() == BitMessage.MessageType.UNCHOKE) {
                    logDebug("Mensaje UNCHOKE");
                    peer.localIsChoked = false;
                } else if (msg.getType() == BitMessage.MessageType.INTERESTED) {
                    logDebug("Mensaje INTERESTED");
                    peer.remoteIsInterested = true;
                } else if (msg.getType() == BitMessage.MessageType.UNINTERESTED) {
                    logDebug("Mensaje UNINTERESTED");
                    peer.remoteIsInterested = false;
                } else if (msg.getType() == BitMessage.MessageType.HAVE) {
                    peer.addToBitfield(msg.getIndex());
                    logOutput(BitLibrary.getTimeString() 
                              + ": PEER " + peer.getIP()
                              + " TIENE " 
                              + BitLibrary.getBitString(peer.getBitfield()));
                    // Pedimos el fragmento si no lo tenemos
                    if (localBitfield[msg.getIndex()] == false) {
                        sendMessage(peer,
                            new BitMessage(BitMessage.MessageType.INTERESTED));
                    } else if (BitLibrary.isAllTrue(peer.getBitfield())) {
                        // Hacer espacio para otros peers si es seeder
                        if (peer.remoteIsChoked == false) {
                            peer.remoteIsChoked = true;
                            sendMessage(peer, new BitMessage(BitMessage.MessageType.CHOKE));
                            --numUnchoked;
                        }
                    }
                } else if (msg.getType() == BitMessage.MessageType.BITFIELD) {
                    boolean[] bf = BitLibrary.bitsToBoolean(msg.getBitfield(), numPieces);
                    peer.setBitfield(bf);
                    logOutput(BitLibrary.getTimeString() 
                              + ": PEER " + peer.getIP()
                              + " TIENE " 
                              + BitLibrary.getBitString(peer.getBitfield()));
                } else if (msg.getType() == BitMessage.MessageType.REQUEST) {
                    logDebug("Mensaje REQUEST: Peer quiere el fragmento " + msg.getIndex());
                    if (peer.remoteIsChoked) {
                        logDebug("El peer esta choked, no puede enviar ");
                    } else {
                        BitMessage reply = null;
                        // Revisando que el cliente tenga el fragmento
                        if (localBitfield[msg.getIndex()] == false) {
                            // El peer tiene el bitfield incorrecto, enviar otro
                            logDebug("warning: El peer piensa incorrectamente que tenemos " + msg.getIndex());
                            reply = new BitMessage(BitMessage.MessageType.BITFIELD,
                                           BitLibrary.booleanToBits(localBitfield));
                        // Leemos el frag del archivo
                        } else {
                            byte[] replyData = new byte[msg.getBlockLength()];
                            int numRead = 0;
                            try {
                                file.seek(msg.getBegin());
                                numRead = file.read(replyData, 0, msg.getBlockLength());
                                logDebug("Leer " + numRead + " bytes del archivo");
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            reply = new BitMessage(BitMessage.MessageType.PIECE,
                                        msg.getIndex(), msg.getBegin(), replyData);
                        }

                        sendMessage(peer, reply);
                        logOutput(BitLibrary.getTimeString() 
                              + ": ENVIAMOS FRAGMENTO " + msg.getIndex() 
                              + " A " + peer.getIP());
                    }
                } else if (msg.getType() == BitMessage.MessageType.PIECE) {
                    if (localBitfield[msg.getIndex()]) {
                        logDebug("warning: El fragmento recibido ya se tiene");
                        continue;
                    }
                    // Buscamos y escribimos en el archivo
                    try {
                        file.seek(msg.getBegin());
                        file.write(msg.getBlock());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    // Actualizamos bitfield, enviamos la respuesta HAVE a todos los peers
                    localBitfield[msg.getIndex()] = true;
                    BitMessage haveMsg 
                                   = new BitMessage(BitMessage.MessageType.HAVE,
                                                    msg.getIndex());
                    for (BitPeer p : peerList) {
                        sendMessage(p, haveMsg);
                    }
                    logOutput(BitLibrary.getTimeString() + ": AHORA TIENE "
                                + BitLibrary.getBitString(localBitfield));

                    // become a seeder if all downloaded
                    if (BitLibrary.isAllTrue(localBitfield)) {
                        logOutput(BitLibrary.getTimeString() + ": DESCARGA COMPLETA");
                        logDebug("bitfield local " 
                                 + BitLibrary.getBitString(localBitfield));
                        isSeeder = true;
                    }
                } else if (msg.getType() == BitMessage.MessageType.CANCEL) {
                    // used in "end game" mode, not implemented in this project
                } else {
                    throw new RuntimeException("Tipo de mensaje recibido invalido");
                }
            }
            // (II): Actualizamos el estado de los interesados
            for (BitPeer peer : peerList) {
                if (!peer.localIsInterested 
                    && peer.getRarePiece(localBitfield) > -1) {
                    peer.localIsInterested = true;
                    BitMessage msg 
                            = new BitMessage(BitMessage.MessageType.INTERESTED);
                    sendMessage(peer, msg);
                }
            }

            // (III): Solicitando los fragmentos a todos los peers unchoked
            if (!isSeeder) {    // Falta al menos un fragmento
                for (BitPeer peer : peerList) {
                    int index;
                    if (!peer.localIsChoked && peer.localIsInterested
                        && (index = peer.getRarePiece(localBitfield)) > -1
                        && !peer.outstandingRequests.contains(index)) {
                        int indexLength = pieceLength;
                        if (index ==numPieces-1 && fileLength%pieceLength > 0) {
                            indexLength = fileLength % pieceLength;
                        }
                        BitMessage request 
                                = new BitMessage(BitMessage.MessageType.REQUEST,
                                       index, index * pieceLength, indexLength);
                        peer.outstandingRequests.add(index);
                        sendMessage(peer, request);
                    }
                }
            }
            // Insertamos pausas en el debugging
            if (runSlowly) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {

                }
            }
        }
    }

    /* Enviamos un BitMenssage al peer especificado */
    public static void sendMessage(BitPeer peer, BitMessage msg) {
        byte[] packedMsg = msg.pack();
        peer.write(packedMsg, 0, packedMsg.length);
        // Registro de los mensajes enviados
        StringBuilder sb = new StringBuilder();
        //sb.append(BitLibrary.getTimeString() + ": ENVIADO " + msg.getType());
        /*if (msg.getType() == BitMessage.MessageType.REQUEST
            || msg.getType() == BitMessage.MessageType.PIECE) {
            sb.append(" PARA " + msg.getIndex());
        }
        sb.append(" A " + peer.getIP());
        logOutput(sb.toString());*/
    }

    /* parseArgs:  create saveFile for writing, get torrent metadata */
    /* return -1 on failure and 0 otherwise */
    public static int parseArgs(String[] args) {
        peerList = new ArrayList<BitPeer>();
        if (args.length == 0 || args.length % 2 == 0 
            || BitLibrary.hasStr(args, "-h")) {
            logError("uso: java BitClient [FLAGS]* torrentFile");
            logError("\t-h         \t Informacion de uso");
            logError("\t-s saveFile\t Especifica la ubicacion");
            logError("\t-p IP:port \t IP del peer");
            logError("\t-w port    \t Puerto del socket de bienvenida");
            logError("\t-x seed    \t Iniciar este cliente como seeder");
            logError("\t-z slow    \t Ejecutar el retardo para pruebas");
            return -1;
        }

        for (int i = 0; i < args.length - 1; i += 2) {
            if (args[i].equals("-s")) {
                savePath = args[i+1];
            } else if (args[i].equals("-p")) {
                // add a peer to the list
                InetAddress peerAddr = null;
                int peerPort = 0;
                try {
                    int delimPos = args[i+1].indexOf(':', 0);
                    String ipString = args[i+1].substring(0, delimPos);
                    String portString = args[i+1].substring(delimPos + 1);

                    peerAddr = InetAddress.getByName(ipString);
                    peerPort = Integer.parseInt(portString);
                    peerList.add(new BitPeer(peerAddr, peerPort));
                } catch (UnknownHostException|NumberFormatException ex) {
                    logError("error: IP:port desconocidos " + args[i+1]);
                    return -1;
                }
                logDebug("Peer agregado: IP = " + peerAddr + ", " 
                         + "Puerto = " + peerPort);
            } /*else if (args[i].equals("-v")) {
                if (args[i+1].equals("on")) {
                    _DEBUG = true;
                } else {
                    _DEBUG = false;
                }
            }*/ else if (args[i].equals("-w")) {
                try {
                    welcomePort = Integer.parseInt(args[i+1]);
                } catch (NumberFormatException ex) {
                    logError("error: Puerto de bienvenida invalido " + args[i+1]);
                    return -1;
                }
            } else if (args[i].equals("-x")) {
                isSeeder = true;
                // Archivo a transmitir encontrado
            } else if (args[i].equals("-z")) {
                runSlowly = true;
            }
        }
        /* Leemos los datos del archivo torrent */
        try {
            String torrentName = TRNT_DIR + args[args.length - 1];
            byte[] torrentData = Files.readAllBytes(Paths.get(torrentName));
            encoded = new String(torrentData, "US-ASCII");
            encoded = encoded.trim();
        } catch (IOException ex) {
            logError("error: No se puede abrir " + args[args.length - 1]);
            return -1;
        }

        return 0;
    }

    /* Analizando archivos */
    /* success ==> initialized: fileLength, pieceLength, saveBuf, pieces */
    public static int initClient() {
        BObject[] metainfo = BDecoder.read(encoded);
        if (metainfo.length != 1) {
            logError("error: Archivo .torrent invalido");
            return -1;
        }
        BDict metaDict = (BDict) metainfo[0];
        // (a) Analizamos el diccinario de informacion 
        if (metaDict.containsKey("info")) {
            BDict infoDict = (BDict) metaDict.get("info");
            infoBencoded = infoDict.encode();

            // (I) Longitud del campo
            //BObject len = infoDict.get("Longitud");
            BObject len = infoDict.get("length");
            if (len == null) {
                logError("error: Longitud no valida en el archivo .torrent");
                return -1;
            }
            fileLength = Integer.parseInt(len.print());
            logDebug("Longitud del archivo obtenido " + fileLength);

            // (II) Longitud del campo del fragmento
            //BObject plen = infoDict.get("Longitud del fragmento");
            BObject plen = infoDict.get("piece length");
            if (plen == null) {
                logError("error: Longitud del fragmento del archivo .torrent no valido");
                return -1;
            }
            pieceLength = Integer.parseInt(plen.print());
            logDebug("Longitud del fragmento obtenido " + pieceLength);
            numPieces = fileLength / pieceLength;
            if (fileLength % pieceLength > 0) {
                ++numPieces;
            }
            logDebug("Numero de fragmentos obtenidos " + numPieces);

            // (III) Campo para guardar el nombre ==> guardar en DNLD_DIR/<nombre_sugerido>
            //BObject sname = infoDict.get("Nombre");
            BObject sname = infoDict.get("name");
            if (sname != null && savePath == null) {    // Flag -s no utilizada
                savePath = DNLD_DIR + sname.print();
                logDebug("Ruta de acceso obtenida " + savePath);
            }

            // (iv) valores SHA1 para los fragmentos
            //BObject sha = infoDict.get("Fragmentos");
            BObject sha = infoDict.get("pieces");
            if (sha == null) {
                logError("error: Codificacion SHA1 invalida");
                return -1;
            }
            String piecesSHA1 = sha.print();
            if (piecesSHA1.length() % SHA_LENGTH != 0) {
                logError("error: Longitud de SHA1 no divisible por 20");
                return -1;
            } else {
                // Dividir los SHA1 hashes en un array
                pieces = new String[piecesSHA1.length() / SHA_LENGTH];
                for (int i = 0; i < pieces.length; ++i) {
                    String s = piecesSHA1.substring(SHA_LENGTH * i, 
                                                    SHA_LENGTH * (i + 1));
                    byte[] hashData;
                    try {
                        hashData = s.getBytes("UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        ex.printStackTrace();
                        return -1;
                    }
                    pieces[i] = BitLibrary.bytesToHex(hashData);
                }
                if (_DEBUG) {
                    logDebug("Tengo los siguientes fragmentos:");
                    if (_DEBUG) {
                        for (int i = 0; i < pieces.length; ++i) {
                            logDebug(pieces[i]);
                        }
                    }
                }
            }

            // (V) Campo de bist (bitfield)
            localBitfield = new boolean[numPieces];
            if (isSeeder) {
                logDebug("Soy el SEEDER");
            }
            for (int i = 0; i < localBitfield.length; ++i) {
                localBitfield[i] = isSeeder;   // true si es seeder, else si no
            }
        } else {
            logError("error: No se especifico ningun campo de informacion en el archivo .torrent");
            return -1;
        }
        // (b) Obtener el URL del tracker
        //BObject tracker = metaDict.get("Anunciar");
        BObject tracker = metaDict.get("announce");
        if (tracker != null) {
            trackerURL = tracker.print();
        }
        logDebug("Tengo la URL del Tracker " + trackerURL);
        // (c) Inicializa el archivo torrent para leer/escribir
        if (savePath == null) {
            logError("error: No se especifico la ubicacion para guardar");  // .torrent ni CLI
            return -1;
        }
        if (isSeeder) {
            // Cambiar a UPLD_DIR para un seeder
            savePath = savePath.substring(savePath.lastIndexOf('/') + 1);
            savePath = UPLD_DIR + savePath;
            logDebug("Seeder ahora tiene una ruta de guardado = " + savePath);
            // Asegurando que el archivo existe y tiene la longitud adecuada 
            File source = new File(savePath);
            if (!source.isFile()) {
                logError("error: seeder no tiene " + savePath);
                return -1;
            } else if (source.length() != fileLength) {
                logError("error: La longitud del archivo difiere de las especificaciones del archivo torrent");
                return -1;
            }
            try {
                file = new RandomAccessFile(source, "r");
                logDebug("El seeder abrio el archivo en " + source);
            } catch (IOException ex) {
                logError("error: El seeder no se pudo abrir " + savePath);
                return -1;
            }
        } else {
            try {
                file = new RandomAccessFile(savePath, "rw");
                file.setLength(fileLength);
                logDebug("Leecher abrio un nuevo archivo en " + savePath);
            } catch (IOException ex) {
                logError("error: El cliente no pudo abrir " + savePath);
                return -1;
            }
        }
            
        // (d) Creamos un hilo de la bienvenida
        welcomeQ = new LinkedList<Socket>();
        welcomer = new BitWelcomer(welcomePort, welcomeQ);
        welcomer.start();

        return 0;
    }

    public static void logError(String str) {
        System.err.println(str);
    }

    public static void logDebug(String str) {
        if (_DEBUG) {
            System.err.println(str);
        }
    }

    public static void logOutput(String str) {
        System.out.println(str);
    }
}
