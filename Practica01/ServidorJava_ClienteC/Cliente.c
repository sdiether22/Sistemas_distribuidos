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
#include "Socket.c"

int main(int argc, char *argv[])
{
	struct sockaddr_in direccionSocket;
	struct in_addr *direccionIP;
	int direccionPuerto;
	int Socket_Con_Servidor;
	int longitud_Cadena=0;
	int castRed;
	char cadena[100];
	if(argc!=2)
	{
		printf("Tiene que escribir como parametro la ubicacion del servidor\n");
		return -1;
	}
	/*Se abre la conexion con el servidor, pasando el nombre del ordenador
	y el servicio solicitado.*/
	direccionIP=inet_addr(argv[1]);
	direccionPuerto=atoi("15557");
	direccionSocket.sin_family = AF_INET;
	direccionSocket.sin_addr.s_addr=direccionIP;
	direccionSocket.sin_port=htons(direccionPuerto);
	Socket_Con_Servidor=socket(AF_INET, SOCK_STREAM, 0);
	if(Socket_Con_Servidor==-1)
	{
		return -1;
		printf ("Error al crear el socket\n");
	}
	if(connect(Socket_Con_Servidor,(struct sockaddr *)&direccionSocket, sizeof(direccionSocket))==-1)
	{
		printf ("Error al buscar servidor\n");
		return -1;
	}
	if (Socket_Con_Servidor == 1)
	{
		printf ("No se pudo establecer conexion con el servidor\n");
		return -1;
	}

	//Se va a enviar una cadena de 6 caracteres, incluido el \0
	printf("Enviale un mensaje al servidor: ");
	scanf("%s",cadena);
	longitud_Cadena=strlen(cadena)+1;

	//Antes de enviar el entero hay que transformalo a formato red
	castRed = htonl (longitud_Cadena);
	Escribe_Socket (Socket_Con_Servidor, (char *)&castRed, sizeof(longitud_Cadena));

	//Se envía la cadena
	Escribe_Socket(Socket_Con_Servidor, cadena, longitud_Cadena);
	printf ("Enviamos un: %s\n", cadena);

	//Se lee un entero con la longitud de la cadena, incluido el \0
	Lee_Socket(Socket_Con_Servidor,(char *)&castRed,sizeof(int));
	longitud_Cadena=ntohl(castRed);

	//Se lee la cadena de la longitud indicada
	Lee_Socket(Socket_Con_Servidor, cadena, longitud_Cadena);
	printf("Recibimos del Servidor.java un: %s\n", cadena);

	printf("La conexion se cerro\n");

	//Se cierra el socket con el servidor
	close (Socket_Con_Servidor);
}
