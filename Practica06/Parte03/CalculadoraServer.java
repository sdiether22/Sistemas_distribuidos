import java.rmi.*;
import java.rmi.Naming;

public class CalculadoraServer {

	public CalculadoraServer() {
		try {
			System.setSecurityManager(new SecurityManager());
			Calculadora c = new CalculadoraObj();
			Naming.rebind("//192.168.0.29/ServicioDeCalculadora", c);
			System.out.println("Servidor listo...");
		} catch (Exception e) {
			System.out.println("Problema: " + e);
		}
	}

	public static void main(String args[]) {
		new CalculadoraServer();
	}
}
