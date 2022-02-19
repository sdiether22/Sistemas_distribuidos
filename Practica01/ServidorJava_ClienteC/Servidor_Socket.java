import java.net.*;
import java.io.*;

public class Servidor_Socket
{
	public static void main (String [] args)
	{
		new Servidor_Socket();
	}

	public Servidor_Socket()
	{
		try
		{
			ServerSocket socket = new ServerSocket (15557);
			System.out.println ("Esperando cliente");
			Socket cliente = socket.accept();
			System.out.println ("Conectado con cliente de " + cliente.getInetAddress());
			cliente.setSoLinger(true, 15);

			DatoSocket dato = new DatoSocket("Hola que tal");
			DataOutputStream bufferSalida = new DataOutputStream (cliente.getOutputStream());
			dato.writeObject (bufferSalida);

			DataInputStream bufferEntrada = new DataInputStream (cliente.getInputStream());
			
			DatoSocket aux = new DatoSocket("");
			aux.readObject (bufferEntrada);
			System.out.println ("Recibo del Cliente.C un: " + aux.toString());
			System.out.println ("Envio un: " + dato.toString());

			System.out.println("\nConexion cerrada\n");

			cliente.close();
			socket.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
