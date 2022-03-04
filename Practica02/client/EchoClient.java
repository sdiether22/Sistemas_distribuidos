
package client;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class EchoClient {
    //definimos el Stub del cliente
    private static EchoObjectStub ss;
    
    public static void main(String[] args) 
    {
        // revisamos que los argumentos para ejecutar el programa son correctos
        String pago="";
        String tarjeta="";
        String cvv="";
        
        if (args.length<2) {
            System.out.println("Para ejecutar , hazlo en este formato: Echo <nombre o IP del Equipo> <numero de puerto>");
            System.exit(1);
        }
        //instanciamos el STUB
        ss = new EchoObjectStub();
    	//le asignamos al STUB el puerto y nombre del equipo HOST (el nombre del servidor) 
        ss.setHostAndPort(args[0],Integer.parseInt(args[1]));
        String input,output;
        int rep=1;
        Scanner lectura = new Scanner(System.in);
        try {  
                    //construyo un bucle infinito:

            do{
                //preparo "apuntador" que es el lector de flujo para el teclado
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                // asigno a una variable y leo una linea del lector de flujo que leyo del teclado
                System.out.println("\nIntroduce el monto a pagar: ");
                pago=in.readLine();
                System.out.println("Introduce el numero de tarjeta: ");
                tarjeta=in.readLine();
                System.out.println("Introduce el cvv: ");
                cvv=in.readLine();
                // Invocar el stub con el metodo remoto echo e Imprimir .. 
                //por pantalla lo que regreso el metodo remoto echo
                //System.out.println(ss.echo(pago,tarjeta,cvv));//informacion obtenida
                ss.echo(pago,tarjeta,cvv);
                System.out.println ("\nDesea realizar otro pago? Si[1], No[0] ");
                rep = lectura.nextInt();
                
                
              }while(rep!=0);
        } 
        //catch (UnknownHostException e) {
            //System.err.println("Don't know about host: "+ args[0]);
        //} 
        catch (IOException e) {
            System.err.println("Falla conexion de E/S con el host:"+args[0]);
        }
    }
}
