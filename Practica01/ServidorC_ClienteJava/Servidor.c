#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <netinet/in.h>
#include <netdb.h>
#include <unistd.h>
#include <errno.h>
#include <Socket_Servidor.h>
#include <Socket.h>


int main()
{
	//Descriptores de socket servidor y de socket con el cliente
	int socket_Servidor;
	int socket_Cliente;
	int longitud_Cadena;
	int castRed;
	int info,auxiliar=0,valor=0;
	char cadena[100];
	unsigned short int direccionPuerto;
	struct servent *puerto;
	puerto=getservbyname("cpp_java", "tcp");
	direccionPuerto=puerto->s_port;
	if (puerto == NULL)
		return -1;
	printf("Direccion Puerto: %d\n",ntohs(direccionPuerto));

	/*
	* Se abre el socket servidor, con el servicio "cpp_java" dado de
	* alta en el archivo de /etc/services.
	*/
	socket_Servidor = Abre_Socket_Inet ("cpp_java");
	if (socket_Servidor == -1)
	{
		printf ("No se puede abrir socket servidor\n");
		exit (-1);
	}

	//Se espera un cliente que quiera conectarse
	socklen_t longitud_Cliente;
	struct sockaddr cliente;
	longitud_Cliente=sizeof(cliente);
	printf ("Esperando comunicacion con el cliente\n");
	socket_Cliente=accept(socket_Servidor,&cliente,&longitud_Cliente);
	if(socket_Cliente==-1)
	{
		printf ("No se puede abrir socket de cliente\n");
		return -1;
	}

	do
	{
		
		//Se lee la informacion del cliente, primero el número de caracteres de la cadena que vamos a recibir (incluido el \0) y luego la cadena.
		Lee_Socket(socket_Cliente,(char *)&castRed, sizeof(longitud_Cadena));

		//El entero recibido hay que transformarlo de formato red a formato hardware
		longitud_Cadena=ntohl(castRed);

		//Se lee la cadena
		Lee_Socket(socket_Cliente,cadena,longitud_Cadena);
		info=atoi(cadena);//convertimos el dato a entero
		if(info!=0)
		{
			printf("\nRecibo del Cliente.java: %d\n",info);
			
			valor=info+1;

			//Se envia un entero con la longitud de una cadena (incluido el \0 del final) y la cadena.

			sprintf(cadena,"%d",valor);
		
			longitud_Cadena=strlen(cadena)+1;
	
			//El entero que se envía por el socket hay que transformalo a formato red
			castRed=htonl(longitud_Cadena);

			//Se envía el entero transformado
			Escribe_Socket(socket_Cliente,(char *)&castRed,sizeof(longitud_Cadena));
//			printf("Servidor C: Enviado %d\n", longitud_Cadena);

			//Se envía la cadena
			Escribe_Socket(socket_Cliente,cadena,longitud_Cadena);
			printf("Envio un: %s\n",cadena);

		}
		else
		{
			printf("\nRecibo del Cliente.java: %d\n",info);
			
			valor=info+1;

			//Se envia un entero con la longitud de una cadena (incluido el \0 del final) y la cadena.

			sprintf(cadena,"%d",valor);
		
			longitud_Cadena=strlen(cadena)+1;
			//El entero que se envía por el socket hay que transformalo a formato red
			castRed=htonl(longitud_Cadena);

			//Se envía el entero transformado
			Escribe_Socket(socket_Cliente,(char *)&castRed,sizeof(longitud_Cadena));
//			printf("Servidor C: Enviado %d\n", longitud_Cadena);

			//Se envía la cadena
			Escribe_Socket(socket_Cliente,cadena,longitud_Cadena);
			
			break;
		}
		
	}while(1);
	//Se cierran los sockets
	printf("\nConexion cerrada\n");
	close(socket_Cliente);
	close(socket_Servidor);
}
