import java.io.*;

public class DatoSocket implements Serializable
{
	public int longitudMensaje = 0;

	public String mensaje = "";
//	public int mensaje = 0;
	

	public DatoSocket (String cadena)
//	public DatoSocket (int cadena)
	{
		if (cadena != null)
		{
			longitudMensaje = cadena.length();
//			longitudMensaje = 1;
			mensaje = cadena;
		}
	}

	public String toString ()
//	public int entero ()
	{
		String resultado;
//		int resultado;
		resultado = mensaje;
		return resultado;
	}

	public void writeObject(java.io.DataOutputStream out)throws IOException
	{
		out.writeInt (longitudMensaje+1);
		out.writeBytes (mensaje);
		out.writeByte ('\0');
	}
	public void readObject(java.io.DataInputStream in)throws IOException
	{
		// Se lee la longitud de la cadena y se le resta 1 para eliminar el \0 que nos envia C.
		longitudMensaje=in.readInt()-1;
         
		// Array de bytes auxiliar para la lectura de la cadena.
		byte[]aux=null;

		aux=new byte[longitudMensaje];// Se le da el tamano
		in.read(aux, 0, longitudMensaje);// Se leen los bytes
		mensaje=new String(aux);//Se convierten a String
		in.read(aux,0,1);// Se lee el \0
	}
}
