
package rmi;
//Interfaz de tipo remota, cabe resaltar que
// solo tiene el metodo echo ( no hay instrucciones) 
public interface EchoInterface extends java.rmi.Remote 
{
    public String echo(String pago, String tarjeta, String cvv)throws java.rmi.RemoteException;
}





