
package server;
import java.net.*;
import java.io.*;
public class EchoMultiServerThread implements Runnable{
    private static EchoObjectSkeleton eo = new EchoObjectSkeleton();
    private Socket clientSocket = null;
    private String myURL = "localhost";
    private BufferedReader is = null;
    private PrintWriter os = null;
    private String inputline = new String();
    String pago="";
    String tarjeta="";
    String cvv="";
        
    EchoMultiServerThread(Socket socket) {
        clientSocket = socket;
        try {
            is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            os = new PrintWriter(clientSocket.getOutputStream());
        } catch (IOException e) {
            System.err.println("Error sending/receiving" + e.getMessage());
            e.printStackTrace();
        }
        try {
            myURL=InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            System.out.println("Unknown Host :" + e.toString());
            System.exit(1);
        }
    }
    @Override
    public void run() {
        try {
            
            //while ((inputline = is.readLine()) != null) 
            do{
            //EJERCICIO: Invocar el objeto
            //EJERCICIO: y devolver la respuesta por el socket
            	pago = is.readLine();
            	tarjeta = is.readLine();
            	cvv = is.readLine();
            
                os.println(eo.echo(pago,tarjeta,cvv));
                os.flush();
            }while ((inputline = is.readLine()) != null);
            os.close();
            is.close();
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error sending/receiving" + e.getMessage());
            e.printStackTrace();
        }
    }
}
