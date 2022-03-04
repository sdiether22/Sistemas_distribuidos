
package server;
import java.net.*;
import java.io.*;
import java.text.*;
import java.util.*;
import rmi.EchoInterface;
public class EchoObjectSkeleton implements EchoInterface {
    String myURL="localhost";
    double valor_pago;
    long valor_tarjeta;
    int valor_cvv;
    int validacion;
    double monto1 = 9999.00d;
    double monto2 = 100.50;
    double monto3 = 1000000.87;
    	
    //Constructor de la clase EchoObjectSkeleton
    public EchoObjectSkeleton()
    {
        try {
	// obtengo el nombre del equipo donde estoy ejecutando y lo guardo en la propiedad MyURL
            myURL=InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) 
               {
                // si no pude conocer el nombre del equipo, mantengo el nombre localhost para MyURL
               myURL="localhost";
              }
    }
    // el Metodo Echo que es la implementacion de la interfaz EchoInterface
    public String echo(String pago, String tarjeta, String cvv) throws java.rmi.RemoteException 
    {
    	String tarjeta1 = "5555 6666 7777 8888";
    	String tarjeta2 = "0123 4567 8901 2345";
    	String tarjeta3 = "1111 2222 3333 4444";
    	int cvv1 = 908;
    	int cvv2 = 989;
    	int cvv3 = 123;
    	double aux1;
        double aux2;
        double aux3;
        valor_pago = Double.parseDouble(pago);
        valor_cvv = Integer.parseInt(cvv);
        System.out.println("Atendiendo al cliente '"+ myURL + "', procesando el pago ");
        String ret;
        DecimalFormat formato=new DecimalFormat("#.##");
        if(tarjeta.equals(tarjeta1) && valor_cvv==cvv1)
        {
        	aux1=monto1-valor_pago;
        	
        	if(aux1>=0)
        	{
        		monto1=aux1;
        		//System.out.println(monto1);
        		ret="1";
        	}
        	else
        	{
        		//System.out.println(monto1);
        		ret="2";
        	}
        	System.out.println("Monto: '"+ pago + "' No.tarjeta: '" + tarjeta + "' CVV: '" + cvv + "' \nSaldo disponible: '" + formato.format(monto1) +"' ");
        }
        else if(tarjeta.equals(tarjeta2) && valor_cvv==cvv2)
        {
        	aux2=monto2-valor_pago;
        	
        	if(aux2>=0)
        	{
        		monto2=aux2;
        		ret="1";
        	}
        	else
        	{
        		ret="2";
        	}
        	System.out.println("Monto: '"+ pago + "' No.tarjeta: '" + tarjeta + "' CVV: '" + cvv + "' \nSaldo disponible: '" + formato.format(monto2) +"' ");
        }
        else if(tarjeta.equals(tarjeta3) && valor_cvv==cvv3)
        {
        	aux3=monto3-valor_pago;
        	
        	if(aux3>=0)
        	{
        		monto3=aux3;
        		ret="1";
        	}
        	else
        	{
        		ret="2";
        	}
        	System.out.println("Monto: '"+ pago + "' No.tarjeta: '" + tarjeta + "' CVV: '" + cvv + "' \nSaldo disponible: '" + formato.format(monto3) +"' ");
        }
        else
        {
        	ret="0";
        	System.out.println("Monto: '"+ pago + "' No.tarjeta: '" + tarjeta + "' CVV: '" + cvv + "' ");
        } 
        try {
            Thread.sleep(3000); // hilo actual
            //ret = ret + " (retrasada 3 segundos)";
        } catch (InterruptedException e) {}
        System.out.println("Servicio terminado.\n");
        //return "0";
        return ret;
    }
   }
